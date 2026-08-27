# Slot Engine

Rostyslav Nosikov

A foundation for creating slot games.

Frontend attach guide: [docs/frontend.md](docs/frontend.md). GitHub Actions runs `./gradlew test` on push to `main`.

## Implemented mechanics

- N×M grid
- Paylines (10 / 20 / 25 presets)
- Ways to win (243 / 1024)
- Wild: substitute, multiplier, expanding, sticky in free spins
- Scatter (pays anywhere)
- Free spins: awards table, retrigger, dedicated feature reels
- Buy bonus
- Ante (extra bet, separate reel set)
- Cascade / tumble with a rising multiplier
- Both-ways evaluation
- Mystery jackpot tiers
- Max-win cap

---

## What this is for

Use this repository as the **base of a slot product**:

| You keep                                      | You replace / extend                                  |
| :-------------------------------------------- | :---------------------------------------------------- |
| `SlotEngine` — pure round math                | Art, animation, sound on the frontend                 |
| JSON / `GameBuilder` game definitions         | Your titles, RTP variants, themes                     |
| REST play protocol (`spins[]`)                | Your UI kit (React, Unity, Phaser, …)                 |
| `WalletGateway` port                          | Operator wallet, Redis sessions, Postgres round ledger |
| Templates (`classic-20`, `ways-243`, …)       | Extra mechanics (Megaways, cluster, pick bonus)       |

The math server does not render reels. It returns **already-resolved rounds**. The frontend never computes lines, ways, or RTP.

---

## Strengths

- **Math is isolated.** `SlotEngine.play(game, request)` has no HTTP, no wallet, no database. Same function drives the API, the Monte Carlo simulator, and replay.
- **Money is a port, not a hard-wire.** Debit → play → credit (rollback on engine failure). Plug `SimulatedWalletGateway`, HTTP operator wallet, or your own `@Bean WalletGateway`.
- **Frontend-ready in one response.** A spin returns the full sequence: base, cascades, free spins, win positions, balance. The client only animates `spins[]`.
- **Fast to author new titles.** Templates, reel weights, `core` knobs, JSON overlays (`extendsId`) — without rewriting evaluators.
- **RTP is measured, not faked.** Runtime does not nudge outcomes. Exact cycle walk + Monte Carlo report RTP, hit rate, feature rate, volatility.
- **Replay and idempotency.** Seeded xorshift64* RNG. Same seed → same round. Same `roundId` / `Idempotency-Key` retries settlement, not a second spin.
- **Integer money.** Credits are `long`. No floating-point in payouts.
- **LIVE vs SIMULATION.** Studio tools (math API, top-up, client seed) turn off for real-money mode without forking the engine.
- **Modular Gradle layout.** Change `slot-api` or add a ledger; `slot-model` / `slot-engine` stay put.

---

## Capabilities

**Studio**

- Java `GameBuilder` and JSON math files
- Templates: `classic-10`, `classic-20`, `ways-243`, `cascade-20`
- Per-game `core` knobs (FS multiplier, buy-bonus cost, cascade, max win, …)
- RTP variants via overlay (`extendsId`)
- Workbench: apply overlay, preview simulation without saving
- CLI: `simulate` / `enumerate`

**Runtime**

- REST + OpenAPI (`/swagger`)
- In-memory session + simulated wallet (replaceable)
- CORS on `/api/**`
- Demo page at `/` that uses the same protocol as a real client

---

## Architecture

```
Frontend (any)
    │  REST
    ▼
slot-api     sessions, wallet orchestration, studio endpoints
    │
    ▼
slot-engine  SlotEngine.play — stops, wilds, lines/ways, scatters, FS, cascades
    │
slot-model   GameDefinition (immutable)
slot-math    Monte Carlo + exact base cycle (studio only)
```

Round pipeline inside the engine:

```
PlayRequest
  1. seed → SeededGameRng
  2. one stop per reel
  3. expanding wilds
  4. paylines or ways + scatters
  5. cascades while there is a line/ways win
  6. 3+ scatter → free-spin packet (resolved in this response)
  7. max-win cap
RoundResult → spins[]
```

API money loop (not inside the engine):

```
Idempotency-Key / roundId
  → wallet.debitBet(roundId:bet)
  → SlotEngine.play          # result stored before credit
  → wallet.creditWin(roundId:win)
  → engine failure after debit → wallet.rollbackBet
```

---

## Modules

| Module          | Role                                                      |
| :-------------- | :-------------------------------------------------------- |
| `slot-model`    | Symbols, strips, paytable, features, `GameBuilder`        |
| `slot-engine`   | RNG, evaluators, catalog, JSON loader, templates          |
| `slot-math`     | RTP simulation and exact enumeration                      |
| `slot-api`      | Spring Boot REST, demo UI, wallet adapters                |

---

## Run

Requires JDK 21+ (the project compiles to Java 21 bytecode).

```bash
./gradlew test
./gradlew :slot-api:bootRun
```

| URL                                   | What                                          |
| :------------------------------------ | :-------------------------------------------- |
| http://localhost:8080                 | Demo client (same API a real frontend uses)   |
| http://localhost:8080/swagger         | OpenAPI UI                                    |
| http://localhost:8080/actuator/health | Health                                        |

```bash
./gradlew :slot-math:run --args="simulate classic-fruits 100000 10"
./gradlew :slot-math:run --args="enumerate classic-fruits 10"
```

---

## Authoring a game

### Java

```java
GameDefinition game = SlotsEngine.fromTemplate("midnight-bells", "classic-20")
    .named("Midnight Bells")
    .freeSpinsMultiplier(2)
    .buyBonus(80, 3)
    .maxWin(2500)
    .targetRtp(0.96)
    .build();
```

Full builder (no template):

```java
SlotsEngine.game("golden-lynx")
    .grid(5, 3)
    .wild("WILD")
    .scatter("SCATTER")
    .standard20Paylines()
    .linePay("LYNX", 3, 30, 4, 100, 5, 500)
    .scatterPay("SCATTER", 3, 2, 4, 10, 5, 50)
    .baseReels(reels -> reels.reelWeights(/* ... */))
    .freeSpins(fs -> fs.award(3, 10).multiplier(3))
    .build();
```

### JSON (loaded from `games/` at startup)

```json
{
  "id": "midnight-bells",
  "template": "classic-20",
  "math": { "targetRtp": 0.96, "volatility": "MEDIUM", "maxWinMultiplier": 2500 },
  "core": { "freeSpinsMultiplier": 2, "buyBonusCostMultiplier": 80 },
  "reels": { "baseWeights": [ { "WILD": 1, "SCATTER": 2, "H1": 2 } ] }
}
```

RTP variant of an existing title:

```json
{
  "id": "golden-lynx-94",
  "extendsId": "golden-lynx",
  "math": { "targetRtp": 0.94, "volatility": "HIGH", "maxWinMultiplier": 4000 },
  "core": { "freeSpinsMultiplier": 2 }
}
```

Reload files: `POST /api/v1/games/reload`.

### Studio API

| Method | Path                                             | Purpose                         |
| :----- | :----------------------------------------------- | :------------------------------ |
| GET    | `/api/v1/templates`                              | List skeletons                  |
| POST   | `/api/v1/games/from-template`                    | `{ id, template, name? }`       |
| GET    | `/api/v1/games/{id}/math`                        | Weights, pays, core knobs       |
| PUT    | `/api/v1/games/{id}/math`                        | Apply overlay                   |
| POST   | `/api/v1/games/{id}/math/preview?spins=10000`    | Simulate **without** saving     |

`core` knobs: evaluation, bothWays, expanding wilds, cascade, free-spin multiplier/awards, buy bonus, ante, max win.

---

## Mathematics (how payouts are computed)

Payouts are integer credits.

| Kind    | Formula                                                                          |
| :------ | :------------------------------------------------------------------------------- |
| Payline | `paytable[symbol][count] × lineBet × extraMultiplier × wildProduct`              |
| Ways    | `paytable[symbol][count] × totalBet × Π(matches on consecutive reels) × multipliers` |
| Scatter | `scatterPay[symbol][count] × totalBet` (anywhere in the window)                  |

- `lineBet = totalBet / lineCount` on payline games.
- RTP is **not** adjusted at runtime. It follows strips + paytable. Measure it with Monte Carlo (full engine) or exact cycle (base game only, no free-spin EV).
- Max win = `totalBet × maxWinMultiplier`.

---

## LIVE vs SIMULATION

|                            | SIMULATION (default)                 | LIVE                                          |
| :------------------------- | :----------------------------------- | :-------------------------------------------- |
| Wallet                     | in-memory `SimulatedWalletGateway`   | HTTP operator or custom `WalletGateway`       |
| `credits` on session create | yes                                  | forbidden                                     |
| Client `seed`              | yes (replay / QA)                    | forbidden                                     |
| Math / import / templates  | yes                                  | 403                                           |
| Engine                     | same `SlotEngine`                    | same `SlotEngine`                             |

```yaml
# studio
slot.mode: SIMULATION
slot.wallet.provider: SIMULATED

# live: ./gradlew :slot-api:bootRun --args='--spring.profiles.active=live'
slot.mode: LIVE
slot.math-enabled: false
slot.wallet.provider: HTTP
slot.wallet.http.base-url: ${OPERATOR_WALLET_URL}
slot.wallet.http.auth-token: ${OPERATOR_WALLET_TOKEN}
```

HTTP wallet contract:

```
POST /wallet/debit     { playerId, sessionId, gameId, roundId, txId, currency, type, amount }
POST /wallet/credit    { ... }
POST /wallet/rollback  { playerId, roundId, txId, originalTxId }
GET  /wallet/balance/{playerId}
```

Custom wallet — one bean, engine unchanged:

```java
@Bean
WalletGateway walletGateway() {
    return new MyOperatorWalletGateway(client);
}
```

Round history today is in-memory (`RoundLedger`). For real money, persist that port (Postgres unique `txId`, etc.). Do not put reel math in a database.

---

## Example games

The repo ships a few **example games** only. They are not product titles — just samples so you can spin, simulate, and copy a setup.

Together they cover different **modes and settings**: paylines vs ways, free spins, sticky / expanding wilds, cascade, buy bonus, ante, and an RTP overlay (`extendsId`). List them at runtime with `GET /api/v1/games`. Add your own via a template, a JSON file in `games/`, or `GameBuilder`.

---

# Frontend guide

This section is for client developers (web, Unity, Phaser, native). You do **not** implement slot math. You call REST, then animate what the server already decided.

Live spec while the API is running: [http://localhost:8080/swagger](http://localhost:8080/swagger). CORS is enabled for `/api/**`.

A working client using this protocol ships at `/` (`slot-api/src/main/resources/static/index.html`).

## Responsibilities

| Server                                      | Frontend                                          |
| :------------------------------------------ | :------------------------------------------------ |
| Stops, windows, wins, features, balance     | Art for each symbol id                            |
| Line / ways / scatter evaluation            | Reel spin / tumble / FS animation                 |
| Free-spin packet in one HTTP response       | Play `spins[]` in order                           |
| Wallet debit/credit                         | Show `balance` and `totalWin` from the response   |
| RTP                                         | Never computed on the client                      |

Do **not**: pick random symbols, evaluate paylines, send `seed` in LIVE, or send `credits` in LIVE.

## Base URL

```
http://localhost:8080/api/v1
```

JSON, `Content-Type: application/json`.

## Boot sequence

```
GET  /runtime          → flags (LIVE vs studio)
GET  /games            → catalog
GET  /games/{id}       → grid, lines/ways, buyBonus, maxWin
GET  /games/{id}/symbols → id list for sprite mapping (optional)
POST /sessions         → sessionId + starting balance
POST /sessions/{id}/spin → every round
```

### 1. Runtime flags

```http
GET /api/v1/runtime
```

```json
{
  "operatingMode": "SIMULATION",
  "walletProvider": "SIMULATED",
  "mathEnabled": true,
  "studioEnabled": true,
  "allowClientSeed": true,
  "allowTopUp": true,
  "currency": "CREDITS"
}
```

Use this before the first session:

- If `allowTopUp` is false, omit `credits` on session create.
- If `allowClientSeed` is false, omit `seed` on spin.
- Hide math/studio UI when `mathEnabled` / `studioEnabled` are false.

### 2. Pick a game

```http
GET /api/v1/games
GET /api/v1/games/golden-lynx
```

```json
{
  "id": "golden-lynx",
  "name": "Golden Lynx",
  "grid": "5x3",
  "evaluation": "PAYLINES",
  "paylines": 20,
  "ways": null,
  "targetRtp": "0.9600",
  "volatility": "MEDIUM_HIGH",
  "maxWin": "5000x",
  "freeSpins": true,
  "buyBonus": true,
  "cascade": false
}
```

`grid` is `{reels}x{rows}` (e.g. `5x3`). Build a canvas of that size. For `WAYS`, `ways` is 243 / 1024 / …; `paylines` may be 0.

Default bet for payline games is usually `paylines` credits (1 per line). For ways games use `1` or a value from your bet ladder.

### 3. Open a session

```http
POST /api/v1/sessions
Content-Type: application/json
```

```json
{ "playerId": "player-1", "gameId": "golden-lynx", "credits": 100000 }
```

`credits` is studio-only (`allowTopUp`). In LIVE the balance comes from the operator wallet.

```json
{
  "sessionId": "6e75724b-69b6-4179-b917-14d92971e754",
  "playerId": "player-1",
  "gameId": "golden-lynx",
  "balance": 100000,
  "lastRoundId": null,
  "operatingMode": "SIMULATION",
  "currency": "CREDITS"
}
```

Keep `sessionId` for all spins.

### 4. Spin

```http
POST /api/v1/sessions/{sessionId}/spin
Content-Type: application/json
Idempotency-Key: <optional uuid>
```

```json
{ "bet": 20, "mode": "NORMAL" }
```

| Field     | Required | Notes                                                                                      |
| :-------- | :------- | :----------------------------------------------------------------------------------------- |
| `bet`     | yes      | Positive integer. On payline games must be divisible by `paylines`.                        |
| `mode`    | no       | `NORMAL` (default), `ANTE`, `BUY_BONUS`. Send ANTE / BUY_BONUS only if the game supports it. |
| `seed`    | no       | Studio / QA only. Forbidden in LIVE.                                                       |
| `roundId` | no       | Same as `Idempotency-Key`: retry settlement, do not respin.                                |

On network retry, send the **same** `Idempotency-Key` (or `roundId`). The server returns the stored round; it will not debit twice or roll new stops.

## Round response (what you animate)

```json
{
  "roundId": "3bec4f06-bf83-4e1f-bb81-1c259e8f82d3",
  "gameId": "golden-lynx",
  "mode": "NORMAL",
  "seed": 42,
  "totalBet": 20,
  "charged": 20,
  "totalWin": 52,
  "maxWinCapped": false,
  "balance": 10032,
  "operatingMode": "SIMULATION",
  "betTxId": "...:bet",
  "winTxId": "...:win",
  "currency": "CREDITS",
  "triggers": [
    { "type": "FREE_SPINS", "symbolId": "SCATTER", "count": 3, "awardedSpins": 10, "details": [] }
  ],
  "spins": [
    {
      "index": 0,
      "phase": "BASE",
      "window": [
        ["A", "K", "WILD", "TEN", "NINE"],
        ["LYNX", "A", "K", "Q", "J"],
        ["TEN", "Q", "A", "K", "NINE"]
      ],
      "stops": [12, 3, 7, 19, 8],
      "wins": [
        {
          "type": "LINE",
          "symbolId": "A",
          "ofAKind": 3,
          "waysOrLine": 0,
          "amount": 30,
          "multiplier": 1,
          "positions": [
            { "reel": 0, "row": 1 },
            { "reel": 1, "row": 1 },
            { "reel": 2, "row": 1 }
          ]
        }
      ],
      "winAmount": 30,
      "multiplier": 1,
      "triggers": [],
      "freeSpinsRemaining": 0
    }
  ]
}
```

### Window coordinates

`window` is **row-major, top to bottom**:

```
window[row][reel]
row 0 = top
reel 0 = leftmost
```

For a `5x3` game: `window.length === 3`, `window[0].length === 5`.

`positions` on a win use the same system: `{ reel, row }`. Highlight those cells.

`stops` is the stop index per reel (length = reel count). Optional for animation; useful if you drive a strip scroller from the physical strip. Symbol art is keyed by the **string ids** in `window` (`WILD`, `LYNX`, `SCATTER`, …), not by stops.

### How to play `spins[]`

Walk the array **in order**. Each element is one visual step. Free spins and cascades are already expanded — you do not call `/spin` again for them.

| `phase`             | Typical presentation                                  |
| :------------------ | :---------------------------------------------------- |
| `BASE`              | Main paid spin (reel stop)                            |
| `CASCADE`           | Winning symbols removed, new ones drop in             |
| `FREE_SPIN`         | One free spin (same grid)                             |
| `BUY_BONUS_ENTRY`   | Feature purchased; next items are free spins          |

Suggested loop:

1. Animate reels (or tumble) until they match `spin.window`.
2. Highlight `spin.wins[].positions`; sum or show `spin.winAmount`.
3. If `spin.triggers` contains `FREE_SPINS` / `RETRIGGER`, play the feature intro, then continue the array.
4. After the last item, set UI balance to `round.balance` and total win to `round.totalWin`.
5. If `maxWinCapped` is true, show a max-win banner.

`charged` may be larger than `bet` (ante 1.25×, buy bonus 100×). Debit the player using `charged`, not `bet`. Display currency from `currency`.

### Win objects

| `type`    | Meaning                | `waysOrLine`   |
| :-------- | :--------------------- | :------------- |
| `LINE`    | Payline hit            | payline index  |
| `WAYS`    | Ways hit               | number of ways |
| `SCATTER` | Anywhere scatter pay   | unused (1)     |
| `JACKPOT` | Mystery / tier award   | unused         |
| `BONUS`   | Reserved               | unused         |

`ofAKind` is the match length (3/4/5). `amount` is credits already multiplied. `multiplier` is the extra factor applied (FS × cascade × wild).

### Round-level `triggers`

Feature announcements for the whole round (and sometimes per spin):

| `type`        | Meaning                                            |
| :------------ | :------------------------------------------------- |
| `FREE_SPINS`  | Feature started (`awardedSpins`)                   |
| `RETRIGGER`   | Extra free spins during the feature                |
| `JACKPOT`     | Jackpot awarded (`details` may hold the amount)    |

## Minimal client

```javascript
const api = (path, opts = {}) =>
  fetch("/api/v1" + path, {
    headers: { "Content-Type": "application/json", ...(opts.headers || {}) },
    ...opts,
  }).then(async (res) => {
    const body = await res.json().catch(() => ({}));
    if (!res.ok) throw Object.assign(new Error(body.message || res.statusText), body);
    return body;
  });

const runtime = await api("/runtime");
const session = await api("/sessions", {
  method: "POST",
  body: JSON.stringify({
    playerId: "player-1",
    gameId: "golden-lynx",
    ...(runtime.allowTopUp ? { credits: 100000 } : {}),
  }),
});

const round = await api(`/sessions/${session.sessionId}/spin`, {
  method: "POST",
  headers: { "Idempotency-Key": crypto.randomUUID() },
  body: JSON.stringify({ bet: 20, mode: "NORMAL" }),
});

for (const spin of round.spins) {
  await animateTo(spin.window); // window[row][reel]
  highlight(spin.wins.flatMap((w) => w.positions));
  showWin(spin.winAmount, spin.phase);
}
setBalance(round.balance);
setTotalWin(round.totalWin);
```

Map each symbol id to a sprite atlas. Fetch `GET /api/v1/games/{id}/symbols` if you want the id list up front.

## Errors

Body shape:

```json
{ "error": "INSUFFICIENT_FUNDS", "message": "Insufficient funds: have 5, need 20", "details": [] }
```

| HTTP | `error`                                         | What the UI should do                                      |
| :--- | :---------------------------------------------- | :--------------------------------------------------------- |
| 400  | `BAD_REQUEST` / `VALIDATION` / `INVALID_GAME`   | Fix bet (must divide by line count) or payload             |
| 403  | `CLIENT_SEED_FORBIDDEN`                         | Drop `seed`                                                |
| 403  | `TOP_UP_FORBIDDEN`                              | Drop `credits`                                             |
| 403  | `MATH_DISABLED` / `STUDIO_DISABLED`        | Hide studio tools                                          |
| 404  | `SESSION_NOT_FOUND`                             | Create a new session                                       |
| 409  | `INSUFFICIENT_FUNDS`                            | Prompt deposit / lower bet                                 |
| 502  | `WALLET_ERROR`                                  | Operator wallet failed; retry with same idempotency key    |

## Replay

```http
GET /api/v1/rounds/{roundId}
```

Returns the stored round (balance on replay may be `-1` if the session is gone). Useful for a “show last win” screen.

In SIMULATION, posting the same `seed` on a **new** roundId produces the same windows — QA only, not for LIVE.

## What not to call from a player client

These exist for math designers and should be gated on `runtime`:

- `POST /api/v1/math/simulate`
- `POST /api/v1/math/enumerate`
- `POST /api/v1/games/from-template`
- `PUT /api/v1/games/{id}/math`
- `POST /api/v1/games/import`

A production player app only needs: `/runtime`, `/games`, `/sessions`, `/spin`, optionally `/rounds/{id}` and `/games/{id}/symbols`.
