package webhook

// TaskQueue is the queue the Worker polls and the client submits to.
const TaskQueue = "webhook-queue"

// WebhookReceiverURL is the local receiver that records deliveries.
const WebhookReceiverURL = "http://localhost:9000/hooks"

// WebhookDelivery is the input to DeliverWebhook, whether run standalone or in a Workflow.
type WebhookDelivery struct {
	URL     string         `json:"url"`
	Payload map[string]any `json:"payload"`
	EventID string         `json:"eventId"`
}
