package main

import (
	"context"
	"log"
	"os"

	"standaloneactivities/solution/06-same-code-runs-anywhere/webhook"

	"go.temporal.io/sdk/client"
)

func main() {
	eventID := "evt_002"
	if len(os.Args) > 1 {
		eventID = os.Args[1]
	}

	c, err := client.Dial(client.Options{HostPort: "localhost:7233"})
	if err != nil {
		log.Fatalln("Unable to create client", err)
	}
	defer c.Close()

	req := webhook.WebhookDelivery{
		URL:     webhook.WebhookReceiverURL,
		Payload: map[string]any{"eventId": eventID, "type": "order.created", "amount": 99.99},
		EventID: eventID,
	}

	we, err := c.ExecuteWorkflow(context.Background(), client.StartWorkflowOptions{
		ID:        "wf-" + eventID,
		TaskQueue: webhook.TaskQueue,
	}, webhook.WebhookWorkflow, req)
	if err != nil {
		log.Fatalln("Unable to execute workflow", err)
	}

	var status int
	if err := we.Get(context.Background(), &status); err != nil {
		log.Fatalln("Workflow failed", err)
	}
	log.Printf("Workflow completed with Activity returning status %d", status)
}
