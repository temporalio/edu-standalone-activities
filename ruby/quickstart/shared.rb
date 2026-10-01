# The Task Queue the Worker polls and the client targets. Keep this in sync
# between the Worker and the client.
TASK_QUEUE = (ENV['TEMPORAL_TASK_QUEUE'] || 'quickstart-standalone-activities').freeze
