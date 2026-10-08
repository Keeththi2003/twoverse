// send-push: delivers FCM pushes to a user's partner (FR-NOT, FR-LOC-5).
// Secrets (Supabase secrets, never in Git): FIREBASE_SERVICE_ACCOUNT, PUSH_INTERNAL_SECRET.
// SUPABASE_URL, SUPABASE_ANON_KEY and SUPABASE_SERVICE_ROLE_KEY are provided by Supabase.
import { createClient } from "jsr:@supabase/supabase-js@2";
import { createFcmSender } from "./fcm.ts";
import { handle } from "./handler.ts";
import type { PushTarget } from "./message.ts";

interface TargetRow {
  fcm_token: string;
  partner_name: string | null;
  partner_pronouns: string | null;
}

/** Rows from prepare_partner_push and push_targets. */
function toTargets(data: unknown): PushTarget[] {
  return ((data as TargetRow[] | null) ?? []).map((row) => ({
    token: row.fcm_token,
    partnerName: row.partner_name,
    partnerPronouns: row.partner_pronouns,
  }));
}

const supabaseUrl = Deno.env.get("SUPABASE_URL")!;
const anonKey = Deno.env.get("SUPABASE_ANON_KEY")!;
const serviceClient = createClient(supabaseUrl, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!, {
  auth: { persistSession: false },
});
const serviceAccount = Deno.env.get("FIREBASE_SERVICE_ACCOUNT");

Deno.serve((req) =>
  handle(req, {
    internalSecret: Deno.env.get("PUSH_INTERNAL_SECRET"),
    async preparePartnerPush(authorization, kind) {
      // Runs as the caller, so the database decides who they may reach.
      const userClient = createClient(supabaseUrl, anonKey, {
        global: { headers: { Authorization: authorization } },
        auth: { persistSession: false },
      });
      const { data, error } = await userClient.rpc("prepare_partner_push", { p_kind: kind });
      if (error) {
        const known = ["not_authenticated", "not_paired", "not_allowed", "rate_limited"];
        return { error: known.includes(error.message) ? error.message : (error.code === "PGRST301" ? "not_authenticated" : "internal") };
      }
      return { targets: toTargets(data) };
    },
    async pushTargets(userIds) {
      const { data, error } = await serviceClient.rpc("push_targets", { p_user_ids: userIds });
      if (error) throw error;
      return toTargets(data);
    },
    async removeTokens(tokens) {
      await serviceClient.rpc("remove_device_tokens", { p_tokens: tokens });
    },
    send: serviceAccount ? createFcmSender(serviceAccount) : null,
  })
);
