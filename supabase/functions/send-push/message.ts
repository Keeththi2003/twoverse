/** Push types and the FCM messages built for them (FR-NOT, FR-LOC-5, FR-ORB). */

export type PushType =
  | "wake_up"
  | "partner_joined"
  | "new_memory"
  | "reunion_day"
  | "shooting_star"
  | "anniversary"
  | "orbit_milestone";

/** Types a signed-in user may send to their partner; the database applies the rules. */
export const USER_PUSH_TYPES: ReadonlySet<string> = new Set(["wake_up", "partner_joined", "new_memory"]);

/** Types only the server sends (pg_cron and database triggers, with the internal secret). */
export const SERVER_PUSH_TYPES: ReadonlySet<string> = new Set([
  "reunion_day",
  "shooting_star",
  "anniversary",
  "orbit_milestone",
]);

/** Types that carry a number: years for an anniversary, the day number for a milestone (FR-NOT-6). */
export const COUNTED_PUSH_TYPES: ReadonlySet<string> = new Set(["anniversary", "orbit_milestone"]);

/** Upper bound for that number, so a bad request can't put anything else into a push. */
export const MAX_PUSH_COUNT = 100_000;

export interface FcmMessage {
  token: string;
  data: Record<string, string>;
  android: { priority: "HIGH"; ttl: string; collapse_key?: string };
}

/**
 * Data-only messages: the app builds the notification from its own strings, so a push can
 * never carry a photo, caption or any other content (FR-NOT-1). The wake-up ping is silent
 * (FR-NOT-5); high priority lets the partner's phone wake to upload its location. Anniversary and
 * milestone pushes add only their number.
 */
export function buildMessage(type: PushType, token: string, count?: number): FcmMessage {
  if (type === "wake_up") {
    return { token, data: { type }, android: { priority: "HIGH", ttl: "60s", collapse_key: "wake_up" } };
  }
  const data: Record<string, string> = count === undefined ? { type } : { type, count: String(count) };
  return { token, data, android: { priority: "HIGH", ttl: "86400s" } };
}
