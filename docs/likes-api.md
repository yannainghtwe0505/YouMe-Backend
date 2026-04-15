# Likes, super likes, passes API

## GET `/likes`

**Auth:** Bearer  

**200:** JSON array of **outbound** likes (current user → others). Each item includes like `id`, `toUserId`, `toUserName`, `toUserAvatar`, `superLike`, `matched`, `matchId`, `createdAt` (see `LikeService.getLikesForUser`).

---

## GET `/likes/inbound`

**Auth:** Bearer  

**200:** Subscription-gated object from `LikeService.getInboundLikesPayloadForViewer`:

### Free plan

```json
{
  "plan": "free",
  "likes_count": 37,
  "locked": true,
  "placeholders": [{ "slot": 0 }, { "slot": 1 }]
}
```

No liker ids, names, or photos are included.

### Plus / Gold

```json
{
  "plan": "plus",
  "likes_count": 2,
  "locked": false,
  "likes": [
    {
      "id": 1,
      "fromUserId": 9,
      "fromUserName": "Alex",
      "fromUserAvatar": "https://...",
      "superLike": false,
      "createdAt": "2026-04-10T12:00:00Z"
    }
  ]
}
```

Gold may add `"gold_features": { "priorityLikesTeaser": true }`.

---

## POST `/likes/{toUserId}`

**Auth:** Bearer  

**200:**

```json
{ "matched": true, "matchId": 15 }
```

or `matched: false`, `matchId: null` if no mutual match yet.

---

## POST `/superlikes/{toUserId}`

**Auth:** Bearer  

Same response shape as regular like; persists `super_like` on the like row.

Controller: `SuperlikeController`.

---

## POST `/dislikes/{toUserId}`

**Auth:** Bearer  

Records a **pass** (no mutual match). **200** empty body.

Controller: `DislikeController` → `PassService`.

---

## Errors

- Like endpoints may no-op or error on self-like / block depending on service (typically guarded in `LikeService`).
