/** Push types and the FCM messages built for them (FR-NOT, FR-LOC-5). */

export type PushType = "wake_up" | "partner_joined" | "new_memory" | "reunion_day";

/** Types a signed-in user may send to their partner; the database applies the rules. */
export const USER_PUSH_TYPES: ReadonlySet<string> = new Set(["wake_up", "partner_joined", "new_memory"]);

/** Types only the server sends (pg_cron with the internal secret). */
export const SERVER_PUSH_TYPES: ReadonlySet<string> = new Set(["reunion_day"]);

export interface FcmMessage {
  token: string;
  data: Record<string, string>;
  android: { priority: "HIGH"; ttl: string; collapse_key?: string };
}

/**
 * Data-only messages: the app builds the notification from its own strings, so a push can
 * never carry a photo, caption or any other content (FR-NOT-1). The wake-up ping is silent
 * (FR-NOT-5); high priority lets the partner's phone wake to upload its location.
 */
export function buildMessage(type: PushType, token: string): FcmMessage {
  if (type === "wake_up") {
    return { token, data: { type }, android: { priority: "HIGH", ttl: "60s", collapse_key: "wake_up" } };
  }
  return { token, data: { type }, android: { priority: "HIGH", ttl: "86400s" } };
}
