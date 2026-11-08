# Google sign-in setup

Nebula's Android Google sign-in uses Supabase Auth's hosted OAuth flow with PKCE and returns to the app through the `nebula://auth/callback` deep link.

## Google Cloud

1. Open [Google Auth Platform → Clients](https://console.cloud.google.com/auth/clients).
2. Create an OAuth client with application type **Web application**.
3. Add this exact URI under **Authorized redirect URIs**:

   `https://stddhotjqljstirmrrdm.supabase.co/auth/v1/callback`

4. Save the Web client ID and client secret.

For this browser-based PKCE flow, an Android OAuth client ID is not required by the app's configured redirect flow. Do not substitute a Desktop client ID.

## Supabase

1. Open **Authentication → Sign In / Providers → Google**.
2. Enable Google.
3. Add the Web OAuth client ID and Web client secret.
4. Save the provider.
5. Open **Authentication → URL Configuration → Redirect URLs**.
6. Add:

   `nebula://auth/*`

The Google callback URL belongs in Google Cloud's **Authorized redirect URIs**. The `nebula://auth/*` URL belongs in Supabase's **Redirect URLs** allowlist. They serve different steps of the flow.

## App configuration

- Application ID: `com.nebula.app`
- Supabase project: `https://stddhotjqljstirmrrdm.supabase.co`
- Deep link: `nebula://auth/callback`

The app receives the OAuth callback through its Android manifest intent filter and completes the PKCE session exchange.

## Security

- Never commit the Google Web client secret to the repository or include it in the APK.
- Use only the Supabase anon/publishable key in the Android client.
- Never use the Supabase service-role key in a mobile app.
- Treat OAuth tokens and session tokens as secrets; do not paste them into issue reports or logs.

## Troubleshooting

| Symptom | Check |
|---|---|
| Google reports `redirect_uri_mismatch` | The Google Cloud Authorized redirect URI must exactly match the Supabase callback URL above. |
| Supabase rejects the redirect | Add `nebula://auth/*` to Supabase Redirect URLs. |
| Google provider is unavailable | Confirm Google is enabled and the Web client ID/secret are saved in Supabase. |
| App does not reopen | Confirm the `nebula` scheme intent filter is present in the app manifest and the installed build includes it. |
