package main

import (
	"fmt"
	"log"

	"quickstart/greet"

	"go.temporal.io/sdk/client"
	"go.temporal.io/sdk/contrib/envconfig"
	"go.temporal.io/sdk/worker"
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
	// LoadDefaultClientOptions reads the TEMPORAL_PROFILE env var (default
	// "default"), loads that profile from the TOML config file (default OS
	// config dir, or TEMPORAL_CONFIG_FILE), and applies any TEMPORAL_*
	// overrides. No connection details are hardcoded, so the same Worker binary
	// runs against a local dev server or Temporal Cloud.
	options, err := envconfig.LoadDefaultClientOptions()
	if err != nil {
		return fmt.Errorf("load Temporal client options: %w", err)
	}

	c, err := client.Dial(options)
	if err != nil {
		return fmt.Errorf("create client: %w", err)
	}
	defer c.Close()

	w := worker.New(c, greet.TaskQueue, worker.Options{})
	w.RegisterActivity(greet.Greet)

	hostPort := options.HostPort
	if hostPort == "" {
		hostPort = "localhost:7233"
	}
	namespace := options.Namespace
	if namespace == "" {
		namespace = "default"
	}
	log.Printf("Worker connected to %s (namespace %s), polling task queue %q", hostPort, namespace, greet.TaskQueue)

	if err := w.Run(worker.InterruptCh()); err != nil {
		return fmt.Errorf("run worker: %w", err)
	}
	return nil
}
