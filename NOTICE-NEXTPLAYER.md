# NextPlayer attribution

The playback core and player UI are adapted from **NextPlayer**
(https://github.com/anilbeesetti/nextplayer), commit `e1f406f59288439cdef1607522bc4e2a8116c6d0` (2026-10-21), GPL-3.0.

- `player/NextPlayerCore.kt` follows `feature/player/.../service/PlayerService.kt`:
  `NextRenderersFactory` (nextlib FFmpeg decoders), DefaultTrackSelector with
  tunneling/language parameters, movie AudioAttributes with audio focus, noisy
  handling, pause-at-end, wake mode.
- `player/PlayerScreen.kt` follows the layout and gestures of
  `MediaPlayerScreen` / `MediaPlayerControls` / `PlayerSeekbar` /
  `PlayerGestures` (top bar, centre transport, bottom seek bar with buffered
  range, lock mode, double-tap seek indicator, vertical volume/brightness).

Local playback follows NextPlayer's local/SAF pipeline: `DefaultMediaSourceFactory`
over `DefaultDataSource`, so `content://` URIs granted by the system file picker
play through the ContentResolver; the picked URI's read permission is persisted
(`takePersistableUriPermission`).

The Telegram source stays this project's own: TDLib streaming bytes through
`TdByteWindow` / `TdDataSource`. Both projects are GPL-3.0-or-later; this
notice must be kept with the source.
