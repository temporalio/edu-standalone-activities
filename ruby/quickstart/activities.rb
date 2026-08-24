require 'temporalio/activity'

# A plain Activity. Nothing here knows or cares whether it was invoked as a
# Standalone Activity or from a Workflow: the same class works either way. The
# Activity type name defaults to the class name, "Greet".
class Greet < Temporalio::Activity::Definition
  def execute(name)
    "Hello, #{name}! This ran as a Standalone Activity."
  end
end
