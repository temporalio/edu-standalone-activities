from dataclasses import dataclass, field
from typing import Any

TASK_QUEUE = "webhook-queue"
WEBHOOK_RECEIVER_URL = "http://localhost:9000/hooks"


@dataclass
class WebhookDelivery:
    url: str
    payload: dict
    event_id: str


@dataclass
class WebhookDeliveryBatch:
    url: str
    items: list[dict[str, Any]] = field(default_factory=list)
