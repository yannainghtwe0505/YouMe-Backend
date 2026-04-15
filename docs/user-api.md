# User / profile API (`/me`)

Controller: `ProfileController`.

All routes require **Bearer JWT** unless noted. Principal subject is numeric `userId` as string (not `pending:*` for most profile operations; pending flows use registration controller).

## GET `/me`

Returns `MeResponse` JSON: `userId`, `email`, `phoneE164`, `registrationComplete`, `onboardingStep`, profile fields (`name`, `bio`, `age`, `location`, `distanceKm`, `education`, `work`, `hobby`, `gender`, `birthday`, `interests`, `latitude`, `longitude`, `photos`, `discoverySettings`, `lifestyle`, `minAge`, `maxAge`, `avatar`, `isPremium`, `locale`, `aiQuota`, `subscriptionPlan`, `aiEntitlements`).

**404** — no profile row for user.

---

## PUT `/me/profile`

**Body:** `ProfileEntity` partial fields; non-null fields merged into existing profile.

**200** — saved `ProfileEntity`.

---

## POST `/me/profile`

Creates profile if none exists; **400** `"Profile already exists"` if row present.

---

## PUT `/me/locale`

**Body:** `{ "locale": "en" | "ja" | "my" }`

**200** — `{ "locale": "..." }`  
**400** — invalid locale  
**404** — user missing

---

## PUT `/me/password`

**Body:** `ChangePasswordRequest` — `currentPassword`, `newPassword`

**200** — `{ "ok": true }`  
**400** — wrong current password `{ "error": "current password is incorrect" }`  
**404** — user not found

---

## DELETE `/me`

Deletes user (cascade per schema). **204** no content.

---

## PUT `/me/discovery-settings`

**Body:** JSON map; supported keys:

| Key | Type | Notes |
|-----|------|--------|
| `maxDistanceKm` | number or null | null or ≤0 clears; else clamped 1–500 |
| `latitude` / `longitude` | number | |
| `minAge` / `maxAge` | number or null | clamped 18–80 |
| `discoverySettings` | object or null | stored as JSONB |
| `lifestyle` | object or null | stored as JSONB |

**200** — echo of saved values.

**400** — invalid types.

---

## POST `/me/assistant/profile-tips`

**Body:** optional `{ "locale": "..." }` override.

**200** — `{ tips, llmConfigured, subscriptionPlan, aiQuota, aiEntitlements }`  
**403** — pending principal cannot use (`{ "error": "Finish sign-up before using this." }`)

May throw / 429-style errors via usage service when LLM configured and quota exceeded.

---

## POST `/me/upgrade` (demo / fallback)

**Body:** optional `{ "plan": "PLUS" | "GOLD" }` (default PLUS)

**200** — `{ "isPremium", "subscriptionPlan" }`  
**400** — invalid plan name  
**404** — no profile

Applies dev/demo plan via `UserSubscriptionService.applyDevDemoPlan`.
