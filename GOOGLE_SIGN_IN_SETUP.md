# Google sign-in setup (one time, about 5 minutes)

The app code is finished. Google sign-in only needs these dashboard settings. It uses Supabase's browser flow (PKCE),
so **no SHA-1, no google-services.json and no extra Android client are needed**.

Project: `https://stddhotjqljstirmrrdm.supabase.co`

## 1. Google Cloud Console
1. APIs & Services, OAuth consent screen: set app name `Nebula`, add your support email, publish (or add yourself as a test user while testing).
2. APIs & Services, Credentials, Create credentials, **OAuth client ID**, type **Web application**.
3. Authorized redirect URI (exactly):
   `https://stddhotjqljstirmrrdm.supabase.co/auth/v1/callback`
4. Copy the **Client ID** and **Client secret**.

## 2. Supabase Dashboard
1. Authentication, Sign In / Providers, **Google**: enable it, paste the Client ID and Client secret, save.
2. Authentication, URL Configuration, Redirect URLs: add `nebula://auth/*` (the same entry email links use).

## 3. Test
Install the APK, tap **Continue with Google**, pick an account. The browser tab closes itself and Nebula opens on Home.

Troubleshooting:
- `redirect_uri_mismatch` on the Google page: the URI in step 1.3 is not exactly the one above.
- Browser stays open or opens the website: `nebula://auth/*` is missing from Supabase Redirect URLs.
- "Unsupported provider": Google is not enabled in Supabase (step 2.1).
