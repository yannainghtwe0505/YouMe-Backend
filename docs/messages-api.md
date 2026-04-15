# Matches and messages API

## GET `/matches`

**Auth:** Bearer  

**200:** JSON array of match summaries for inbox UI: `matchId`, peer identity, last message fields, `unreadCount`, etc. (`MatchQueryService.listMatchesForUser`).

---

## GET `/matches/unread-total`

**Auth:** Bearer  

**200:** `{ "total": 0 }` — aggregate unread across matches.

---

## POST `/matches/{matchId}/read`

**Auth:** Bearer  

**200:** `{ "ok": true }`  
**403** — not a participant or blocked with peer

---

## DELETE `/matches/{matchId}`

**Auth:** Bearer  

**204** — deleted for participant  
**404** — not found / not participant

---

## GET `/matches/{matchId}/messages`

**Auth:** Bearer  

**Query:** `page` (default `0`), page size 50 on server.

**200:**

```json
{
  "content": [
    {
      "id": 1,
      "body": "Hi",
      "createdAt": "...",
      "senderId": 2,
      "messageKind": "user",
      "isAssistant": false,
      "isFromCurrentUser": false
    }
  ],
  "number": 0,
  "size": 1
}
```

**403** — forbidden / blocked

Assistant rows use `messageKind: "assistant"` and may have `senderId: null`.

---

## POST `/matches/{matchId}/messages`

**Auth:** Bearer  

**Body:** `{ "body": "text" }` (validated `@NotBlank`)

**200:** `{ "id": <new message id> }`

**403** — not allowed

Also triggers realtime broadcast to WebSocket subscribers for the match.

---

## Assistant endpoints

Documented under [feed-blocks-photos-ai-api.md](./feed-blocks-photos-ai-api.md) (`/matches/{matchId}/assistant/...`).
