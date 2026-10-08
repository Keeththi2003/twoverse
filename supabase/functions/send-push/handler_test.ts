import { assertEquals } from "jsr:@std/assert@1";
import { handle, type PushDeps } from "./handler.ts";
import type { FcmMessage } from "./message.ts";

const USER_A = "11111111-1111-4111-8111-111111111111";

function fakeDeps(overrides: Partial<PushDeps> = {}) {
  const sent: FcmMessage[] = [];
  const removed: string[][] = [];
  const prepared: string[] = [];
  const deps: PushDeps = {
    internalSecret: "internal-secret",
    preparePartnerPush: (_authorization, kind) => {
      prepared.push(kind);
      return Promise.resolve({ targets: [{ token: "partner-token", partnerName: "Kanna", partnerPronouns: "she" }] });
    },
    pushTargets: () => Promise.resolve([{ token: "target-token", partnerName: "Ammu", partnerPronouns: "he" }]),
    removeTokens: (tokens) => {
      removed.push(tokens);
      return Promise.resolve();
    },
    send: (message) => {
      sent.push(message);
      return Promise.resolve("ok");
    },
    ...overrides,
  };
  return { deps, sent, removed, prepared };
}

function post(body: unknown, headers: Record<string, string> = {}) {
  return new Request("http://localhost/send-push", { method: "POST", body: JSON.stringify(body), headers });
}

const signedIn = { authorization: "Bearer user-jwt" };

Deno.test("only POST is accepted", async () => {
  const { deps } = fakeDeps();
  assertEquals((await handle(new Request("http://localhost/send-push"), deps)).status, 405);
});

Deno.test("user pushes need a bearer token", async () => {
  const { deps, prepared } = fakeDeps();
  assertEquals((await handle(post({ type: "wake_up" }), deps)).status, 401);
  assertEquals(prepared.length, 0);
});

Deno.test("users cannot send server-only or unknown types", async () => {
  const { deps, prepared } = fakeDeps();
  assertEquals((await handle(post({ type: "reunion_day" }, signedIn), deps)).status, 400);
  assertEquals((await handle(post({ type: "shooting_star" }, signedIn), deps)).status, 400);
  assertEquals((await handle(post({ type: "anything" }, signedIn), deps)).status, 400);
  assertEquals(prepared.length, 0);
});

Deno.test("users can't choose recipients: tokens come only from the partner check", async () => {
  const { deps, sent, prepared } = fakeDeps();
  const response = await handle(post({ type: "wake_up", user_ids: [USER_A] }, signedIn), deps);
  assertEquals(response.status, 200);
  assertEquals(prepared, ["wake_up"]);
  assertEquals(sent.map((m) => m.token), ["partner-token"]);
});

Deno.test("partner check failures become HTTP errors", async () => {
  for (const [error, status] of [["not_paired", 403], ["not_allowed", 403], ["rate_limited", 429], ["not_authenticated", 401], ["internal", 500]] as const) {
    const { deps, sent } = fakeDeps({ preparePartnerPush: () => Promise.resolve({ error }) });
    assertEquals((await handle(post({ type: "wake_up" }, signedIn), deps)).status, status, error);
    assertEquals(sent.length, 0);
  }
});

Deno.test("server pushes need the internal secret", async () => {
  const { deps, sent } = fakeDeps();
  const body = { type: "reunion_day", user_ids: [USER_A] };
  assertEquals((await handle(post(body, { "x-internal-secret": "wrong" }), deps)).status, 401);
  assertEquals((await handle(post(body, { "x-internal-secret": "internal-secret" }), deps)).status, 200);
  assertEquals(sent.map((m) => m.token), ["target-token"]);
});

Deno.test("a shooting star push is a server push that carries only its type and the partner label", async () => {
  const { deps, sent } = fakeDeps();
  const response = await handle(post({ type: "shooting_star", user_ids: [USER_A] }, { "x-internal-secret": "internal-secret" }), deps);
  assertEquals(response.status, 200);
  assertEquals(sent[0].data, { type: "shooting_star", partner_name: "Ammu", partner_pronouns: "he" });
});

Deno.test("anniversary and milestone pushes carry only their type, number and partner label", async () => {
  const { deps, sent } = fakeDeps();
  const secret = { "x-internal-secret": "internal-secret" };
  assertEquals((await handle(post({ type: "anniversary", count: 2, user_ids: [USER_A], note: "x" }, secret), deps)).status, 200);
  assertEquals(sent[0].data, { type: "anniversary", count: "2", partner_name: "Ammu", partner_pronouns: "he" });
  assertEquals((await handle(post({ type: "orbit_milestone", count: 1000, user_ids: [USER_A] }, secret), deps)).status, 200);
  assertEquals(sent[1].data, { type: "orbit_milestone", count: "1000", partner_name: "Ammu", partner_pronouns: "he" });
});

Deno.test("anniversary and milestone pushes need a sensible number and are server-only", async () => {
  const { deps, sent } = fakeDeps();
  const secret = { "x-internal-secret": "internal-secret" };
  for (const count of [undefined, 0, -1, 1.5, "2", 1_000_000]) {
    assertEquals((await handle(post({ type: "anniversary", count, user_ids: [USER_A] }, secret), deps)).status, 400, String(count));
  }
  assertEquals((await handle(post({ type: "orbit_milestone", count: 100 }, signedIn), deps)).status, 400);
  assertEquals(sent.length, 0);
});

Deno.test("server pushes are refused when no internal secret is configured", async () => {
  const { deps } = fakeDeps({ internalSecret: undefined });
  const response = await handle(post({ type: "reunion_day", user_ids: [USER_A] }, { "x-internal-secret": "" }), deps);
  assertEquals(response.status, 401);
});

Deno.test("server pushes only send server types to valid user ids", async () => {
  const { deps } = fakeDeps();
  const secret = { "x-internal-secret": "internal-secret" };
  assertEquals((await handle(post({ type: "wake_up", user_ids: [USER_A] }, secret), deps)).status, 400);
  assertEquals((await handle(post({ type: "reunion_day", user_ids: ["not-a-uuid"] }, secret), deps)).status, 400);
});

Deno.test("messages carry only the type and partner label: no notification, photo or caption", async () => {
  const { deps, sent } = fakeDeps();
  await handle(post({ type: "new_memory", caption: "secret words", image_url: "x" }, signedIn), deps);
  assertEquals(sent[0].data, { type: "new_memory", partner_name: "Kanna", partner_pronouns: "she" });
  assertEquals(Object.keys(sent[0]).sort(), ["android", "data", "token"]);
});

Deno.test("the partner label is trimmed, capped and only uses known pronouns", async () => {
  const { deps, sent } = fakeDeps({
    pushTargets: () => Promise.resolve([{ token: "t", partnerName: `  ${"x".repeat(40)}  `, partnerPronouns: "it" }]),
  });
  await handle(post({ type: "reunion_day", user_ids: [USER_A] }, { "x-internal-secret": "internal-secret" }), deps);
  assertEquals(sent[0].data, { type: "reunion_day", partner_name: "x".repeat(30) });
});

Deno.test("the silent wake-up carries no partner label", async () => {
  const { deps, sent } = fakeDeps();
  await handle(post({ type: "wake_up" }, signedIn), deps);
  assertEquals(sent[0].data, { type: "wake_up" });
});

Deno.test("the wake-up ping is a short-lived high-priority data message", async () => {
  const { deps, sent } = fakeDeps();
  await handle(post({ type: "wake_up" }, signedIn), deps);
  assertEquals(sent[0].android, { priority: "HIGH", ttl: "60s", collapse_key: "wake_up" });
});

Deno.test("tokens FCM no longer knows are removed", async () => {
  const { deps, removed } = fakeDeps({ send: () => Promise.resolve("invalid_token") });
  const response = await handle(post({ type: "wake_up" }, signedIn), deps);
  assertEquals(await response.json(), { sent: 0 });
  assertEquals(removed, [["partner-token"]]);
});

Deno.test("without a Firebase service account nothing is sent, after the permission check", async () => {
  const { deps, prepared } = fakeDeps({ send: null });
  assertEquals((await handle(post({ type: "wake_up" }, signedIn), deps)).status, 503);
  assertEquals(prepared, ["wake_up"]);
});
