import { ApplicationFailure, activityInfo, log } from '@temporalio/activity';
import type { WebhookDelivery } from './shared';

export async function deliverWebhook(req: WebhookDelivery): Promise<number> {
  const { attempt } = activityInfo();
  log.info('Delivering webhook for event', { eventId: req.eventId, attempt });

  // The webhook event id is stable across retries, so every retry POSTs the
  // same logical delivery key and the receiver can dedupe the side effect.
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    'Idempotency-Key': `webhook:${req.eventId}`,
  };

  const response = await fetch(req.url, {
    method: 'POST',
    headers,
    body: JSON.stringify(req.payload),
  });
  if (!response.ok) throw new Error(`HTTP ${response.status}`);

  // Simulate transient failure on attempts 1-2.
  if (attempt < 3) {
    throw ApplicationFailure.create({
      message: `Simulated transient failure on attempt ${attempt}`,
    });
  }

  return response.status;
}
