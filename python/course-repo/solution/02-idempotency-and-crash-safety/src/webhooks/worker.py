import asyncio
from concurrent.futures import ThreadPoolExecutor

from temporalio.client import Client
from temporalio.worker import Worker

from .activities import deliver_webhook
from .shared import TASK_QUEUE


async def main() -> None:
    client = await Client.connect("localhost:7233")
    with ThreadPoolExecutor(5) as executor:
        worker = Worker(
            client,
            task_queue=TASK_QUEUE,
            activities=[deliver_webhook],
            activity_executor=executor,
        )
        print(f"Worker running on task queue '{TASK_QUEUE}'")
        await worker.run()


if __name__ == "__main__":
    asyncio.run(main())
