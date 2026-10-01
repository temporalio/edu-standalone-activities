// The Task Queue the Worker polls and the client targets. Keep this in sync
// between the Worker and the client.
export const TASK_QUEUE = process.env.TEMPORAL_TASK_QUEUE || 'quickstart-standalone-activities';
