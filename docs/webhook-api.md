# Webhook API

Controller: `BillingWebhookController` (`/webhooks`).

## POST `/webhooks/stripe`

| | |
|---|---|
| **Auth** | Stripe signature in header `Stripe-Signature` |
| **Content-Type** | `application/json` |
| **Body** | Raw JSON string (Stripe event payload) |

**200** — plain text body from `subscriptionService.handleStripeWebhook` (e.g. `"ok"` or provider-specific acknowledgment).

Invalid signature or processing errors depend on `SubscriptionService` implementation (may be **400**).

---

## POST `/webhooks/apple`

**Body:** optional raw JSON (ASN v2 placeholder)

**200:**

```json
{
  "status": "accepted",
  "note": "Implement ASN v2 verification and entitlement sync (see docs/SYSTEM_SPECIFICATION.md)."
}
```

---

## POST `/webhooks/google`

**Body:** optional raw JSON (RTDN / Pub/Sub placeholder)

**200:**

```json
{
  "status": "accepted",
  "note": "Subscribe to Pub/Sub and verify notifications; see docs/SYSTEM_SPECIFICATION.md."
}
```

---

## Legacy route

`POST /subscription/webhook` on `SubscriptionController` is **deprecated**; same handler as Stripe above. Configure Stripe Dashboard to call **`/webhooks/stripe`** on the API host.
