# Emulator smoke test, 2026-11-15

Paired Wear OS emulator (`BreatheWear`, API 36) and phone emulator (`BreathePhone`, API 36), debug builds from `main` at c32fb14.

| Step | Result | Evidence |
|---|---|---|
| Notification permission on first start | Prompt shown; allow leads to the heart-rate prompt | `02-notification-permission.png`, `03-after-allow.png` |
| Breathing session with live heart rate | Runs, foreground Ongoing Activity service active | `04-breathing-session.png`, `04b-breathing-session.png` |
| Process death mid-session | Process killed with `kill -9` after ~80 s; on relaunch the session is recorded (1 min today) | `05-session-before-kill.png`, `06-after-process-death-relaunch.png` |
| Finish a breathing session on the watch | Saved, today shows 2 / 10 | `07-watch-session-finished.png` |
| Session appears on the phone | Recent sessions, streak and weekly chart updated | `08-phone-after-watch-session.png` |
| Focus session on the watch, stopped after ~70 s | Saved and synced; appears on the phone as "Focus" | `11-focus-running.png`, `12-focus-stopped.png`, `13-phone-focus-session.png` |
| Tile refresh | Tile shows 3 / 10, matching the three saved minutes | `14-watch-tile.png` |

Not covered: a phone-to-watch pattern change, and a focus block completing on its own (a block is 25 min).
