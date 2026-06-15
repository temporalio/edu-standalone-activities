import asyncio
import logging
from concurrent.futures import ThreadPoolExecutor

from temporalio.client import Client
from temporalio.worker import Worker

from .activities import deliver_webhook
from .shared import TASK_QUEUE


# Show activity.logger.info(...) output; default WARNING would drop INFO.
logging.basicConfig(level=logging.INFO)


async def main() -> None:
    client = await Client.connect("localhost:7233")
    with ThreadPoolExecutor(10) as executor:
        worker = Worker(
            client,
            task_queue=TASK_QUEUE,
            activities=[deliver_webhook],
            activity_executor=executor,
            # Cap dispatch rate so we don't 429 the downstream service.
            # The server holds excess work durably in the task queue.
            max_activities_per_second=2.0,
        )
        print(f"Worker running on task queue '{TASK_QUEUE}' (rate cap: 2/sec)")
        await worker.run()


if __name__ == "__main__":
    asyncio.run(main())
