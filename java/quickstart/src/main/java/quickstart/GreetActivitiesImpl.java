package quickstart;

public class GreetActivitiesImpl implements GreetActivities {
    @Override
    public String greet(String name) {
        return "Hello, " + name + "! This ran as a Standalone Activity.";
    }
}
