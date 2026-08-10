package quickstart;

/** Shared constant: the Task Queue the Worker polls and the client submits to. */
public final class Greeting {
    /** Keep the Worker and client pointed at the same value. */
    public static final String TASK_QUEUE = "quickstart-standalone-activities";

    private Greeting() {}
}
