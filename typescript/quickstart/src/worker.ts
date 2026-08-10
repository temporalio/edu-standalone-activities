import { NativeConnection, Worker } from '@temporalio/worker';
import { loadClientConnectConfig } from '@temporalio/envconfig';
import * as activities from './activities';
import { TASK_QUEUE } from './shared';

async function main() {
  const { connectionOptions, namespace } = loadClientConnectConfig();

  const connection = await NativeConnection.connect(connectionOptions);
  try {
    const worker = await Worker.create({
      connection,
      namespace,
      taskQueue: TASK_QUEUE,
      activities,
    });

    console.log(
      `Worker connected to ${connectionOptions.address ?? 'localhost:7233'} ` +
        `(namespace: ${namespace ?? 'default'}), polling task queue '${TASK_QUEUE}'`
    );

    await worker.run();
  } finally {
    await connection.close();
  }
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
