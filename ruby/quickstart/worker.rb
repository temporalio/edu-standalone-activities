require 'temporalio/client'
require 'temporalio/worker'
require 'temporalio/env_config'
require_relative 'activities'
require_relative 'shared'

# Flush output immediately so the Worker's connection message appears right away,
# even when stdout is redirected (Ruby block-buffers a non-TTY stdout).
$stdout.sync = true

# load_client_connect_options reads the TEMPORAL_PROFILE env var (default
# "default"), loads that profile from the TOML config file (default OS config
# dir, or TEMPORAL_CONFIG_FILE), and applies any TEMPORAL_* overrides. No
# connection details are hardcoded, so the same Worker runs against a local dev
# server or Temporal Cloud.
args, kwargs = Temporalio::EnvConfig::ClientConfig.load_client_connect_options
# With no config file and no env overrides, address/namespace are nil; fall back
# to the local dev server defaults.
args[0] ||= 'localhost:7233'
args[1] ||= 'default'

client = Temporalio::Client.connect(*args, **kwargs)

worker = Temporalio::Worker.new(
  client:,
  task_queue: TASK_QUEUE,
  activities: [Greet]
)

puts "Worker connected to #{args[0]} (namespace: #{args[1]}), " \
     "polling task queue '#{TASK_QUEUE}'"

worker.run(shutdown_signals: ['SIGINT'])
