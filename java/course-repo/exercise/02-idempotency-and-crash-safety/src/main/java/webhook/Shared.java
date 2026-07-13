package webhook;

/** Constants shared by the Worker, the client starters, and the Activity. */
public final class Shared {
    /** The Task Queue the Worker polls and the client submits to. */
    public static final String TASK_QUEUE = "webhook-queue";

    /** The local receiver that records deliveries. */
    public static final String WEBHOOK_RECEIVER_URL = "http://localhost:9000/hooks";

    private Shared() {}
}
