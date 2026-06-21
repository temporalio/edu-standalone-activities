import { Client, Connection } from '@temporalio/client';
import { TASK_QUEUE, WEBHOOK_RECEIVER_URL } from './shared';

async function main(eventId: string) {
  const connection = await Connection.connect({ address: 'localhost:7233' });
  const client = new Client({ connection });

  const result = await client.activity.execute("deliverWebhook", {
    args: [{ url: WEBHOOK_RECEIVER_URL, payload: { eventId, type: 'order.created' }, eventId }],
    id: `deliver-${eventId}`,
    taskQueue: TASK_QUEUE,
    startToCloseTimeout: '30 seconds',
  });

  console.log(`Standalone activity completed with status ${result}`);
  await connection.close();
}

const eventId = process.argv[2] ?? 'evt_001';
main(eventId).catch((err) => {
  console.error(err);
  process.exit(1);
});
