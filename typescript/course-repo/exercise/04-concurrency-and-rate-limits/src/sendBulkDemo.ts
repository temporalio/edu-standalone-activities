/**
 * Rate-limit pain demo: fan out N deliveries using `demo-*` activityIds.
 *
 * Separate from sendBulk.ts so leftover in-flight retries from this demo
 * do not collide with the `bulk-*` IDs used in sections 1, 3, and 4.
 */
import { Client, Connection } from '@temporalio/client';
import { TASK_QUEUE, WEBHOOK_RECEIVER_URL } from './shared';

async function main(count: number) {
  const connection = await Connection.connect({ address: 'localhost:7233' });
  const client = new Client({ connection });

  const handles = await Promise.all(
    Array.from({ length: count }, (_, i) =>
      client.activity.start("deliverWebhook", {
        args: [{ url: WEBHOOK_RECEIVER_URL, payload: { eventId: `demo_${String(i).padStart(3, '0')}`, type: 'demo_rate_limit' }, eventId: `demo_${String(i).padStart(3, '0')}` }],
        id: `demo-${String(i).padStart(3, '0')}`,
        taskQueue: TASK_QUEUE,
        startToCloseTimeout: '30 seconds',
      })
    )
  );

  await Promise.all(handles.map(h => h.result()));
  console.log(`All ${count} deliveries completed.`);
  await connection.close();
}

const count = parseInt(process.argv[2] ?? '60', 10);
main(count).catch((err) => {
  console.error(err);
  process.exit(1);
});
