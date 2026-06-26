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
    // TODO: cap how many activities this worker dispatches per second so
    // we don't overwhelm the downstream service.
    // The Worker option is: maxActivitiesPerSecond: <number>
    // Try 2 first to match the receiver's 2 req/sec rate limit.
  });
  console.log(`Worker running on task queue '${TASK_QUEUE}'`);
  await worker.run();
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
