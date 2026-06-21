/**
 * Call start() TWICE with the same activityId, back-to-back.
 *
 * Simulates an upstream system sending the same logical event twice.
 * Without an ID conflict policy, the second call raises an error.
 *
 * Fix: pass idConflictPolicy: ActivityIdConflictPolicy.USE_EXISTING to both calls
 * so the second one quietly returns the existing handle.
 */
import { ActivityIdConflictPolicy, Client, Connection } from '@temporalio/client';
import { TASK_QUEUE, WEBHOOK_RECEIVER_URL } from './shared';

// TODO: uncomment the ActivityIdConflictPolicy import above once you're ready to set the policy.

async function start(client: Client, eventId: string, label: string) {
  console.log(`[${label}] start activityId=deliver-${eventId}`);
  try {
    const handle = await client.activity.start("deliverWebhook", {
      args: [{ url: WEBHOOK_RECEIVER_URL, payload: { eventId, type: 'dup_test' }, eventId }],
      id: `deliver-${eventId}`,
      taskQueue: TASK_QUEUE,
      startToCloseTimeout: '30 seconds',
      // TODO: add idConflictPolicy: ActivityIdConflictPolicy.USE_EXISTING
      // so the second call returns the existing handle instead of erroring.
    });
    console.log(`[${label}] handle ok (activityId=${handle.activityId})`);
    return handle;
  } catch (err) {
    console.log(`[${label}] FAILED: ${err}`);
    return null;
  }
}

async function main(eventId: string) {
  const connection = await Connection.connect({ address: 'localhost:7233' });
  const client = new Client({ connection });

  const h1 = await start(client, eventId, 'call-1');
  // Second call simulates the upstream retry/replay arriving right after the first.
  const h2 = await start(client, eventId, 'call-2');

  if (h1) { await h1.result(); console.log('[call-1] activity completed'); }
  if (h2) { await h2.result(); console.log('[call-2] activity completed'); }

  await connection.close();
}

// Suppress the unused import warning until the TODO is resolved.
void ActivityIdConflictPolicy;

const eventId = process.argv[2] ?? 'evt_dup';
main(eventId).catch((err) => {
  console.error(err);
  process.exit(1);
});
