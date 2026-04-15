# Feed, blocks, photos, and AI APIs

## GET `/feed`

**Auth:** Bearer  

**200:** JSON array of discover cards for the current user (`FeedService.feedForUser`). Shape is a `List<Map<String,Object>>` with profile fields, photos, distance, etc.

---

## POST `/blocks/{userId}`

**Auth:** Bearer  

**204** — blocked  
**400** — `{ "error": "cannot block yourself" }`

---

## DELETE `/blocks/{userId}`

**Auth:** Bearer  

**204** — unblocked  
**404** — no such block row

---

## Photos (`/photos`)

**Auth:** Bearer for all.

### GET `/photos`

**200:** Array of `{ "id", "url", "primary" }` for current user.

### DELETE `/photos/{id}`

**204** — deleted  
**404** — not found / not owned

### PUT `/photos/{id}/primary`

**200** — `{ "ok": true }`  
**404** — not found / not owned

### POST `/photos/presign`

**Query params:** `filename`, `contentType`

**200:** `{ "uploadUrl", "s3Key" }`  
**400** — max 6 photos  
**503** — presign not configured

Client must `PUT` bytes to `uploadUrl` with the same `Content-Type`.

### POST `/photos/complete`

**Query param:** `s3Key` (must match key from presign flow)

**200:** `{ "id", "s3Key" }`  
**400** — max photos

---

## AI — public caps matrix

### GET `/ai/capabilities`

**Auth:** Public  

**200:** `{ "llmConfigured": boolean, "redisUsageEnabled": boolean }`

### GET `/ai/plans`

**Auth:** Public  

**200:** Nested map: per `SubscriptionPlan`, per `AiFeature`, limits and `goldFairUseDailyCap` (see `AiCapabilitiesController`).

---

## Chat assistant (per match)

Base path: `/matches/{matchId}/assistant`  
**Auth:** Bearer  
**403** if user not in match or blocked with peer.

### POST `/matches/{matchId}/assistant/icebreaker`

**200:**

```json
{
  "created": true,
  "subscriptionPlan": "PLUS",
  "greetingQuota": { "usedToday": 0, "dailyLimit": 1, "remaining": 1, "fairUseCap": false }
}
```

### POST `/matches/{matchId}/assistant/reply-ideas`

**Body:** optional `{ "tone": "warm and playful" }`

**200:** `{ "ideas", "llmConfigured", "subscriptionPlan", "aiQuota", "aiEntitlements" }`

### POST `/matches/{matchId}/assistant/match-insight`

**200:** `{ "insight", "llmConfigured", "subscriptionPlan", "aiQuota" }`

*(Not currently called by the React SPA; available for clients.)*

---

## WebSocket chat

Not REST — see `WebSocket` endpoint **`/ws/chat?token=<JWT>`**. Broadcast payload for new messages aligns with `MessageViews.toBroadcastPayload`.
