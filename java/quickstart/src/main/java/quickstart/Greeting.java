package quickstart;

/** Shared constant: the Task Queue the Worker polls and the client submits to. */
public final class Greeting {
    /**
     * Keep the Worker and client pointed at the same value. Reads TEMPORAL_TASK_QUEUE
     * when set — e.g. a test harness isolating each run on its own queue — else the
     * shared default, so a copy-paste user's behavior is unchanged.
     */
    public static final String TASK_QUEUE =
            System.getenv().getOrDefault("TEMPORAL_TASK_QUEUE", "quickstart-standalone-activities");

    private Greeting() {}
}
