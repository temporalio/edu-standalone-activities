import { Worker, NativeConnection } from '@temporalio/worker';
import * as activities from './activities';
import { TASK_QUEUE } from './shared';

async function main() {
  const connection = await NativeConnection.connect({ address: 'localhost:7233' });
  const worker = await Worker.create({
    connection,
    taskQueue: TASK_QUEUE,
    activities,
    maxConcurrentActivityTaskExecutions: 10,
    // Cap dispatch rate so we don't 429 the downstream service.
    // Excess work waits in the Temporal task queue on the server.
    maxActivitiesPerSecond: 2,
  });
  console.log(`Worker running on task queue '${TASK_QUEUE}' (rate cap: 2/sec)`);
  await worker.run();
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
