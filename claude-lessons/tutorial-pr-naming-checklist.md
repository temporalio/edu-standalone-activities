# Tutorial-PR naming checklist

When a PR adds or substantially revises a tutorial in this repo, Claude generates a ranked table of candidate titles and a ready-to-paste Slack poll, posts both as a PR comment, and lets the human pick.

The goal: pick a title that is **catchy, SEO-friendly, truthful**, and clearly signals **what the viewer will learn**.

---

## Title generation rules

Every candidate title MUST:

1. **Include the core feature name.** For this tutorial track: `Standalone Activities`. For other tracks, the canonical product or feature being taught. No undefined acronyms (`SAA` is fine in PR conversation, not in a title).
2. **Signal tutorial intent.** One of: `Tutorial`, `Hands-On`, `How to`, `From Scratch`, `Step by Step`, `Crash Course`, `Explained`. Bare topic titles without an intent signal score lower.
3. **Name the language** when the tutorial is language-specific (`Python`, `Go`, `TypeScript`, `Java`, `.NET`). Place it in parentheses or after a separator if it doesn't fit naturally.
4. **Stay ≤60 characters.** YouTube truncates around 60 on mobile. 60 is the hard ceiling; 50–55 is the sweet spot.
5. **Be truthful.** Don't promise an outcome the tutorial doesn't actually deliver. If the title says "Half the Temporal Bill," the content has to show the math.
6. **Front-load the strongest keyword** for SEO. Either the feature name, a number, or a value verb (`Cut`, `Build`, `Skip`).

Cover multiple angles so the poll respondents have real choices:

| Angle | Example hook |
|---|---|
| Cost | "Half the Temporal Bill", "Cut Temporal Costs", "Cheaper" |
| Scale | "10M Jobs/Day", "at 10M/Day" |
| How-to (SEO) | "How to Use", "How to Build" |
| Comparison | "vs Workflows", "Workflow or Standalone Activity?" |
| Project / build | "Build a Job Queue", "Stripe-Style Webhooks" |
| Question | "Do You Need...?" |
| Explainer / beginner | "Explained", "From Scratch" |
| Time-bound | "30-Minute", "in Under an Hour" |
| Year | "2026", "for 2026" |

Pick **one title per angle** for the final top-5 poll so the votes spread on intent, not phrasing.

---

## Ranking criteria

Score each candidate 0–5 on each dimension; total out of 20. Highest total wins, ties broken by character count (lower is better).

1. **SEO weight** - front-loaded feature name + tutorial signal + language. Max 5.
2. **Hook strength** - does the first 30 chars create a click? Numbers, value verbs, curiosity. Max 5.
3. **Truthfulness fit** - does the actual tutorial content back the title? Max 5.
4. **Character efficiency** - ≤50 = 5, 51–55 = 4, 56–60 = 3, >60 = 0 (disqualified). Max 5.

Aim for ~10 candidates total. Show the full ranked table in the PR comment. Pick the top 5 (one per distinct angle) for the Slack poll.

---

## PR-comment output format

The PR comment should be exactly this structure:

````markdown
## Title candidates for this tutorial

| Rank | Title | Angle | Chars | SEO | Hook | Truth | Char | Total |
|---|---|---|---|---|---|---|---|---|
| 1 | … | … | … | … | … | … | … | …/20 |
| 2 | … | … | … | … | … | … | … | …/20 |
| … | | | | | | | | |
| 10 | … | … | … | … | … | … | … | …/20 |

## Top 5 for the Slack poll

(One per angle, picked from the ranked table above. Copy the block below into Polly.)

```
Our [N]th interactive [TOPIC] tutorial is almost ready for review. Please vote on your favorite tutorial name. The goal of the name is to be catchy, SEO friendly, truthful. I also want to make sure that some of the messaging is okay with the product leaders.

1️⃣ [Title #1]
2️⃣ [Title #2]
3️⃣ [Title #3]
4️⃣ [Title #4]
5️⃣ [Title #5]
```
````

Polly auto-detects 1️⃣–5️⃣ at the start of consecutive lines and turns them into clickable vote buttons. Use those emoji characters literally; don't substitute `1.` or `(1)`.

---

## What goes in the Polly intro text

Customize the first paragraph per tutorial:

- `[N]` - ordinal of the tutorial in the series (`1st`, `2nd`, `3rd`).
- `[TOPIC]` - short topic label (`SAA`, `Nexus`, `Workflows`, etc.). Acronyms are OK inside Slack - the audience knows them.
- Keep the three-criterion stem ("catchy, SEO friendly, truthful") since that's how the team has been voting.
- Keep the product-leader sign-off line if a stakeholder review is part of the loop; drop it for purely internal-comms tutorials.

---

## When to re-run this

- New tutorial PR opened → generate from scratch.
- Existing PR materially changes scope (more modules added, audience shifts beginner↔advanced, language changes) → regenerate; old titles may no longer be truthful.
- Vote winner doesn't fit a final spec change → propose 2–3 replacements in the same format, no full re-poll needed.
