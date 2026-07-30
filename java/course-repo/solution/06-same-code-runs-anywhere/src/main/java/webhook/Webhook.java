package webhook;

/** Constants for the webhook delivery job: where the Worker polls and where deliveries go. */
public final class Webhook {
    /** The Task Queue the Worker polls and the client submits to. */
    public static final String TASK_QUEUE = "webhook-queue";

    /** The local receiver that records deliveries. */
    public static final String RECEIVER_URL = "http://localhost:9000/hooks";

    private Webhook() {}
}
