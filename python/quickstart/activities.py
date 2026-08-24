from temporalio import activity


# A plain Activity. Nothing here knows or cares whether it was invoked as a
# Standalone Activity or from a Workflow: the same function works either way.
@activity.defn
async def greet(name: str) -> str:
    return f"Hello, {name}! This ran as a Standalone Activity."
