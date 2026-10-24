# Findroid component attribution

Findroid (https://github.com/jarnedemeulemeester/findroid), commit `f7d090f7814c7aa6e1e19fe319fe15f4e6e01ce4` (2026-10-14), GPL-3.0.

The UI components in `app/src/main/java/com/tgplayer/android/ui/findroid/FindroidComponents.kt`
are adapted from `app/phone/src/main/java/dev/jdtech/jellyfin/presentation/film/components/`:

- **ItemCard** — 260 dp horizontal / 150 dp vertical cards, `MaterialTheme.shapes.small`,
  clickable surface, title `bodyMedium` (2 lines) + subtitle `bodySmall`.
- **ItemPoster** — 1.77f horizontal / 0.66f vertical artwork ratio, surface background.
- **ProgressBar** — 4 dp bar, `shapes.extraSmall`, primary colour, 8 dp inset.
- **HomeSection** — 42 dp header with `titleMedium`, 4 dp gap, `LazyRow` with 12 dp item spacing.
- Home layout follows Findroid's HomeScreen: content starts under a floating title bar,
  vertical list of horizontal card sections.

Adaptations: the model is this app's `MediaItem` instead of `FindroidItem`, artwork is
loaded from the Telegram thumbnail cache instead of Coil, Jellyfin-specific badges and
episode subtitles are omitted, and the section content is provided as a `LazyListScope`
lambda. Both projects are GPL-3.0-or-later; this notice must be kept with the source.
