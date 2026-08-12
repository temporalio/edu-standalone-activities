// A plain Activity. Nothing here knows or cares whether it was invoked as a
// Standalone Activity or from a Workflow: the same function works either way.
export async function greet(name: string): Promise<string> {
  return `Hello, ${name}! This ran as a Standalone Activity.`;
}
