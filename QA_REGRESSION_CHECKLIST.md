# ExtraScenes Regression Checklist

## Phase 0 — Baseline instrumentation
- Enable camera debug: `/scene debugcamera on`
- Enable preview debug: `/scene debugpreview on`
- Enable actor debug: `/scene debugactors on`
- Run self test: `/scene selftest <scene>`

Expected log signatures:
- `[scene-session] start` / `[scene-session] finish`
- `[debugcamera]` + `[debugcamera-rigstep]`
- `[debugpreview]`
- `[debugactors]`
- `[camera-clamp]` (only on large deltas)
- `[scene-command] blocked global command` (when policy blocks `global:` prefixed command)

## Manual acceptance matrix
1. Single actor recording:
   - start actor recording from GUI
   - verify 3..2..1..REC countdown
   - verify action bar timer advances and auto-stop on duration
2. Two actors recording:
   - switch actor while previewing, ensure only one runtime actor per actorId
3. Preview lifecycle:
   - close editor and verify preview entities are despawned
4. Camera cinematic:
   - run `/scene play <name>` and verify no camera drift and no teleport spam
5. Zoom effect:
   - verify slowness effect during scene and cleared on stop
6. Scale + nametag:
   - set custom scale and hidden nameplate; verify both during playback
7. GUI navigation:
   - validate scene main menu, group pages and actor tick actions
8. Per-player isolation:
   - two viewers play same scene; verify each sees only session entities
9. Inventory recovery:
   - disconnect while scene is playing, reconnect, verify helmet/inventory restored
10. Command policy:
   - with scene setting disabled, `global:` command keyframes are blocked
   - with setting enabled, same command keyframes execute
