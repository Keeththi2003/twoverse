import {
  buildMessage,
  COUNTED_PUSH_TYPES,
  type FcmMessage,
  MAX_PUSH_COUNT,
  type PushType,
  SERVER_PUSH_TYPES,
  USER_PUSH_TYPES,
} from "./message.ts";

export type SendResult = "ok" | "invalid_token" | "failed";

/** Everything the handler needs from outside, so it can be tested without network calls. */
export interface PushDeps {
  /** Shared secret for server-originated pushes (reunion day), from Supabase secrets. */
  internalSecret?: string;
  /** Runs prepare_partner_push as the calling user: the partner-only permission check. */
  preparePartnerPush(authorization: string, kind: string): Promise<{ tokens: string[] } | { error: string }>;
  /** Device tokens of the given users (server pushes only). */
  pushTargets(userIds: string[]): Promise<string[]>;
  removeTokens(tokens: string[]): Promise<void>;
  /** Null when FCM isn't configured (no service account secret). */
  send: ((message: FcmMessage) => Promise<SendResult>) | null;
}

const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

/** Error keys raised by prepare_partner_push, as HTTP statuses. */
const ERROR_STATUS: Record<string, number> = {
  not_authenticated: 401,
  not_paired: 403,
  not_allowed: 403,
  rate_limited: 429,
};

export async function handle(req: Request, deps: PushDeps): Promise<Response> {
  if (req.method !== "POST") return json({ error: "method_not_allowed" }, 405);

  let body: { type?: unknown; user_ids?: unknown; count?: unknown };
  try {
    body = await req.json();
  } catch {
    return json({ error: "invalid_body" }, 400);
  }
  const type = typeof body.type === "string" ? body.type : "";

  const internalHeader = req.headers.get("x-internal-secret");
  let tokens: string[];
  let count: number | undefined;
  if (internalHeader !== null) {
    if (!deps.internalSecret || !constantTimeEquals(internalHeader, deps.internalSecret)) {
      return json({ error: "unauthorized" }, 401);
    }
    const userIds = body.user_ids;
    if (!SERVER_PUSH_TYPES.has(type) || !Array.isArray(userIds) || !userIds.every((id) => typeof id === "string" && UUID.test(id))) {
      return json({ error: "invalid_request" }, 400);
    }
    if (COUNTED_PUSH_TYPES.has(type)) {
      const value = body.count;
      if (typeof value !== "number" || !Number.isInteger(value) || value < 1 || value > MAX_PUSH_COUNT) {
        return json({ error: "invalid_request" }, 400);
      }
      count = value;
    }
    tokens = await deps.pushTargets(userIds as string[]);
  } else {
    const authorization = req.headers.get("authorization") ?? "";
    if (!authorization.startsWith("Bearer ")) return json({ error: "unauthorized" }, 401);
    if (!USER_PUSH_TYPES.has(type)) return json({ error: "invalid_type" }, 400);
    const prepared = await deps.preparePartnerPush(authorization, type);
    if ("error" in prepared) {
      return json({ error: prepared.error }, ERROR_STATUS[prepared.error] ?? 500);
    }
    tokens = prepared.tokens;
  }

  if (deps.send === null) return json({ error: "push_not_configured" }, 503);

  const results = await Promise.all(tokens.map((token) => deps.send!(buildMessage(type as PushType, token, count))));
  const invalid = tokens.filter((_, i) => results[i] === "invalid_token");
  if (invalid.length > 0) await deps.removeTokens(invalid);
  return json({ sent: results.filter((r) => r === "ok").length }, 200);
}

function json(body: unknown, status: number): Response {
  return new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } });
}

function constantTimeEquals(a: string, b: string): boolean {
  const left = new TextEncoder().encode(a);
  const right = new TextEncoder().encode(b);
  let diff = left.length ^ right.length;
  for (let i = 0; i < Math.max(left.length, right.length); i++) diff |= (left[i] ?? 0) ^ (right[i] ?? 0);
  return diff === 0;
}
