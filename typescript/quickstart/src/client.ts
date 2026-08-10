import { Client, Connection } from '@temporalio/client';
import { loadClientConnectConfig } from '@temporalio/envconfig';
import { TASK_QUEUE } from './shared';

async function main() {
  // Same environment-configuration lookup the Worker uses: TEMPORAL_PROFILE
  // selects the profile, the TOML file supplies address/namespace/credentials,
  // and TEMPORAL_* env vars can override individual fields.
  const { connectionOptions, namespace } = loadClientConnectConfig();

  const connection = await Connection.connect(connectionOptions);
  const client = new Client({ connection, namespace });

  const name = process.argv[2] ?? 'Temporal';

  console.log(
    `Executing Standalone Activity against ${connectionOptions.address ?? 'localhost:7233'} ` +
      `(namespace: ${namespace ?? 'default'})...`
  );

  // client.activity.execute starts a Standalone Activity: no Workflow is
  // involved. It blocks until the Activity completes and returns its result.
  const result = await client.activity.execute('greet', {
    args: [name],
    id: `greet-${Date.now()}`,
    taskQueue: TASK_QUEUE,
    startToCloseTimeout: '10 seconds',
  });

  console.log(`Standalone Activity result: ${result}`);
  await connection.close();
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
