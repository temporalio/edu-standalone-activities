package webhooks;

public class WebhookActivitiesImpl implements WebhookActivities {

  // Same Activity whether this runs standalone or as a step inside a Workflow.
  @Override
  public int deliverWebhook(WebhookDelivery req) {
    System.out.println("Delivering webhook for event " + req.eventId + " to " + req.url);
    // TODO: POST req.payload to req.url as JSON (Content-Type: application/json)
    // TODO: throw if the response status is >= 400 so Temporal retries
    // TODO: return the HTTP status code
    throw new RuntimeException("Fill in deliverWebhook");
  }
}
