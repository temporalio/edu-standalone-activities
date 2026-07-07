package main

import (
	"log"

	"standaloneactivities/exercise/05-heartbeats-and-checkpointing/webhook"

	"go.temporal.io/sdk/client"
	"go.temporal.io/sdk/worker"
)

func main() {
	c, err := client.Dial(client.Options{HostPort: "localhost:7233"})
	if err != nil {
		log.Fatalln("Unable to create client", err)
	}
	defer c.Close()

	w := worker.New(c, webhook.TaskQueue, worker.Options{})
	w.RegisterActivity(webhook.DeliverWebhookBatch)

	log.Printf("Worker running on task queue %q", webhook.TaskQueue)
	if err := w.Run(worker.InterruptCh()); err != nil {
		log.Fatalln("Unable to start worker", err)
	}
}
