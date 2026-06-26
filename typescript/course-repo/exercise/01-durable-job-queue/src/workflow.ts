import { proxyActivities } from '@temporalio/workflow';
import type { WebhookDelivery } from './shared';
import type * as activities from './activities';

const { deliverWebhook } = proxyActivities<typeof activities>({
  startToCloseTimeout: '10 seconds',
});

// Thin Workflow that calls deliverWebhook as a step.
// The same Activity the standalone caller submits directly.
// Not used in Module 01's narrative - Module 06 uses this pattern.
export async function webhookWorkflow(req: WebhookDelivery): Promise<number> {
  return await deliverWebhook(req);
}
