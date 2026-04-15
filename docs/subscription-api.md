# Subscription API

Controller: `SubscriptionController` (`/subscription`). Stripe integration via `SubscriptionService` / `UserSubscriptionService`.

## GET `/subscription/plans`

**Auth:** Public (catalog for marketing UI)

**200:** Plan catalog map including `plans`, `currency`, `comparison`, etc. (`subscriptionService.getPlansCatalog()`).

---

## GET `/subscription/current`

**Auth:** Bearer  

**200:** Current effective plan, Stripe ids, period end, cancel-at-period-end flags (`subscriptionService.currentPlan`).

---

## POST `/subscription/web/checkout-session`

**Auth:** Bearer  

**Body:** `{ "targetPlan": "PLUS" | "GOLD" }` (case handled server-side)

**200:** Typically `{ "checkoutUrl": "https://checkout.stripe.com/..." }` when Stripe configured; may include `demoUpgradeAvailable` for dev fallback.

**400** — missing `targetPlan`

---

## POST `/subscription/create-payment-session`

**Deprecated** — alias of `web/checkout-session`.

---

## POST `/subscription/upgrade-confirm`

**Auth:** Bearer  

**Body:** `{ "sessionId": "cs_..." }`

**200:** Result of `confirmCheckoutSession` (plan applied, subscription row updated).

**400** — missing session id

---

## POST `/subscription/cancel`

**Auth:** Bearer  

**Body:** optional `{ "immediate": true }` — cancel now vs at period end per service.

**200:** Cancellation result map from `userSubscriptionService.cancel`.

---

## POST `/subscription/downgrade`

**Auth:** Bearer  

**Body:** `{ "targetPlan": "PLUS" | "FREE" | ... , "effective": "IMMEDIATE" | "PERIOD_END" }`

**501** — if `effective` is `PERIOD_END` (not implemented)  
**400** — missing target plan

**200:** Downgrade result from `userSubscriptionService.downgrade`.

---

## POST `/subscription/mobile/verify-purchase`

**Auth:** Bearer  

**Body:** `MobileVerifyBody` — `platform` (`ios` | `android`), optional `receiptData`, `productId`, `purchaseToken`

**200:** Verification result (implementation-dependent).

---

## POST `/subscription/restore-purchase`

**Auth:** Bearer  

Same body as mobile verify — delegates to same handler.

---

## POST `/subscription/webhook` (deprecated)

**Auth:** None (Stripe signature header)  

Prefer **`POST /webhooks/stripe`** (see [webhook-api.md](./webhook-api.md)).

---

## Demo upgrade (outside `/subscription`)

`POST /me/upgrade` — instant tier change without Stripe (see [user-api.md](./user-api.md)).
