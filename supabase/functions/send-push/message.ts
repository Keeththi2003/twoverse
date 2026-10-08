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

/** One device to notify, with how its owner knows their partner (FR-NOT-7). */
export interface PushTarget {
  token: string;
  /** The recipient's nickname for their partner, else the partner's short name. */
  partnerName?: string | null;
  /** The partner's pronouns: she, he or they. */
  partnerPronouns?: string | null;
}

const PRONOUNS: ReadonlySet<string> = new Set(["she", "he", "they"]);
const MAX_NAME_LENGTH = 30;

export interface FcmMessage {
  token: string;
  data: Record<string, string>;
  android: { priority: "HIGH"; ttl: string; collapse_key?: string };
}

/**
 * Data-only messages: the app builds the notification from its own strings, so a push can
 * never carry a photo, caption or any other content (FR-NOT-1). The wake-up ping is silent
 * (FR-NOT-5); high priority lets the partner's phone wake to upload its location. Anniversary and
 * milestone pushes add their number, and visible pushes add how the recipient knows their partner
 * (name and pronouns) so the text can say "Ammu sent you a memory" (FR-NOT-7).
 */
export function buildMessage(type: PushType, target: PushTarget, count?: number): FcmMessage {
  const token = target.token;
  if (type === "wake_up") {
    return { token, data: { type }, android: { priority: "HIGH", ttl: "60s", collapse_key: "wake_up" } };
  }
  const data: Record<string, string> = { type };
  if (count !== undefined) data.count = String(count);
  const name = target.partnerName?.trim().slice(0, MAX_NAME_LENGTH);
  if (name) data.partner_name = name;
  if (target.partnerPronouns && PRONOUNS.has(target.partnerPronouns)) data.partner_pronouns = target.partnerPronouns;
  return { token, data, android: { priority: "HIGH", ttl: "86400s" } };
}
