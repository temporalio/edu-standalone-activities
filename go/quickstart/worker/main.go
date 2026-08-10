package main

import (
	"log"

	"quickstart/greet"

	"go.temporal.io/sdk/client"
	"go.temporal.io/sdk/contrib/envconfig"
	"go.temporal.io/sdk/worker"
)

func main() {
	// LoadDefaultClientOptions reads the TEMPORAL_PROFILE env var (default
	// "default"), loads that profile from the TOML config file (default OS
	// config dir, or TEMPORAL_CONFIG_FILE), and applies any TEMPORAL_*
	// overrides. No connection details are hardcoded, so the same Worker binary
	// runs against a local dev server or Temporal Cloud.
	options, err := envconfig.LoadDefaultClientOptions()
	if err != nil {
		log.Fatalln("Unable to load Temporal client options", err)
	}

	c, err := client.Dial(options)
	if err != nil {
		log.Fatalln("Unable to create client", err)
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
		log.Fatalln("Unable to start worker", err)
	}
}
