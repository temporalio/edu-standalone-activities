import { proxyActivities } from '@temporalio/workflow';
import type { WebhookDelivery } from './shared';
import type * as activities from './activities';

const { deliverWebhook } = proxyActivities<typeof activities>({
  startToCloseTimeout: '10 seconds',
});

export async function webhookWorkflow(req: WebhookDelivery): Promise<number> {
  return await deliverWebhook(req);
}
