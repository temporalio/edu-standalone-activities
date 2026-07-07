# Go SDK Standalone Activities Track — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a Go SDK Instruqt track (`go/`) mirroring the TypeScript track (`add-typescript-track` branch), teaching Temporal Standalone Activities across six webhook-delivery modules, and take it through the full pipeline (build + push image, push track).

**Architecture:** A `go/` tree parallel to `typescript/`: a single-module `course-repo` with per-module subdir-per-command Go packages, Go-ported webhook receiver + demo servers, an amd64 `golang` sandbox image with Temporal CLI, and an Instruqt track definition (track.yml + config.yml + 6 challenges) reusing the existing tabs/notes/demos.

**Tech Stack:** Go 1.24, `go.temporal.io/sdk` (≥ v1.40, target v1.45), Temporal CLI v1.7.2, Temporal Server dev (v1.31.0+), Docker buildx (linux/amd64), Instruqt CLI, GitHub Actions.

## Global Constraints

- Standalone Activities are **Public Preview**; pin `go.temporal.io/sdk` to a version with `client.ExecuteActivity` / `StartActivityOptions` (added v1.40.0; use `v1.45.0`).
- Submit a standalone job with `c.ExecuteActivity(ctx, client.StartActivityOptions{ID, TaskQueue, StartToCloseTimeout}, DeliverWebhook, req)`; block with `handle.Get(ctx, &res)`. `ID` + `TaskQueue` + one timeout are required.
- Dedup: `ActivityIDConflictPolicy: enums.ACTIVITY_ID_CONFLICT_POLICY_USE_EXISTING` from `go.temporal.io/api/enums/v1`.
- Rate cap: `worker.Options{WorkerActivitiesPerSecond: 2, MaxConcurrentActivityExecutionSize: 10}`.
- Heartbeat/resume: `activity.RecordHeartbeat(ctx, n)`; on retry `activity.HasHeartbeatDetails(ctx)` + `activity.GetHeartbeatDetails(ctx, &n)`.
- Activity-in-Workflow: `workflow.ExecuteActivity(ctx, DeliverWebhook, req).Get(ctx, &res)` under `workflow.WithActivityOptions`.
- Container name is `workshop`; every lifecycle script MUST be suffixed `-workshop`.
- Challenge dir prefixes `01`–`06` must be sequential; `slug:` in frontmatter drops the `NN-` prefix.
- Canonical tab order (every module, so `tab-N` is uniform): `tab-0` Temporal UI, `tab-1` Exercise, `tab-2` Solution, `tab-3` Terminal, `tab-4` Worker, `tab-5` Webhook receiver, `tab-6` per-module demo (M03, M05 only). Temporal UI is listed first so it is the landing tab.
- Every tab reference in assignment bodies MUST be a clickable `[button label="X" background="#444CE7"](tab-N)` where N is the label's zero-indexed position; Nexus accent `#444CE7`.
- **No em dashes** in learner-facing copy (assignment `.md`, demo HTML, diagrams). Use comma/colon/period.
- Generic framing only ("traditional job queues", "job queue" lowercase); never name competitor products; capitalize Temporal primitives (Activity, Workflow, Worker, Task Queue, Standalone Activity).
- Do NOT tell learners to save files (editor auto-saves). Pair tutorial code names with their file path in prose.
- Standalone-Activities UI over-claim guard: a *Completed* Standalone Activity does NOT show it was retried; evidence retries via Worker console logs + receiver counts, not the UI record.
- Build/push the sandbox image for **linux/amd64** only.
- Reference source to port from: files on `origin/add-typescript-track` under `typescript/` (read with `git show origin/add-typescript-track:<path>`).

---

### Task 1: Scaffold `go/course-repo` module + Module 01 code (reference module)

**Files:**
- Create: `go/course-repo/go.mod`
- Create: `go/course-repo/solution/01-durable-job-queue/webhook/shared.go`
- Create: `go/course-repo/solution/01-durable-job-queue/webhook/activity.go`
- Create: `go/course-repo/solution/01-durable-job-queue/webhook/workflow.go`
- Create: `go/course-repo/solution/01-durable-job-queue/worker/main.go`
- Create: `go/course-repo/solution/01-durable-job-queue/sendstandalone/main.go`
- Create: `go/course-repo/solution/01-durable-job-queue/sendviaworkflow/main.go`
- Create: exercise mirror at `go/course-repo/exercise/01-durable-job-queue/...` (same files; `activity.go` is a compiling TODO stub)

**Interfaces:**
- Produces (used by every later module and the assignments):
  - Package `webhook` with `const TaskQueue = "webhook-queue"`, `const WebhookReceiverURL = "http://localhost:9000/hooks"`, `type WebhookDelivery struct { URL string; Payload map[string]any; EventID string }`.
  - `func DeliverWebhook(ctx context.Context, req webhook.WebhookDelivery) (int, error)`.
  - `func WebhookWorkflow(ctx workflow.Context, req webhook.WebhookDelivery) (int, error)`.
  - Module import path pattern: `standaloneactivities/solution/01-durable-job-queue/webhook` (module root path `standaloneactivities`).

- [ ] **Step 1: Create `go/course-repo/go.mod`**

```
module standaloneactivities

go 1.24

require (
	go.temporal.io/api v1.51.0
	go.temporal.io/sdk v1.45.0
)
```

(Leave `require` minimal; `go mod tidy` in Step 8 resolves the full graph + `go.sum`. If `go.temporal.io/api` v1.51.0 does not resolve, let `go mod tidy` pick the version compatible with sdk v1.45.0.)

- [ ] **Step 2: `webhook/shared.go` (solution)**

```go
package webhook

// TaskQueue is the queue the Worker polls and the client submits to.
const TaskQueue = "webhook-queue"

// WebhookReceiverURL is the local receiver that records deliveries.
const WebhookReceiverURL = "http://localhost:9000/hooks"

// WebhookDelivery is the input to DeliverWebhook, whether run standalone or in a Workflow.
type WebhookDelivery struct {
	URL     string         `json:"url"`
	Payload map[string]any `json:"payload"`
	EventID string         `json:"eventId"`
}
```

- [ ] **Step 3: `webhook/activity.go` (solution)**

```go
package webhook

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"net/http"

	"go.temporal.io/sdk/activity"
)

// DeliverWebhook is a regular Go function. Standalone vs. inside-a-Workflow is
// decided by HOW it's called, not how it's defined.
func DeliverWebhook(ctx context.Context, req WebhookDelivery) (int, error) {
	activity.GetLogger(ctx).Info("Delivering webhook", "eventId", req.EventID, "url", req.URL)

	body, _ := json.Marshal(req.Payload)
	resp, err := http.Post(req.URL, "application/json", bytes.NewReader(body))
	if err != nil {
		return 0, err // network error: Temporal retries
	}
	defer resp.Body.Close()

	if resp.StatusCode >= 300 {
		return 0, fmt.Errorf("HTTP %d", resp.StatusCode) // 4xx/5xx: Temporal retries
	}
	return resp.StatusCode, nil
}
```

- [ ] **Step 4: `webhook/workflow.go` (solution)**

```go
package webhook

import (
	"time"

	"go.temporal.io/sdk/workflow"
)

// WebhookWorkflow runs the SAME DeliverWebhook Activity as a Workflow step.
func WebhookWorkflow(ctx workflow.Context, req WebhookDelivery) (int, error) {
	ctx = workflow.WithActivityOptions(ctx, workflow.ActivityOptions{
		StartToCloseTimeout: 10 * time.Second,
	})
	var status int
	err := workflow.ExecuteActivity(ctx, DeliverWebhook, req).Get(ctx, &status)
	return status, err
}
```

- [ ] **Step 5: `worker/main.go` (solution)**

```go
package main

import (
	"log"

	"standaloneactivities/solution/01-durable-job-queue/webhook"

	"go.temporal.io/sdk/client"
	"go.temporal.io/sdk/worker"
)

func main() {
	c, err := client.Dial(client.Options{HostPort: "localhost:7233"})
	if err != nil {
		log.Fatalln("Unable to create client", err)
	}
	defer c.Close()

	w := worker.New(c, webhook.TaskQueue, worker.Options{})
	w.RegisterActivity(webhook.DeliverWebhook)
	w.RegisterWorkflow(webhook.WebhookWorkflow)

	log.Printf("Worker running on task queue %q", webhook.TaskQueue)
	if err := w.Run(worker.InterruptCh()); err != nil {
		log.Fatalln("Unable to start worker", err)
	}
}
```

- [ ] **Step 6: `sendstandalone/main.go` (solution)**

```go
package main

import (
	"context"
	"log"
	"os"
	"time"

	"standaloneactivities/solution/01-durable-job-queue/webhook"

	"go.temporal.io/sdk/client"
)

func main() {
	eventID := "evt_001"
	if len(os.Args) > 1 {
		eventID = os.Args[1]
	}

	c, err := client.Dial(client.Options{HostPort: "localhost:7233"})
	if err != nil {
		log.Fatalln("Unable to create client", err)
	}
	defer c.Close()

	req := webhook.WebhookDelivery{
		URL:     webhook.WebhookReceiverURL,
		Payload: map[string]any{"eventId": eventID, "type": "order.created", "amount": 99.99},
		EventID: eventID,
	}

	// One API call submits the durable job. No Workflow, no broker.
	handle, err := c.ExecuteActivity(context.Background(), client.StartActivityOptions{
		ID:                  "deliver-" + eventID,
		TaskQueue:           webhook.TaskQueue,
		StartToCloseTimeout: 10 * time.Second,
	}, webhook.DeliverWebhook, req)
	if err != nil {
		log.Fatalln("Unable to start standalone activity", err)
	}

	var status int
	if err := handle.Get(context.Background(), &status); err != nil {
		log.Fatalln("Standalone activity failed", err)
	}
	log.Printf("Standalone Activity completed with status %d", status)
}
```

- [ ] **Step 7: `sendviaworkflow/main.go` (solution)**

```go
package main

import (
	"context"
	"log"
	"os"

	"standaloneactivities/solution/01-durable-job-queue/webhook"

	"go.temporal.io/sdk/client"
)

func main() {
	eventID := "evt_002"
	if len(os.Args) > 1 {
		eventID = os.Args[1]
	}

	c, err := client.Dial(client.Options{HostPort: "localhost:7233"})
	if err != nil {
		log.Fatalln("Unable to create client", err)
	}
	defer c.Close()

	req := webhook.WebhookDelivery{
		URL:     webhook.WebhookReceiverURL,
		Payload: map[string]any{"eventId": eventID, "type": "order.created", "amount": 99.99},
		EventID: eventID,
	}

	we, err := c.ExecuteWorkflow(context.Background(), client.StartWorkflowOptions{
		ID:        "wf-" + eventID,
		TaskQueue: webhook.TaskQueue,
	}, webhook.WebhookWorkflow, req)
	if err != nil {
		log.Fatalln("Unable to execute workflow", err)
	}

	var status int
	if err := we.Get(context.Background(), &status); err != nil {
		log.Fatalln("Workflow failed", err)
	}
	log.Printf("Workflow completed with Activity returning status %d", status)
}
```

- [ ] **Step 8: Create the exercise mirror**

Copy every solution file into `go/course-repo/exercise/01-durable-job-queue/` with import paths changed `solution/` → `exercise/`. Replace ONLY the `DeliverWebhook` body in the exercise `webhook/activity.go` with a compiling TODO stub:

```go
func DeliverWebhook(ctx context.Context, req WebhookDelivery) (int, error) {
	activity.GetLogger(ctx).Info("Delivering webhook", "eventId", req.EventID, "url", req.URL)
	// TODO 1: POST req.Payload as JSON to req.URL with http.Post.
	// TODO 2: return an error if the response status is >= 300 (Temporal retries).
	// TODO 3: return the response status code on success.
	return 0, fmt.Errorf("TODO: implement DeliverWebhook")
}
```

(Keep the `bytes`/`json`/`net/http` imports out of the stub if unused — the stub only needs `context`, `fmt`, and `activity`. Ensure it compiles: no unused imports.)

- [ ] **Step 9: Tidy + build**

Run (needs local Go; if absent, defer this verification to Task 9's Docker build and note it):
```bash
cd go/course-repo && go mod tidy && go build ./solution/01-durable-job-queue/... ./exercise/01-durable-job-queue/...
```
Expected: builds clean; `go.sum` created.

- [ ] **Step 10: Commit**

```bash
git add go/course-repo/go.mod go/course-repo/go.sum go/course-repo/solution/01-durable-job-queue go/course-repo/exercise/01-durable-job-queue
git commit -m "Go track: module 01 course code (durable job queue)"
```

---

### Task 2: Module 02 — idempotency and crash safety

**Files (both `solution/` and `exercise/` under `go/course-repo/.../02-idempotency-and-crash-safety/`):**
- Create: `webhook/shared.go`, `webhook/activity.go`, `worker/main.go`, `sendstandalone/main.go`

**Interfaces:**
- Consumes: none new.
- Produces: `DeliverWebhook` that sends a stable `Idempotency-Key` header.

Port from `origin/add-typescript-track:typescript/course-repo/solution/02-*`. Key Go differences from Module 01:

- [ ] **Step 1: `webhook/shared.go`** — same as Module 01 but import path base is `.../02-idempotency-and-crash-safety/webhook`. (No `workflow.go` in this module.)

- [ ] **Step 2: `webhook/activity.go` (solution)** — POST with a stable idempotency key, then throw a simulated transient failure on attempts 1-2 so Temporal retries and the same delivery is POSTed three times (the receiver dedupes attempts 2-3). Verified against the TS reference (`ApplicationFailure` on `attempt < 3`, key `webhook:<eventId>`):

```go
package webhook

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"net/http"

	"go.temporal.io/sdk/activity"
	"go.temporal.io/sdk/temporal"
)

func DeliverWebhook(ctx context.Context, req WebhookDelivery) (int, error) {
	attempt := activity.GetInfo(ctx).Attempt
	activity.GetLogger(ctx).Info("Delivering webhook", "eventId", req.EventID, "attempt", attempt)

	body, _ := json.Marshal(req.Payload)
	httpReq, err := http.NewRequestWithContext(ctx, http.MethodPost, req.URL, bytes.NewReader(body))
	if err != nil {
		return 0, err
	}
	httpReq.Header.Set("Content-Type", "application/json")
	// The event id is stable across retries, so every retry POSTs the same
	// logical delivery key and the receiver dedupes the side effect.
	httpReq.Header.Set("Idempotency-Key", "webhook:"+req.EventID)

	resp, err := http.DefaultClient.Do(httpReq)
	if err != nil {
		return 0, err
	}
	defer resp.Body.Close()
	if resp.StatusCode >= 300 {
		return 0, fmt.Errorf("HTTP %d", resp.StatusCode)
	}

	// Simulate a transient failure on attempts 1-2 so Temporal retries and the
	// same delivery is POSTed three times. A stable Idempotency-Key keeps the
	// receiver from processing it more than once.
	if attempt < 3 {
		return 0, temporal.NewApplicationError(
			fmt.Sprintf("Simulated transient failure on attempt %d", attempt), "TransientError")
	}
	return resp.StatusCode, nil
}
```

- [ ] **Step 3: `worker/main.go`** — like Module 01 but register only the Activity (no Workflow), import `.../02-.../webhook`.

- [ ] **Step 4: `sendstandalone/main.go`** — import `.../02-.../webhook` and `go.temporal.io/sdk/temporal`; `StartToCloseTimeout: 10 * time.Second` and `RetryPolicy: &temporal.RetryPolicy{MaximumAttempts: 5}` so the 3 simulated failures are all retried. Submit, then `handle.Get(ctx, &status)`, print `log.Printf("Activity completed with status %d", status)`.

- [ ] **Step 5: Exercise mirror** — copy, swap import paths to `exercise/`, and in the exercise `activity.go` replace ONLY the `Idempotency-Key` header line with a TODO (keep the simulated failure — it is the retry driver, not the thing being taught):
```go
	// TODO: set a stable "Idempotency-Key" header (e.g. "webhook:"+req.EventID)
	//       so the retries below dedupe instead of triple-delivering.
```
(The exercise version compiles and delivers, but its 3 retries all process at the receiver — that is the fail-then-fix.)

- [ ] **Step 6: Build + commit**
```bash
cd go/course-repo && go build ./solution/02-idempotency-and-crash-safety/... ./exercise/02-idempotency-and-crash-safety/...
git add go/course-repo/{solution,exercise}/02-idempotency-and-crash-safety
git commit -m "Go track: module 02 course code (idempotency + crash safety)"
```

---

### Task 3: Module 03 — dedup via ID reuse

**Files (both sides, `.../03-dedup-via-id-reuse/`):** `webhook/shared.go`, `webhook/activity.go`, `worker/main.go`, `sendstandalone/main.go`, `senddouble/main.go`

**Interfaces:**
- Produces: `senddouble` that calls `ExecuteActivity` twice with the same ID and `USE_EXISTING`.

Port from `typescript/course-repo/solution/03-*`.

- [ ] **Step 1:** `shared.go` (import base `.../03-.../webhook`), `activity.go` (same as Module 01 solution: plain deliver, no idempotency header needed here), `worker/main.go` (register Activity only).

- [ ] **Step 2: `senddouble/main.go` (solution)** — the teaching artifact:

```go
package main

import (
	"context"
	"log"
	"os"
	"time"

	"standaloneactivities/solution/03-dedup-via-id-reuse/webhook"

	enums "go.temporal.io/api/enums/v1"
	"go.temporal.io/sdk/client"
)

func start(ctx context.Context, c client.Client, eventID, label string) client.ActivityHandle {
	log.Printf("[%s] start activityId=deliver-%s", label, eventID)
	handle, err := c.ExecuteActivity(ctx, client.StartActivityOptions{
		ID:                  "deliver-" + eventID,
		TaskQueue:           webhook.TaskQueue,
		StartToCloseTimeout: 30 * time.Second,
		// USE_EXISTING: the second submit with the same ID returns the existing
		// handle instead of erroring. Server-side dedup before any Worker runs it.
		ActivityIDConflictPolicy: enums.ACTIVITY_ID_CONFLICT_POLICY_USE_EXISTING,
	}, webhook.DeliverWebhook, webhook.WebhookDelivery{
		URL:     webhook.WebhookReceiverURL,
		Payload: map[string]any{"eventId": eventID, "type": "dup_test"},
		EventID: eventID,
	})
	if err != nil {
		log.Printf("[%s] FAILED: %v", label, err)
		return nil
	}
	log.Printf("[%s] handle ok (activityId=%s runId=%s)", label, handle.GetID(), handle.GetRunID())
	return handle
}

func main() {
	eventID := "evt_dup"
	if len(os.Args) > 1 {
		eventID = os.Args[1]
	}
	c, err := client.Dial(client.Options{HostPort: "localhost:7233"})
	if err != nil {
		log.Fatalln("Unable to create client", err)
	}
	defer c.Close()

	h1 := start(context.Background(), c, eventID, "call-1")
	h2 := start(context.Background(), c, eventID, "call-2")

	var out int
	if h1 != nil {
		_ = h1.Get(context.Background(), &out)
		log.Println("[call-1] activity completed")
	}
	if h2 != nil {
		_ = h2.Get(context.Background(), &out)
		log.Println("[call-2] activity completed")
	}
}
```

Note: verify the exact handle type name during build. Docs show `handle.GetID()`/`handle.GetRunID()`/`handle.Get(ctx,&v)`; the return type of `client.ExecuteActivity` is `client.ActivityHandle` (aliased from internal `ClientActivityHandle`). If the exported name differs, adjust the `start` return type accordingly (or use `:=` and return an `interface{ GetID() string; GetRunID() string; Get(context.Context, ...any) error }`).

- [ ] **Step 3:** `sendstandalone/main.go` (single submit, `USE_EXISTING` too, import `.../03-.../webhook`).

- [ ] **Step 4: Exercise mirror** — in exercise `senddouble/main.go`, drop the `ActivityIDConflictPolicy` line and add:
```go
	// TODO: add ActivityIDConflictPolicy: enums.ACTIVITY_ID_CONFLICT_POLICY_USE_EXISTING
	// so the second submit returns the existing handle instead of erroring.
```
(Exercise version: second call errors with already-started — the fail-then-fix.)

- [ ] **Step 5: Build + commit**
```bash
git add go/course-repo/{solution,exercise}/03-dedup-via-id-reuse
git commit -m "Go track: module 03 course code (dedup via ID reuse)"
```

---

### Task 4: Module 04 — concurrency and rate limits

**Files (both sides, `.../04-concurrency-and-rate-limits/`):** `webhook/shared.go`, `webhook/activity.go`, `worker/main.go`, `sendbulk/main.go`, `sendbulkdemo/main.go`, `sendstandalone/main.go`

Port from `typescript/course-repo/solution/04-*`.

- [ ] **Step 1:** `shared.go`, `activity.go` (plain deliver), `sendstandalone/main.go` as before (import base `.../04-.../webhook`).

- [ ] **Step 2: `worker/main.go` (solution)** — the rate cap:

```go
	w := worker.New(c, webhook.TaskQueue, worker.Options{
		MaxConcurrentActivityExecutionSize: 10,
		// Cap dispatch rate so we don't 429 the downstream receiver.
		// Excess work waits in the Task Queue on the server.
		WorkerActivitiesPerSecond: 2,
	})
	w.RegisterActivity(webhook.DeliverWebhook)
	log.Printf("Worker running on task queue %q (rate cap: 2/sec)", webhook.TaskQueue)
```

- [ ] **Step 3: `sendbulk/main.go` (solution)** — fan out N deliveries concurrently and wait:

```go
package main

import (
	"context"
	"fmt"
	"log"
	"os"
	"strconv"
	"sync"
	"time"

	"standaloneactivities/solution/04-concurrency-and-rate-limits/webhook"

	"go.temporal.io/sdk/client"
)

func main() {
	count := 60
	if len(os.Args) > 1 {
		if n, err := strconv.Atoi(os.Args[1]); err == nil {
			count = n
		}
	}
	c, err := client.Dial(client.Options{HostPort: "localhost:7233"})
	if err != nil {
		log.Fatalln("Unable to create client", err)
	}
	defer c.Close()

	var wg sync.WaitGroup
	for i := 0; i < count; i++ {
		// Distinct strings: Activity ID is "bulk-000" (hyphen), eventId is "bulk_000" (underscore).
		seq := fmt.Sprintf("%03d", i)
		handle, err := c.ExecuteActivity(context.Background(), client.StartActivityOptions{
			ID:                  "bulk-" + seq,
			TaskQueue:           webhook.TaskQueue,
			StartToCloseTimeout: 30 * time.Second,
		}, webhook.DeliverWebhook, webhook.WebhookDelivery{
			URL:     webhook.WebhookReceiverURL,
			Payload: map[string]any{"eventId": "bulk_" + seq, "type": "bulk_send"},
			EventID: "bulk_" + seq,
		})
		if err != nil {
			log.Fatalln("submit failed", err)
		}
		wg.Add(1)
		go func(h client.ActivityHandle) {
			defer wg.Done()
			var out int
			_ = h.Get(context.Background(), &out)
		}(handle)
	}
	wg.Wait()
	log.Printf("All %d deliveries completed.", count)
}
```

- [ ] **Step 4: `sendbulkdemo/main.go`** — IDENTICAL to `sendbulk/main.go` except it uses `demo-%03d` activity IDs and payload `{"eventId": "demo_%03d", "type": "demo_rate_limit"}`. It does NOT arm the rate limit (verified against TS `sendBulkDemo.ts`); the assignment arms the receiver via a manual `curl -X POST http://localhost:9000/_rate_limit?limit=2` in section 2, then runs this script so leftover `demo-*` retries don't collide with the `bulk-*` IDs from sections 1 and 3.

- [ ] **Step 5: Exercise mirror** — exercise `worker/main.go` uses `worker.Options{}` with a TODO:
```go
	// TODO: add WorkerActivitiesPerSecond: 2 (and MaxConcurrentActivityExecutionSize: 10)
	// so the Worker paces dispatch and stops flooding the receiver with 429s.
```

- [ ] **Step 6: Build + commit**
```bash
git add go/course-repo/{solution,exercise}/04-concurrency-and-rate-limits
git commit -m "Go track: module 04 course code (concurrency + rate limits)"
```

---

### Task 5: Module 05 — heartbeats and checkpointing

**Files (both sides, `.../05-heartbeats-and-checkpointing/`):** `webhook/shared.go`, `webhook/activity.go`, `worker/main.go`, `sendbatch/main.go`

Port from `typescript/course-repo/solution/05-*`.

- [ ] **Step 1: `webhook/shared.go`** — add the batch type:
```go
type WebhookDeliveryBatch struct {
	URL   string           `json:"url"`
	Items []map[string]any `json:"items"`
}
```
(Keep `TaskQueue`, `WebhookReceiverURL` constants.)

- [ ] **Step 2: `webhook/activity.go` (solution)** — checkpoint per item, resume from heartbeat details:

```go
package webhook

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"time"

	"go.temporal.io/sdk/activity"
)

func DeliverWebhookBatch(ctx context.Context, req WebhookDeliveryBatch) (int, error) {
	logger := activity.GetLogger(ctx)

	// On retry, resume from the last checkpoint instead of redoing item 0..n.
	startIndex := 0
	if activity.HasHeartbeatDetails(ctx) {
		var checkpoint int
		if err := activity.GetHeartbeatDetails(ctx, &checkpoint); err == nil {
			startIndex = checkpoint
			logger.Info("Resuming from checkpoint", "startIndex", startIndex, "attempt", activity.GetInfo(ctx).Attempt)
		}
	}

	delivered := startIndex
	for i := startIndex; i < len(req.Items); i++ {
		body, _ := json.Marshal(req.Items[i])
		resp, err := http.Post(req.URL, "application/json", bytes.NewReader(body))
		if err != nil {
			return delivered, err
		}
		func() { defer resp.Body.Close() }()
		if resp.StatusCode >= 300 {
			return delivered, fmt.Errorf("HTTP %d", resp.StatusCode)
		}
		delivered++
		// Checkpoint after each item; a future retry reads this back.
		activity.RecordHeartbeat(ctx, delivered)
		time.Sleep(1 * time.Second)
	}
	return delivered, nil
}
```

- [ ] **Step 3: `worker/main.go`** — register `DeliverWebhookBatch` (Activity only), import `.../05-.../webhook`.

- [ ] **Step 4: `sendbatch/main.go` (solution)** — submit the batch with a heartbeat timeout:

```go
	items := make([]map[string]any, count)
	for i := range items {
		items[i] = map[string]any{"eventId": fmt.Sprintf("item_%03d", i), "type": "batch.delivery", "index": i}
	}
	handle, err := c.ExecuteActivity(context.Background(), client.StartActivityOptions{
		ID:                  fmt.Sprintf("deliver-batch-%d", count),
		TaskQueue:           webhook.TaskQueue,
		StartToCloseTimeout: 5 * time.Minute,
		HeartbeatTimeout:    5 * time.Second,
	}, webhook.DeliverWebhookBatch, webhook.WebhookDeliveryBatch{URL: webhook.WebhookReceiverURL, Items: items})
```
(Full main scaffold like Task 4's `sendbulk`: parse `count` arg default 10, dial, submit, `handle.Get(&delivered)`, log.)

- [ ] **Step 5: Exercise mirror** — exercise `activity.go` drops the resume block and heartbeat call:
```go
	startIndex := 0
	// TODO 1: if activity.HasHeartbeatDetails(ctx), read the checkpoint with
	//         activity.GetHeartbeatDetails(ctx, &checkpoint) and resume from it.
	...
		delivered++
	// TODO 2: activity.RecordHeartbeat(ctx, delivered) to checkpoint each item.
```
(Exercise: a killed Worker restarts the batch from 0 — the fail-then-fix.)

- [ ] **Step 6: Build + commit**
```bash
git add go/course-repo/{solution,exercise}/05-heartbeats-and-checkpointing
git commit -m "Go track: module 05 course code (heartbeats + checkpointing)"
```

---

### Task 6: Module 06 — same code runs anywhere

**Files (both sides, `.../06-same-code-runs-anywhere/`):** `webhook/shared.go`, `webhook/activity.go`, `webhook/workflow.go`, `worker/main.go`, `sendstandalone/main.go`, `sendviaworkflow/main.go`

Port from `typescript/course-repo/solution/06-*`. Structurally identical to Module 01 (both `sendstandalone` and `sendviaworkflow` submit the SAME `DeliverWebhook`), import base `.../06-.../webhook`.

- [ ] **Step 1:** Copy Module 01 solution files into Module 06 solution, swap import paths `01-durable-job-queue` → `06-same-code-runs-anywhere`. This is the capstone; both callers import `DeliverWebhook` from the one `webhook` package (guardrail check 4 depends on this).
- [ ] **Step 2:** Exercise mirror: for the capstone, the exercise can equal the solution (no fail-then-fix), OR leave a small TODO in `sendviaworkflow` (`// TODO: call the SAME DeliverWebhook via WebhookWorkflow`). Match the TS module's choice — read `typescript/instruqt/06-*/assignment.md` to confirm whether M06 has TODOs; mirror that.
- [ ] **Step 3: Build all + commit**
```bash
cd go/course-repo && go build ./...
git add go/course-repo/{solution,exercise}/06-same-code-runs-anywhere
git commit -m "Go track: module 06 course code (same code runs anywhere)"
```

---

### Task 7: Go servers (webhook receiver + demo server)

**Files:**
- Create: `go/course-repo/server/webhookreceiver/main.go`
- Create: `go/course-repo/server/demoserver/main.go`

**Interfaces:**
- Produces: an HTTP receiver on `:9000` and a static demo server on `:9001`, byte-compatible JSON shape with the TS receiver.

- [ ] **Step 1: `webhookreceiver/main.go`** — port `typescript/course-repo/server/webhookReceiver.ts` to Go `net/http`. Requirements (match exactly):
  - State (guarded by a `sync.Mutex`): `receivedCount, processedCount, dedupedCount, throttledCount, rateLimit int`, `windowTimestamps []time.Time`, `seenKeys map[string]bool`, `deliveries []Delivery{ReceivedAt string; IdempotencyKey *string; Body any}`.
  - `POST /hooks`: increment received; if rate-limited (sliding 1s window when `rateLimit>0`) increment throttled + return `429`; parse `Idempotency-Key` header; if seen, increment deduped + return 200 `{"deduped":true}`; else record key, increment processed, append delivery, return 200 `{"ok":true,"processed":N}`.
  - `POST /_reset`: zero all state, return `{"reset":true}`.
  - `POST /_rate_limit?limit=N`: set `rateLimit`, clear window, return `{"rateLimit":N}`.
  - `GET /_received`: return the stats JSON with snake_case keys: `received_count, processed_count, deduped_count, throttled_count, rate_limit, count (=processed), deliveries[]{received_at, idempotency_key, body}`.
  - `GET /`: serve the auto-refresh HTML dashboard (copy the `<style>`/stat markup from the TS `HTML_PAGE` verbatim; it is language-agnostic; ensure no em dashes).
  - Listen on `0.0.0.0:9000`.
- [ ] **Step 2: `demoserver/main.go`** — port `demoServer.ts`: serve files from `DEMOS_DIR` (env, default `/opt/workshop/demos`) on `:9001`; default `/` → `heartbeat-demo/index.html`; set `Content-Type` by extension, `X-Frame-Options: ALLOWALL`, `Access-Control-Allow-Origin: *`; 404 otherwise.
- [ ] **Step 3: Build + commit**
```bash
cd go/course-repo && go build ./server/...
git add go/course-repo/server
git commit -m "Go track: Go webhook receiver + demo server"
```

---

### Task 8: Scripts + demos + PRD + README

**Files:**
- Create: `go/course-repo/scripts/{kill-worker.sh,restart-worker.sh,reset-receiver.sh,stop-demo-and-reset.sh}`
- Create: `go/course-repo/demos/**` (copy the HTML demos)
- Create: `go/PRD.md`, `go/README.md`

- [ ] **Step 1: `scripts/kill-worker.sh`** — SIGKILL the Go worker. `go run` spawns a child compiled binary, so match broadly:
```bash
#!/usr/bin/env bash
# SIGKILL (not TERM) to guarantee a mid-flight crash for the retry/heartbeat demos.
pkill -9 -f "exe/worker" 2>/dev/null || pkill -9 -f "go run ./worker" 2>/dev/null || pkill -9 -f "/worker" 2>/dev/null || true
echo "Worker killed."
```
(Verify the pattern during Task 12's local run; `go run ./worker` compiles to a temp binary often named `worker` under `$GOCACHE`/`go-build`. Adjust the pattern to whatever `ps` shows.)

- [ ] **Step 2: Port the other 3 scripts** from `typescript/course-repo/scripts/` (read them first). `reset-receiver.sh` curls `POST /_reset`; `restart-worker.sh` kills then relaunches `go run ./worker`; `stop-demo-and-reset.sh` kills worker + resets receiver + clears rate limit.

- [ ] **Step 3: Copy demos** — reuse the existing HTML demos:
```bash
mkdir -p go/course-repo/demos
git show origin/add-typescript-track:typescript/course-repo/demos/heartbeat-demo/index.html > go/course-repo/demos/heartbeat-demo/index.html   # (mkdir subdirs first)
```
Copy `heartbeat-demo`, `heartbeat-topology`, and the Python `conflict-policy` demo (`git show origin/add-typescript-track:...` or from `python/course-repo/demo/conflict-policy/index.html`). Confirm no em dashes.

- [ ] **Step 4: `go/PRD.md`** — adapt `typescript/PRD.md`: retitle to Go, swap the SDK API notes table for the verified Go APIs (from this plan's Global Constraints), and replace the "Module resolution" section with the Go story (single `go.mod`, module cache pre-warmed at image build, `go run ./worker`).

- [ ] **Step 5: `go/README.md`** — short pointer: what the track teaches + how to run locally (`temporal server start-dev`, `go run ./server/webhookreceiver`, `cd exercise/01-... && go run ./worker`, `go run ./sendstandalone evt_001`).

- [ ] **Step 6: Commit**
```bash
git add go/course-repo/scripts go/course-repo/demos go/PRD.md go/README.md
git commit -m "Go track: scripts, demos, PRD, README"
```

---

### Task 9: Sandbox Dockerfile + image build (compile gate)

**Files:**
- Create: `go/sandbox/Dockerfile`
- Create: `go/.dockerignore`

- [ ] **Step 1: `go/sandbox/Dockerfile`**

```dockerfile
FROM golang:1.24-bookworm

RUN apt-get update && apt-get install -y --no-install-recommends \
    ca-certificates curl wget git jq procps \
    && rm -rf /var/lib/apt/lists/*

# Temporal CLI v1.7.2 ships `temporal activity` subcommands + Standalone Activities UI.
ARG TEMPORAL_VERSION=v1.7.2
RUN ARCH=$(dpkg --print-architecture) && \
    if [ "$ARCH" = "amd64" ]; then ARCH_NAME=amd64; else ARCH_NAME=arm64; fi && \
    VER="${TEMPORAL_VERSION#v}" && \
    curl -fsSL "https://github.com/temporalio/cli/releases/download/${TEMPORAL_VERSION}/temporal_cli_${VER}_linux_${ARCH_NAME}.tar.gz" \
        -o /tmp/temporal.tar.gz && \
    tar -xzf /tmp/temporal.tar.gz -C /usr/local/bin temporal && \
    rm /tmp/temporal.tar.gz && chmod +x /usr/local/bin/temporal

WORKDIR /opt/workshop
COPY course-repo/go.mod course-repo/go.sum ./
RUN go mod download

# Copy the course source and pre-warm/validate the build (compiles every solution + stub).
COPY course-repo/ .
RUN go build ./...

# Build the server binaries onto PATH.
RUN go build -o /usr/local/bin/webhook-receiver ./server/webhookreceiver && \
    go build -o /usr/local/bin/demo-server ./server/demoserver

RUN chmod +x /opt/workshop/scripts/*.sh
EXPOSE 7233 8233 9000 9001
```

- [ ] **Step 2: `go/.dockerignore`** — port from `typescript/.dockerignore` (ignore VCS, local build artifacts).

- [ ] **Step 3: Build for amd64 (this is the compile gate since local Go may be absent)**
```bash
cd go
docker buildx build --platform linux/amd64 -t saa-go:local --load ./sandbox --build-context course-repo=course-repo || \
  docker buildx build --platform linux/amd64 -t saa-go:local --load -f sandbox/Dockerfile .
```
Note: the TS Dockerfile `COPY course-repo/...` implies the build context is `go/` (parent of sandbox/ and course-repo/). Use `docker buildx build --platform linux/amd64 -f sandbox/Dockerfile -t saa-go:local --load .` from `go/`.
Expected: build succeeds through `go build ./...` (validates all module code compiles).

- [ ] **Step 4: Commit**
```bash
git add go/sandbox/Dockerfile go/.dockerignore
git commit -m "Go track: sandbox Dockerfile (golang + Temporal CLI)"
```

---

### Task 10: Instruqt track scaffold (track.yml, config.yml, track_scripts)

**Files:**
- Create: `go/instruqt/track.yml`
- Create: `go/instruqt/config.yml`
- Create: `go/instruqt/track_scripts/setup-workshop`
- Create: `go/instruqt/track_scripts/cleanup-workshop`

- [ ] **Step 1: `config.yml`**
```yaml
version: "3"
containers:
- name: workshop
  image: ghcr.io/temporalio/edu-standalone-activities-sandbox:go-latest
  shell: /bin/bash
  memory: 4096
```

- [ ] **Step 2: `track.yml`** — port `typescript/instruqt/track.yml`: change `slug: standalone-activities-go`, title "Build a Job Queue with Standalone Activities (Go)", teaser/description reworded to Go where SDK-specific, tags swap `typescript`→`go`. Keep `lab_config` block identical (modern-dark, AssignmentLeft, sidebar_size 33, `skipping_enabled: true`, `override_challenge_layout: true`). Rewrite the SDK-specific `loadingMessages` lines to Go idioms (e.g. "c.ExecuteActivity returns a handle you can Get, cancel, or describe."). Remove `id:` and `checksum:` (first push writes them).

- [ ] **Step 3: `track_scripts/setup-workshop`** — port from TS, Go-adapted:
```bash
#!/usr/bin/env bash
set -euo pipefail
mkdir -p /var/log/workshop

nohup temporal server start-dev --ip 0.0.0.0 --ui-port 8233 --db-filename /tmp/temporal.db \
  --log-level warn \
  --dynamic-config-value matching.numTaskqueueReadPartitions=1 \
  --dynamic-config-value matching.numTaskqueueWritePartitions=1 \
  > /var/log/workshop/temporal.log 2>&1 &

nohup webhook-receiver > /var/log/workshop/receiver.log 2>&1 &
DEMOS_DIR=/opt/workshop/demos nohup demo-server > /var/log/workshop/demo.log 2>&1 &

for i in $(seq 1 60); do
  if (echo > /dev/tcp/localhost/7233) 2>/dev/null && (echo > /dev/tcp/localhost/8233) 2>/dev/null \
     && (echo > /dev/tcp/localhost/9000) 2>/dev/null && (echo > /dev/tcp/localhost/9001) 2>/dev/null; then break; fi
  sleep 1
done

for NN in 01-durable-job-queue 02-idempotency-and-crash-safety 03-dedup-via-id-reuse \
          04-concurrency-and-rate-limits 05-heartbeats-and-checkpointing 06-same-code-runs-anywhere; do
  DIR=/root/workshop/exercises/$NN
  for SIDE in exercise solution; do
    mkdir -p "$DIR/$SIDE"
    cp -r /opt/workshop/$SIDE/$NN/. "$DIR/$SIDE/"
    cp -r /opt/workshop/scripts "$DIR/$SIDE/"
    chmod +x "$DIR/$SIDE/scripts/"*.sh
  done
done

# Single go.mod at workshop root so `go run ./worker` resolves the module from any exercise dir.
cp /opt/workshop/go.mod /opt/workshop/go.sum /root/workshop/ 2>/dev/null || true
echo "Workshop setup complete."
```
IMPORTANT: verify at Task 12 whether `go run ./worker` from `/root/workshop/exercises/NN/exercise` resolves the module. Since course code lives under `exercise/NN/...` in the module rooted at `/opt/workshop`, the seeded copy loses the module root. Two options, pick whichever Task 12 proves works:
  (a) Seed the WHOLE `/opt/workshop` module to `/root/workshop` (copy go.mod + go.sum + exercise/ + solution/ preserving paths), and point tabs/workdirs at `/root/workshop/exercise/NN/...`. Simpler module resolution.
  (b) Give each seeded `exercise/NN` and `solution/NN` its own generated `go.mod` (`module m` + the two requires) so `go run ./worker` works standalone; pre-warm the cache so it's offline-fast.
  **Default to (a)** — seed the entire module tree once and set tab paths accordingly. Update Task 11 tab paths to match the chosen layout.

- [ ] **Step 4: `track_scripts/cleanup-workshop`** — port from TS (kill background procs; harmless).

- [ ] **Step 5: Commit**
```bash
git add go/instruqt/track.yml go/instruqt/config.yml go/instruqt/track_scripts
git commit -m "Go track: Instruqt track scaffold + setup scripts"
```

---

### Task 11: Six challenges (assignment.md + setup/check/solve per challenge)

**Files (per NN in the six slugs):**
- Create: `go/instruqt/NN-<slug>/assignment.md`
- Create: `go/instruqt/NN-<slug>/setup-workshop`
- Create: `go/instruqt/NN-<slug>/check-workshop`
- Create: `go/instruqt/NN-<slug>/solve-workshop`

**Interfaces:**
- Consumes: the seeded layout chosen in Task 10 Step 3 (tab `path:` + terminal `workdir:` must match it).

- [ ] **Step 1: For each module, port its `assignment.md`** from `git show origin/add-typescript-track:typescript/instruqt/NN-<slug>/assignment.md`. Transform:
  - Frontmatter: keep `slug`, `type`, `title`, `teaser`, `notes`, `difficulty`, `timelimit`. Remove `id` and per-tab `id:` lines (first push assigns them). Retitle notes to "(Go)". Keep the same **tabs order** (Temporal UI first) and set each `type: code`/`terminal` tab's `path`/`workdir` to the Go seeded dirs (per Task 10 layout choice, e.g. `/root/workshop/exercise/NN-<slug>` and `/root/workshop/solution/NN-<slug>`).
  - Body: translate every code fence to Go; replace `ts-node src/worker.ts` → `go run ./worker`, `ts-node src/sendStandalone.ts evt_001` → `go run ./sendstandalone evt_001` (and `senddouble`, `sendbulk`, `sendbulkdemo`, `sendbatch`, `sendviaworkflow` accordingly). Replace file-path prose (`src/activities.ts` → `webhook/activity.go`, `src/sendStandalone.ts` → `sendstandalone/main.go`).
  - Keep all `[button ...](tab-N)` references; re-verify each N matches the label's position in THIS module's tabs list.
  - Enforce Global Constraints: no em dashes, generic job-queue framing, capitalized primitives, no "save your file", no Standalone-Activities-UI retry over-claim, keep the feedback-form footer.
  - Expected-output blocks: update to the Go log lines this plan's `main.go` files print (e.g. `Standalone Activity completed with status 200`, `Worker running on task queue "webhook-queue"`).

- [ ] **Step 2: `setup-workshop` (per challenge)** — port from TS. Most are a no-op or `cd`-noop since the track-level setup seeds everything; match what the TS per-challenge setup does (read each).

- [ ] **Step 3: `solve-workshop` (per challenge)** — cheap copy so `skipping_enabled` works:
```bash
#!/usr/bin/env bash
set -euo pipefail
NN=NN-<slug>
cp -rf /opt/workshop/solution/$NN/. /root/workshop/exercise/$NN/ 2>/dev/null || true
exit 0
```
(Adjust the destination path to the Task 10 layout choice.)

- [ ] **Step 4: `check-workshop` (per challenge)** — instant pass (exploratory track, matching existing tracks):
```bash
#!/usr/bin/env bash
# Instant pass: lessons advance freely; we don't gate on validation.
exit 0
```

- [ ] **Step 5: Verify tab-button indices** — write/run a quick script that parses each `assignment.md` frontmatter `tabs:` list and asserts every `[button label="X"](tab-N)` has `N == index_of(label)`. Fix mismatches.

- [ ] **Step 6: Commit**
```bash
git add go/instruqt/0*-*
git commit -m "Go track: six challenge assignments + lifecycle scripts"
```

---

### Task 12: Content guardrails (port verify-content.sh) + local validation

**Files:**
- Create: `go/scripts/verify-content.sh`

- [ ] **Step 1: Port `python/scripts/verify-content.sh`** to `go/scripts/verify-content.sh`. Keep checks 1-3, 5, 6 as-is (they scan `INSTRUQT_DIR`, `COURSE_DIR`, `PRD`, etc., which auto-root to `go/`). Adapt **check 4** (Module-06 "same Activity, two callers"): assert both `go/course-repo/{exercise,solution}/06-same-code-runs-anywhere/sendstandalone/main.go` and `sendviaworkflow/main.go` (via the workflow) reference `DeliverWebhook` from the same `webhook` package, instead of grepping the Python `.activities` import.

- [ ] **Step 2: Run the guardrails**
```bash
bash go/scripts/verify-content.sh
```
Expected: exit 0.

- [ ] **Step 3: Validate the Instruqt schema**
```bash
cd go/instruqt && instruqt track validate
```
Expected: exit 0. Fix any schema/script-name errors (script suffix must be `-workshop`; challenge dirs sequential; slugs match dirs minus `NN-`).

- [ ] **Step 4: Local end-to-end walk-through** (per AGENTS.md "Testing your track locally") using the `saa-go:local` image from Task 9:
```bash
docker run -d --name saa-go-test --platform linux/amd64 \
  -p 7233:7233 -p 8233:8233 -p 9000:9000 -p 9001:9001 saa-go:local
docker cp go/course-repo/. saa-go-test:/opt/workshop/
docker cp go/instruqt/track_scripts/setup-workshop saa-go-test:/tmp/setup-workshop
docker exec saa-go-test chmod +x /tmp/setup-workshop && docker exec saa-go-test /tmp/setup-workshop
# For each module, from BOTH exercise and solution, start the worker and run the starter:
docker exec -d saa-go-test bash -lc 'cd /root/workshop/solution/01-durable-job-queue && go run ./worker > /tmp/w.log 2>&1'
docker exec saa-go-test bash -lc 'cd /root/workshop/solution/01-durable-job-queue && go run ./sendstandalone evt_001'
docker exec saa-go-test bash -lc 'curl -s localhost:9000/_received'
# ...repeat per module; for M02/M05 run scripts/kill-worker.sh mid-flight and confirm retry/resume.
docker stop saa-go-test && docker rm saa-go-test
```
Expected: each module's worker starts, starters print the Go success lines, receiver counts move. Confirm `kill-worker.sh`'s pattern actually kills `go run ./worker` (adjust the pattern in Task 8 if not). Fix any bug found (this is where AGENTS.md says real bugs surface).

- [ ] **Step 5: Commit**
```bash
git add go/scripts/verify-content.sh
git commit -m "Go track: content guardrails + local validation fixes"
```

---

### Task 13: CI workflows

**Files:**
- Create: `.github/workflows/build-sandbox-go.yml`
- Create: `.github/workflows/push-track-go.yml`

- [ ] **Step 1:** Port `.github/workflows/build-sandbox-typescript.yml` (on `origin/add-typescript-track`) to `build-sandbox-go.yml`: trigger on `go/course-repo/**` + `go/sandbox/**`; `docker/build-push-action@v6` with `platforms: linux/amd64`, context `go/`, file `go/sandbox/Dockerfile`, tag `ghcr.io/temporalio/edu-standalone-activities-sandbox:go-latest`.
- [ ] **Step 2:** Port `push-track-typescript.yml` to `push-track-go.yml`: on merge to `main` touching `go/instruqt/**`/`go/course-repo/**`/`go/sandbox/**`, wait for the image build, then `cd go/instruqt && instruqt track push --force` using `INSTRUQT_TOKEN`.
- [ ] **Step 3: Commit**
```bash
git add .github/workflows/build-sandbox-go.yml .github/workflows/push-track-go.yml
git commit -m "Go track: CI (build sandbox image + push track)"
```

---

### Task 14: Full pipeline — build+push image, push track, capture IDs

**Preconditions (resolve first):**
- `gh auth refresh -h github.com -s write:packages,read:packages` (current token lacks `write:packages`) — ask the user to run via `! gh auth ...`.
- `instruqt auth login` done (confirm `instruqt config` / a whoami).

- [ ] **Step 1: Push the image to GHCR**
```bash
echo "$(gh auth token)" | docker login ghcr.io -u nadvolod --password-stdin
cd go && docker buildx build --platform linux/amd64 \
  -f sandbox/Dockerfile -t ghcr.io/temporalio/edu-standalone-activities-sandbox:go-latest --push .
```
Then set the GHCR package **Public** (one-time UI action) so Instruqt can pull. Verify with an anonymous bearer-token pull check.

- [ ] **Step 2: Create + push the track**
```bash
cd go/instruqt
instruqt track validate
instruqt track create standalone-activities-go --title "Build a Job Queue with Standalone Activities (Go)" 2>/dev/null || true
instruqt track push --force
```

- [ ] **Step 3: Capture server-assigned IDs**
```bash
cd go/instruqt && instruqt track pull   # writes id/checksum + per-tab ids
```
Merge the `id:` (track + per-challenge + per-tab) into the committed files; delete any `.remote` files.

- [ ] **Step 4: Commit the pinned IDs**
```bash
git add go/instruqt/track.yml go/instruqt/0*-*/assignment.md
git commit -m "Go track: pin Instruqt track + challenge + tab IDs"
```

---

### Task 15: PR

- [ ] **Step 1:** Push branch `add-go-track` and open a PR whose body includes the plan's Context, What-changed (per Task), and How-to-verify (`go build ./...` via Docker, `go/scripts/verify-content.sh`, `instruqt track validate`, local container walk-through) per AGENTS.md "PRs developed via plan mode". End the PR body with the Claude Code footer.
```bash
git push -u origin add-go-track
gh pr create --title "Add Go SDK Standalone Activities Instruqt track" --body "<plan-derived body>"
```

---

## Self-review notes

- **Spec coverage:** every spec section maps to a task — course code (T1-6), servers (T7), scripts/demos/PRD/README (T8), Dockerfile (T9), track scaffold (T10), challenges (T11), guardrails (T12), CI (T13), pipeline (T14), PR (T15).
- **Open verification points flagged inline** (not placeholders): exact `client.ActivityHandle` type name (T3 S2), seeded module-resolution layout (T10 S3), `kill-worker.sh` process pattern (T8 S1). Each has a concrete default + a Task-12 proof step.
- **Type consistency:** `DeliverWebhook(ctx, WebhookDelivery)(int,error)`, `WebhookWorkflow(ctx, WebhookDelivery)(int,error)`, `DeliverWebhookBatch(ctx, WebhookDeliveryBatch)(int,error)`, `client.StartActivityOptions`, `enums.ACTIVITY_ID_CONFLICT_POLICY_USE_EXISTING`, `worker.Options{WorkerActivitiesPerSecond}` used consistently across tasks.
