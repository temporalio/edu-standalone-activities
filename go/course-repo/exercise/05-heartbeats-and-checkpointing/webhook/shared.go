package webhook

// TaskQueue is the queue the Worker polls and the client submits to.
const TaskQueue = "webhook-queue"

// WebhookReceiverURL is the local receiver that records deliveries.
const WebhookReceiverURL = "http://localhost:9000/hooks"

// WebhookDeliveryBatch is the input to DeliverWebhookBatch: a list of items to
// POST one at a time, with per-item progress checkpointed via Heartbeat.
type WebhookDeliveryBatch struct {
	URL   string           `json:"url"`
	Items []map[string]any `json:"items"`
}
