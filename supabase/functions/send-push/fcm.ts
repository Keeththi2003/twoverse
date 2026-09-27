import type { FcmMessage } from "./message.ts";
import type { SendResult } from "./handler.ts";

/** Fields used from the Firebase service account JSON (stored as a Supabase secret). */
interface ServiceAccount {
  project_id: string;
  client_email: string;
  private_key: string;
  token_uri: string;
}

const SCOPE = "https://www.googleapis.com/auth/firebase.messaging";

/** Sends through FCM HTTP v1, reusing the OAuth token until shortly before it expires. */
export function createFcmSender(serviceAccountJson: string): (message: FcmMessage) => Promise<SendResult> {
  const account = JSON.parse(serviceAccountJson) as ServiceAccount;
  let cached: { token: string; expiresAt: number } | null = null;

  async function accessToken(): Promise<string> {
    if (cached && cached.expiresAt > Date.now() + 60_000) return cached.token;
    const now = Math.floor(Date.now() / 1000);
    const assertion = await signJwt(account, { iss: account.client_email, scope: SCOPE, aud: account.token_uri, iat: now, exp: now + 3600 });
    const response = await fetch(account.token_uri, {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: new URLSearchParams({ grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer", assertion }),
    });
    if (!response.ok) throw new Error(`oauth_failed_${response.status}`);
    const result = await response.json() as { access_token: string; expires_in: number };
    cached = { token: result.access_token, expiresAt: Date.now() + result.expires_in * 1000 };
    return cached.token;
  }

  return async (message) => {
    const response = await fetch(`https://fcm.googleapis.com/v1/projects/${account.project_id}/messages:send`, {
      method: "POST",
      headers: { "Content-Type": "application/json", Authorization: `Bearer ${await accessToken()}` },
      body: JSON.stringify({ message }),
    });
    if (response.ok) return "ok";
    const text = await response.text();
    // The app was uninstalled or the token rotated: FCM answers 404 UNREGISTERED (or 400 for a malformed token).
    if (response.status === 404 || text.includes("UNREGISTERED") || text.includes("registration token is not a valid")) {
      return "invalid_token";
    }
    return "failed";
  };
}

async function signJwt(account: ServiceAccount, claims: Record<string, unknown>): Promise<string> {
  const encode = (value: unknown) => base64Url(new TextEncoder().encode(JSON.stringify(value)));
  const unsigned = `${encode({ alg: "RS256", typ: "JWT" })}.${encode(claims)}`;
  const pem = account.private_key.replace(/-----[^-]+-----/g, "").replace(/\s+/g, "");
  const key = await crypto.subtle.importKey(
    "pkcs8",
    Uint8Array.from(atob(pem), (c) => c.charCodeAt(0)),
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"],
  );
  const signature = await crypto.subtle.sign("RSASSA-PKCS1-v1_5", key, new TextEncoder().encode(unsigned));
  return `${unsigned}.${base64Url(new Uint8Array(signature))}`;
}

function base64Url(bytes: Uint8Array): string {
  return btoa(String.fromCharCode(...bytes)).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}
