package webhooks;

/** Constants shared by the Worker and the starter programs. */
public final class Shared {
  public static final String TASK_QUEUE = "webhook-queue";
  public static final String WEBHOOK_RECEIVER_URL = "http://localhost:9000/hooks";

  private Shared() {}
}
