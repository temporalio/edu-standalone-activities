import os

# The Task Queue the Worker polls and the client targets. Keep this in sync
# between the Worker and the client. Reads TEMPORAL_TASK_QUEUE when set — e.g. a
# test harness isolating each run on its own queue — and otherwise uses the
# shared default, so a copy-paste user's behavior is unchanged.
TASK_QUEUE = os.getenv("TEMPORAL_TASK_QUEUE", "quickstart-standalone-activities")
