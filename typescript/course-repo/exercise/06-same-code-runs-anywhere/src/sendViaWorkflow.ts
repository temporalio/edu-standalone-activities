import { Client, Connection } from '@temporalio/client';
import { TASK_QUEUE, WEBHOOK_RECEIVER_URL } from './shared';

async function main(eventId: string) {
  const connection = await Connection.connect({ address: 'localhost:7233' });
  const client = new Client({ connection });

  const result = await client.workflow.execute('webhookWorkflow', {
    args: [{ url: WEBHOOK_RECEIVER_URL, payload: { eventId, type: 'order.created', amount: 99.99 }, eventId }],
    workflowId: `wf-${eventId}`,
    taskQueue: TASK_QUEUE,
  });

  console.log(`Workflow completed with Activity returning status ${result}`);
  await connection.close();
}

const eventId = process.argv[2] ?? 'evt_002';
main(eventId).catch((err) => {
  console.error(err);
  process.exit(1);
});
