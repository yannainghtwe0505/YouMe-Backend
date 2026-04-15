# Auth API

Controllers: `AuthController` (`/auth`), `RegistrationController` (`/auth/registration`).

## POST `/auth/login`

| | |
|---|---|
| **Auth** | Public |
| **Content-Type** | `application/json` |

**Body (`LoginReq`):**

| Field | Type | Required | Notes |
|-------|------|----------|--------|
| `email` | string | one of email/phone | Normalized server-side |
| `phone` | string | one of email/phone | JP formats normalized to E.164 |
| `password` | string | yes | |

**200** — `{ "token", "userId", "registrationComplete", "onboardingStep" }`

**401** — `{ "error": "invalid creds" }`

---

## POST `/auth/register`

| | |
|---|---|
| **Auth** | Public (do not send stale Bearer; client strips it) |
| **Content-Type** | `multipart/form-data` |

**Form fields:**

| Field | Notes |
|-------|--------|
| `email` | |
| `password` | |
| `displayName` | |
| `photos` | Array of files (multipart) |

**200** — `{ "token", "userId" }`

---

## Registration flow (`/auth/registration`)

### POST `/auth/registration/email/send`

**Body:** `{ "email": "..." }`  
**200:** `{ "ok": true, "message": "..." }`

### POST `/auth/registration/email/verify`

**Body:** `{ "email", "code" }`  
**200:** Payload from `OnboardingRegistrationService.verifyEmailCode` (session/token fields).

### POST `/auth/registration/phone/send`

**Body:** `{ "phone", "email?" }`  
**200:** `{ "ok": true, "message": "..." }`

### POST `/auth/registration/phone/verify`

**Body:** `{ "phone", "code" }`  
**200:** Verify result object from service.

### POST `/auth/registration/password`

**Body:** `{ "pendingSessionToken", "password" }`  
**200:** Account creation result (token/user identifiers per service).

### GET `/auth/registration/tokyo-wards`

**Auth:** Public  
**200:** `{ "wards": [ ... ] }`

### GET `/auth/registration/status`

**Auth:** Bearer (user id or `pending:{id}` principal)  
**200:** Onboarding status DTO from service.

### PUT `/auth/registration/profile`

**Auth:** Bearer  
**Body:** `RegistrationProfilePatch` (partial profile draft)  
**200:** Updated status/patch result.

### POST `/auth/registration/complete`

**Auth:** Bearer  
**Content-Type:** `multipart/form-data`  
**Form:** `photos` — files  
**200:** Completion result (user/token as implemented in service).

---

## Errors

- Registration endpoints may return **400** with `{ "error": "..." }` depending on validation and service rules.
- **401/403** when JWT invalid or wrong principal type.
