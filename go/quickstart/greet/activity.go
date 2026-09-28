// Package greet holds the Task Queue name and the Activity shared by the Worker
// and the client.
package greet

import (
	"context"
	"os"
)

// TaskQueue is the queue the Worker polls and the client submits to. Keep the
// Worker and client pointed at the same value. Reads TEMPORAL_TASK_QUEUE when
// set — e.g. a test harness isolating each run on its own queue — and otherwise
// uses the shared default, so a copy-paste user's behavior is unchanged.
var TaskQueue = taskQueue()

func taskQueue() string {
	if v := os.Getenv("TEMPORAL_TASK_QUEUE"); v != "" {
		return v
	}
	return "quickstart-standalone-activities"
}

// Greet is a plain Activity. Nothing here knows or cares whether it was invoked
// as a Standalone Activity or from a Workflow: the same function works either
// way.
func Greet(ctx context.Context, name string) (string, error) {
	return "Hello, " + name + "! This ran as a Standalone Activity.", nil
}
