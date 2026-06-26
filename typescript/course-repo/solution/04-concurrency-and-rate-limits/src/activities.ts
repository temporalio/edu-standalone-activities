import { log } from '@temporalio/activity';
import type { WebhookDelivery } from './shared';

export async function deliverWebhook(req: WebhookDelivery): Promise<number> {
  log.info('Delivering webhook for event', { eventId: req.eventId, url: req.url });
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
  return response.status;
}
