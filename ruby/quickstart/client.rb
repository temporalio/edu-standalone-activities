require 'temporalio/client'
require 'temporalio/env_config'
require_relative 'activities'
require_relative 'shared'

name = ARGV[0] || 'Temporal'

# Same environment-configuration lookup the Worker uses: TEMPORAL_PROFILE selects
# the profile, the TOML file supplies address/namespace/credentials, and
# TEMPORAL_* env vars can override individual fields.
args, kwargs = Temporalio::EnvConfig::ClientConfig.load_client_connect_options
args[0] ||= 'localhost:7233'
args[1] ||= 'default'

client = Temporalio::Client.connect(*args, **kwargs)

puts "Executing Standalone Activity against #{args[0]} (namespace: #{args[1]})..."

# client.execute_activity starts a Standalone Activity: no Workflow is involved.
# It durably enqueues the Activity, waits for a Worker to run it, and returns its
# result.
result = client.execute_activity(
  Greet,
  name,
  id: "greet-#{(Time.now.to_f * 1000).to_i}",
  task_queue: TASK_QUEUE,
  start_to_close_timeout: 10
)

puts "Standalone Activity result: #{result}"
