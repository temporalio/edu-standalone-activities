import { CancelledFailure, activityInfo, heartbeat, log } from '@temporalio/activity';
import type { WebhookDeliveryBatch } from './shared';

export async function deliverWebhookBatch(req: WebhookDeliveryBatch): Promise<number> {
  const { attempt, heartbeatDetails } = activityInfo();

  // On retry, heartbeatDetails holds the last value passed to heartbeat()
  // in the previous attempt. We checkpoint the count of delivered items,
  // so the retry resumes from there instead of redoing everything from item 0.
  let startIndex = 0;
  if (heartbeatDetails != null) {
    startIndex = heartbeatDetails as number;
    log.info('Resuming from checkpoint', { startIndex, attempt });
  }

  let delivered = startIndex;
  try {
    for (let i = startIndex; i < req.items.length; i++) {
      const item = req.items[i];
      const response = await fetch(req.url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(item),
      });
      if (!response.ok) throw new Error(`HTTP ${response.status}`);
      delivered++;
      // Checkpoint after each item. Temporal stores this so a future
      // retry reads it back from activityInfo().heartbeatDetails.
      // heartbeat() is also where cancellation is delivered.
      heartbeat(delivered);
      await new Promise(r => setTimeout(r, 1000));
    }
  } catch (err) {
    if (err instanceof CancelledFailure) {
      log.info('Cancelled', { delivered, total: req.items.length, attempt });
      throw err;
    }
    throw err;
  }

  return delivered;
}
