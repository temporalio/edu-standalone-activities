/**
 * Submit a batch webhook delivery as a single long-running Standalone Activity.
 *
 * The Activity heartbeats progress between items. Kill the Worker mid-batch
 * and the next attempt resumes from the last reported checkpoint instead
 * of redoing items already delivered.
 */
import { Client, Connection } from '@temporalio/client';
import { TASK_QUEUE, WEBHOOK_RECEIVER_URL } from './shared';

async function main(count: number) {
  const connection = await Connection.connect({ address: 'localhost:7233' });
  const client = new Client({ connection });

  const items = Array.from({ length: count }, (_, i) => ({
    eventId: `item_${String(i).padStart(3, '0')}`,
    type: 'batch.delivery',
    index: i,
  }));

  const result = await client.activity.execute("deliverWebhookBatch", {
    args: [{ url: WEBHOOK_RECEIVER_URL, items }],
    id: `deliver-batch-${count}`,
    taskQueue: TASK_QUEUE,
    startToCloseTimeout: '5 minutes',
    // heartbeatTimeout: if no heartbeat for 5s, Temporal treats the
    // attempt as dead and retries - picking up from the last checkpoint.
    heartbeatTimeout: '5 seconds',
  });

  console.log(`Batch delivery completed: ${result} items delivered.`);
  await connection.close();
}

const count = parseInt(process.argv[2] ?? '10', 10);
main(count).catch((err) => {
  console.error(err);
  process.exit(1);
});
