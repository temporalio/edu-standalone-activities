import { log } from '@temporalio/activity';
import type { WebhookDelivery } from './shared';

// Same activity whether this runs standalone or as a step inside a Workflow.
export async function deliverWebhook(req: WebhookDelivery): Promise<number> {
  log.info('Delivering webhook for event', { eventId: req.eventId, url: req.url });
  // TODO: POST req.payload to req.url using fetch()
  // TODO: throw if the response is not ok (response.status >= 400)
  // TODO: return the HTTP status code
  throw new Error('Fill in deliverWebhook');
}
