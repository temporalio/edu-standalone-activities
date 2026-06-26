export const TASK_QUEUE = 'webhook-queue';
export const WEBHOOK_RECEIVER_URL = 'http://localhost:9000/hooks';

export interface WebhookDelivery {
  url: string;
  payload: Record<string, unknown>;
  eventId: string;
}

export interface WebhookDeliveryBatch {
  url: string;
  items: Record<string, unknown>[];
}
