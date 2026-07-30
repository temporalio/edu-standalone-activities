import { ApplicationFailure, activityInfo, log } from '@temporalio/activity';
import type { WebhookDelivery } from './shared';

export async function deliverWebhook(req: WebhookDelivery): Promise<number> {
  const { attempt } = activityInfo();
  log.info('Delivering webhook for event', { eventId: req.eventId, attempt });

  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    // TODO: add an Idempotency-Key header derived from req.eventId.
    // The key must be deterministic across retries - use the logical webhook
    // event id (stable), not crypto.randomUUID() (different every attempt).
    // Format: 'Idempotency-Key': `webhook:${req.eventId}`
  };

  const response = await fetch(req.url, {
    method: 'POST',
    headers,
    body: JSON.stringify(req.payload),
  });
  if (!response.ok) throw new Error(`HTTP ${response.status}`);

  // Simulate a transient failure after the POST already landed.
  // Real-world equivalent: 500 from the receiver, network drop after the
  // receiver processed it, or the process crashed right after fetch resolved.
  // Temporal retries; without an idempotency key the receiver records every attempt.
  if (attempt < 3) {
    throw ApplicationFailure.create({
      message: `Simulated transient failure on attempt ${attempt}`,
    });
  }

  return response.status;
}
