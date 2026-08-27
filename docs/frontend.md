# Frontend guide

How to attach a client (web, Unity, Phaser, native) to Slot Engine.

The canonical copy of this guide lives in the root [README](../README.md#frontend-guide) (section **Frontend guide**). OpenAPI while the server runs: http://localhost:8080/swagger

**One-line rule:** the server returns finished rounds; the client only animates `spins[]`.

Quick path:

1. `GET /api/v1/runtime`
2. `GET /api/v1/games`
3. `POST /api/v1/sessions`
4. `POST /api/v1/sessions/{id}/spin`
5. Play `response.spins` in order; `window[row][reel]` is top-to-bottom, left-to-right.

See the README for full payloads, phases, win types, errors, and a copy-paste `fetch` example.
