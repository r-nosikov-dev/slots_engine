# Math files

JSON here is compiled into the catalog at API startup (and on `POST /api/v1/games/reload`).

Three ways to author a title:

## 1. Template + knobs (fastest)

```json
{
  "id": "midnight-bells",
  "template": "classic-20",
  "math": { "targetRtp": 0.96, "volatility": "MEDIUM", "maxWinMultiplier": 2500 },
  "core": { "freeSpinsMultiplier": 2, "buyBonusCostMultiplier": 80 },
  "reels": { "baseWeights": [ { "WILD": 1, "SCATTER": 1, "H1": 2 } ] }
}
```

Templates: `classic-10`, `classic-20`, `ways-243`, `cascade-20`.

## 2. Overlay on an existing game (RTP variant)

```json
{
  "id": "golden-lynx-94",
  "extendsId": "golden-lynx",
  "math": { "targetRtp": 0.94, "volatility": "HIGH", "maxWinMultiplier": 4000 },
  "core": { "freeSpinsMultiplier": 2 }
}
```

## 3. Full definition

`GET /api/v1/games/{id}/definition` then edit strips/paytable and `POST /api/v1/games/import`.

Workbench:

- `GET /api/v1/games/{id}/math` — weights, pays, core knobs
- `PUT /api/v1/games/{id}/math` — apply overlay
- `POST /api/v1/games/{id}/math/preview?spins=10000` — simulate without saving
