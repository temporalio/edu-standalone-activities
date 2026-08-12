package main

import (
	"context"
	"fmt"
	"log"
	"os"
	"time"

	"quickstart/greet"

	"go.temporal.io/sdk/client"
	"go.temporal.io/sdk/contrib/envconfig"
)

func main() {
	// Keep main tiny: run() owns the deferred cleanup, and only main calls
	// log.Fatalln. Calling log.Fatalln inside run() would os.Exit and skip the
	// deferred c.Close().
	if err := run(); err != nil {
		log.Fatalln(err)
	}
}

func run() error {
	name := "Temporal"
	if len(os.Args) > 1 {
		name = os.Args[1]
	}

	// Same environment-configuration lookup the Worker uses: TEMPORAL_PROFILE
	// selects the profile, the TOML file supplies address/namespace/credentials,
	// and TEMPORAL_* env vars can override individual fields.
	options, err := envconfig.LoadDefaultClientOptions()
	if err != nil {
		return fmt.Errorf("load Temporal client options: %w", err)
	}

	c, err := client.Dial(options)
	if err != nil {
		return fmt.Errorf("create client: %w", err)
	}
	defer c.Close()

	hostPort := options.HostPort
	if hostPort == "" {
		hostPort = "localhost:7233"
	}
	namespace := options.Namespace
	if namespace == "" {
		namespace = "default"
	}
	log.Printf("Executing Standalone Activity against %s (namespace %s)...", hostPort, namespace)

	// ExecuteActivity starts a Standalone Activity: no Workflow is involved. The
	// handle blocks in Get until the Activity completes and returns its result.
	handle, err := c.ExecuteActivity(context.Background(), client.StartActivityOptions{
		ID:                  fmt.Sprintf("greet-%d", time.Now().UnixNano()),
		TaskQueue:           greet.TaskQueue,
		StartToCloseTimeout: 10 * time.Second,
	}, greet.Greet, name)
	if err != nil {
		return fmt.Errorf("start standalone activity: %w", err)
	}

	var result string
	if err := handle.Get(context.Background(), &result); err != nil {
		return fmt.Errorf("standalone activity failed: %w", err)
	}
	log.Printf("Standalone Activity result: %s", result)
	return nil
}
