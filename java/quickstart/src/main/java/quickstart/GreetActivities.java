package quickstart;

import io.temporal.activity.ActivityInterface;

/**
 * A regular annotated interface. Standalone vs. inside-a-Workflow is decided by
 * HOW greet is called, not how it's defined: the same Activity works either way.
 */
@ActivityInterface
public interface GreetActivities {
    String greet(String name);
}
