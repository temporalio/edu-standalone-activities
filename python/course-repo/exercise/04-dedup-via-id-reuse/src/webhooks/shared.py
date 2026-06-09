from dataclasses import dataclass

TASK_QUEUE = "webhook-queue"
ECHO_SERVER_URL = "http://localhost:9000/hooks"


@dataclass
class WebhookDelivery:
    url: str
    payload: dict
    event_id: str
