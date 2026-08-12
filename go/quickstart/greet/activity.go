// Package greet holds the Task Queue name and the Activity shared by the Worker
// and the client.
package greet

import "context"

// TaskQueue is the queue the Worker polls and the client submits to. Keep the
// Worker and client pointed at the same value.
const TaskQueue = "quickstart-standalone-activities"

// Greet is a plain Activity. Nothing here knows or cares whether it was invoked
// as a Standalone Activity or from a Workflow: the same function works either
// way.
func Greet(ctx context.Context, name string) (string, error) {
	return "Hello, " + name + "! This ran as a Standalone Activity.", nil
}
