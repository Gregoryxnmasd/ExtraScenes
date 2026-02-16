# ExtraScenes — Professional Stabilization Brief for Codex

## Goal
Deliver a **production-grade, professional-quality** cinematic plugin experience where camera, actor recording/playback, NPC visibility, and GUI workflows are reliable, deterministic, and intuitive.

This brief translates current user-reported failures into a structured execution plan that should be implemented **in phases**, with strong validation after each phase.

---

## Product Quality Standard (Non-Negotiable)
Codex must treat this as a commercial-grade plugin quality target:

- No placeholder actions that only send "done"/"updated" chat messages.
- Every visible GUI button must produce a meaningful and testable state change.
- No orphan entities/NPCs, no leaked preview actors, no cross-player interference.
- Camera movement must be smooth and stable (no teleport jitter, no "moved too quickly" spam).
- Recording must have deterministic start/stop timing and optional countdown UX.
- Actor scale, nameplate visibility, and playback behavior must match configuration exactly.

---

## High-Priority Defect List (From User Reports)

### 1) Camera Control and View Locking
- Player can still move camera freely during cinematic when they should be locked.
- Invisible armor stand spectating behavior is not working as expected.
- Carved pumpkin head effect with extreme negative speed does not produce the desired visible zoom effect.
- Need visible zoom-like effect via **Slowness** (or equivalent reliable method), not an invisible/no-op effect.
- Camera movement is not smooth; it teleports and triggers "moved too quickly" errors.
- Camera should remain perfectly stable on target (no micro movement from player mouse input).

### 2) Actor Recording Lifecycle
- No countdown before recording starts.
- No fixed recording duration that auto-stops at configured seconds/ticks.
- User wants both command and GUI integration to configure recording duration.
- During recording, action bar should display elapsed/limit format (example: `3/15s`).
- Need easier stop-recording UX than manually typing a command (typing adds unwanted idle frames).

### 3) NPC Spawn/Playback Consistency
- Creating a second actor while recording can produce duplicate NPCs from previous actor.
- One duplicate stays static forever and never despawns.
- On play with multiple actors, only second actor may appear; first actor missing while ghost NPC remains.
- Scene preview NPCs appear but do not disappear afterward.
- Preview should only include actors that already have valid animation data.

### 4) Actor Visual Properties
- Actor scale setting does not apply; actor remains normal size.
- NPC name tag should be hidden but is visible.

### 5) Per-Player Isolation and Concurrency
- NPC visibility should be scoped to the viewer(s) of a given cinematic only.
- Multiple players watching same cinematic must not break each other’s state.
- Multiple simultaneous scenes/cinematics should not cross-leak entities, state, or playback control.

### 6) GUI/UX Reliability
- Main command does not open the expected scene list menu.
- Menu actions often only output irrelevant success text without performing real action.
- Group page title does not update when changing pages.
- "Current tick" configured options display as "configured" only, with no editable controls.
- "Actor tick actions" click only prints "Actor tick action updated" but performs nothing meaningful.
- Overall menu is currently unreliable and confusing.

### 7) Inventory Lock Side Effect
- User inventory cannot be used due to a persistent blocking state.
- This lock appears to remain active unexpectedly and must be fixed.

### 8) Global Commands Behavior
- `allow global commands` option is unclear and appears to block all commands when disabled.
- Behavior and naming need correction so expected command execution is consistent.

---

## Implementation Strategy (Do This in Separate Tasks)

### Phase 0 — Baseline + Instrumentation
1. Add debug toggles and structured logs for:
   - scene session lifecycle
   - actor spawn/despawn
   - preview entity lifecycle
   - camera mode transitions
   - GUI click dispatch/action handlers
2. Reproduce each reported bug with a deterministic script/checklist.
3. Create regression checklist before changing behavior.

**Exit criteria:** Every reported issue has clear reproduction steps and at least one log signature.

### Phase 1 — Camera System Stabilization
1. Implement authoritative camera controller state machine:
   - enter cinematic
   - lock control
   - transition between keyframes smoothly
   - exit and fully restore player state
2. Enforce input lock using reliable spectator target approach.
3. Replace ineffective pumpkin-speed trick with a reliable visible zoom approach (slowness or supported equivalent).
4. Eliminate teleport-only updates unless explicitly in "hard cut" mode.
5. Remove "moved too quickly" conditions by clamping movement deltas/intervals.

**Exit criteria:** Smooth motion in test scene, no camera drift, no moved-too-quickly spam, visible zoom effect present.

### Phase 2 — Recording UX + Timing Engine
1. Add recording presets:
   - duration in seconds
   - duration in ticks
2. Add pre-roll countdown titles (e.g., 3..2..1..REC).
3. Start recording only after countdown completes.
4. Auto-stop when duration reached.
5. Add action-bar counter during recording (`elapsed/max`).
6. Add GUI and command parity for all recording parameters.
7. Add a quick stop action (hotbar item or GUI button) that does not require chat command typing.

**Exit criteria:** Recording start/stop timing is deterministic and user-friendly.

### Phase 3 — Actor Entity Lifecycle + Multi-Actor Playback
1. Refactor actor runtime handles so each actor has unique, tracked lifecycle per scene session.
2. Ensure all temporary/preview actors are despawned on:
   - recording stop
   - scene close
   - menu close
   - player disconnect
   - plugin disable
3. Fix multi-actor playback so all actors render and animate simultaneously as configured.
4. Prevent duplicate spawn race conditions when switching actors during recording.

**Exit criteria:** No ghost NPCs, no duplicate leftovers, both first and second actor play correctly.

### Phase 4 — Actor Properties Fidelity
1. Validate and fix scale propagation to runtime NPC/model entity.
2. Ensure hidden nameplate setting is enforced at spawn and after updates.
3. Add runtime re-application hooks when actor definition changes.

**Exit criteria:** Scale and nametag behavior always match editor settings.

### Phase 5 — GUI Correctness Overhaul
1. Audit each GUI button for real action mapping.
2. Remove fake "success" feedback where no state change occurs.
3. Fix main command to open correct scenes overview GUI.
4. Fix group page title updates.
5. Make "configured" options actually editable via click flow.
6. Ensure actor tick actions perform real modifications and persist.
7. Add consistency rules: all GUI actions must provide specific confirmation and error messages.

**Exit criteria:** No dead controls; all controls are functional and verifiable.

### Phase 6 — Per-Player Visibility Isolation
1. Move to per-session viewer scoping for spawned entities/NPC packets.
2. Guarantee only active viewers can see cinematic actors.
3. Support multi-viewer same scene without shared mutable state corruption.
4. Add teardown verification to avoid leaking visibility.

**Exit criteria:** Two or more viewers can run the same cinematic without interference.

### Phase 7 — Inventory Lock + Command Policy Cleanup
1. Find and fix persistent inventory lock source; restore inventory behavior reliably on session end.
2. Clarify semantics of `allow global commands`:
   - rename if needed
   - enforce predictable behavior
   - document exact effect

**Exit criteria:** Inventory is usable outside intended lock windows; command policy works exactly as labeled.

---

## Required Engineering Practices During Implementation

1. **One phase per branch/PR** (small focused changes).
2. Add automated checks where feasible (unit tests/integration hooks for pure logic).
3. Add manual QA scripts for in-game validation (repro + expected behavior).
4. Do not merge a phase without passing its exit criteria.
5. Preserve backward compatibility of saved scene data unless migration is explicitly added.

---

## Acceptance Test Matrix (Must Pass Before Declaring "Done")

1. **Single actor recording:** countdown, timed stop, action bar timer, correct keyframe capture.
2. **Two actors recording:** no duplicate ghosts, switching actors cleanly, both animate on play.
3. **Preview lifecycle:** preview appears when needed and always despawns after context ends.
4. **Camera cinematic:** smooth interpolation, locked view, no jitter, no moved-too-quickly spam.
5. **Zoom effect:** visible and consistent effect while camera lock mode active.
6. **Scale + nametag:** scaled actor appears correctly; nametag hidden when configured.
7. **GUI navigation:** main menu, groups pages, tick action editors, and all click paths functional.
8. **Per-player isolation:** two simultaneous viewers see only their intended cinematic entities.
9. **Inventory recovery:** inventory interactions restored correctly after editor/cinematic workflows.
10. **Command options:** global command toggle behaves exactly as described.

---

## Suggested Immediate Next Task (Task #1)
Start with **Phase 1 (Camera System Stabilization)** only.

Deliverables for Task #1:
- camera lock and smooth motion fixed
- no moved-too-quickly spam
- visible zoom effect implemented with reliable method
- clean enter/exit restoration of player state
- short QA report with before/after behavior

Once Task #1 is fully validated, continue sequentially with Phase 2.
