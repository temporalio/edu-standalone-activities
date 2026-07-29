import { CancelledFailure, activityInfo, heartbeat, log } from '@temporalio/activity';
import type { WebhookDeliveryBatch } from './shared';

// Process a list of webhooks as a single long-running Standalone Activity.
// Heartbeat progress after each delivery so a retry can resume from the
// last checkpoint instead of redoing items already delivered.
export async function deliverWebhookBatch(req: WebhookDeliveryBatch): Promise<number> {
  const { attempt, heartbeatDetails } = activityInfo();

  // TODO: on retry, read heartbeatDetails to find the last reported progress
  // and resume from there. Without this read, every retry starts at item 0
  // even though the previous attempt heartbeated its progress.
  let startIndex = 0;

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
      // Report progress to Temporal - the server stores this so the NEXT attempt
      // can read it back from activityInfo().heartbeatDetails.
      // heartbeat() is also where cancellation is delivered: if the
      // Activity has been cancelled, the next call throws CancelledFailure.
      heartbeat(delivered);
      // Slow enough that you can run kill-worker.sh mid-batch in the demo.
      await new Promise(r => setTimeout(r, 1000));
    }
  } catch (err) {
    if (err instanceof CancelledFailure) {
      log.info('Cancelled', { delivered, total: req.items.length, attempt });
      throw err;
    }
    throw err;
  }

  void heartbeatDetails; // heartbeatDetails is the value passed to heartbeat() -- suppress until TODO filled
  return delivered;
}
