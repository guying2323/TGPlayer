package com.tgplayer.android.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Icon set generated from react-icons 5.7.0 (Feather MIT, Lucide ISC).
 * Source data: design/icons.json — do not hand-edit this file.
 */
object AppIcons {
    /** react-icons: fi/FiHome */
    val Home: ImageVector by lazy { icon("Home", "M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z M9,22 L9,12 L15,12 L15,22") }

    /** react-icons: fi/FiMessageSquare */
    val Chat: ImageVector by lazy { icon("Chat", "M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z") }

    /** react-icons: fi/FiSettings */
    val Settings: ImageVector by lazy { icon("Settings", "M9,12 a3,3 0 1,0 6,0 a3,3 0 1,0 -6,0 M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z") }

    /** react-icons: fi/FiSearch */
    val Search: ImageVector by lazy { icon("Search", "M3,11 a8,8 0 1,0 16,0 a8,8 0 1,0 -16,0 M21,21 L16.65,16.65") }

    /** react-icons: fi/FiPlay */
    val Play: ImageVector by lazy { icon("Play", "M5,3 L19,12 L5,21 L5,3 Z") }

    /** react-icons: fi/FiPause */
    val Pause: ImageVector by lazy { icon("Pause", "M6,4 h4 v16 h-4 Z M14,4 h4 v16 h-4 Z") }

    /** react-icons: fi/FiChevronLeft */
    val Back: ImageVector by lazy { icon("Back", "M15,18 L9,12 L15,6") }

    /** react-icons: fi/FiCast */
    val Cast: ImageVector by lazy { icon("Cast", "M2 16.1A5 5 0 0 1 5.9 20M2 12.05A9 9 0 0 1 9.95 20M2 8V6a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-6 M2,20 L2.01,20") }

    /** react-icons: fi/FiLock */
    val Lock: ImageVector by lazy { icon("Lock", "M3,11 h18 v11 h-18 Z M7 11V7a5 5 0 0 1 10 0v4") }

    /** react-icons: fi/FiSkipForward */
    val Next: ImageVector by lazy { icon("Next", "M5,4 L15,12 L5,20 L5,4 Z M19,5 L19,19") }

    /** react-icons: fi/FiVolume2 */
    val Volume: ImageVector by lazy { icon("Volume", "M11,5 L6,9 L2,9 L2,15 L6,15 L11,19 L11,5 Z M19.07 4.93a10 10 0 0 1 0 14.14M15.54 8.46a5 5 0 0 1 0 7.07") }

    /** react-icons: fi/FiWifiOff */
    val Offline: ImageVector by lazy { icon("Offline", "M1,1 L23,23 M16.72 11.06A10.94 10.94 0 0 1 19 12.55 M5 12.55a10.94 10.94 0 0 1 5.17-2.39 M10.71 5.05A16 16 0 0 1 22.58 9 M1.42 9a15.91 15.91 0 0 1 4.7-2.88 M8.53 16.11a6 6 0 0 1 6.95 0 M12,20 L12.01,20") }

    /** react-icons: fi/FiDownload */
    val Download: ImageVector by lazy { icon("Download", "M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4 M7,10 L12,15 L17,10 M12,15 L12,3") }

    /** react-icons: fi/FiMoreVertical */
    val More: ImageVector by lazy { icon("More", "M11,12 a1,1 0 1,0 2,0 a1,1 0 1,0 -2,0 M11,5 a1,1 0 1,0 2,0 a1,1 0 1,0 -2,0 M11,19 a1,1 0 1,0 2,0 a1,1 0 1,0 -2,0") }

    /** react-icons: fi/FiRefreshCw */
    val Refresh: ImageVector by lazy { icon("Refresh", "M23,4 L23,10 L17,10 M1,20 L1,14 L7,14 M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15") }

    /** react-icons: fi/FiChevronDown */
    val Down: ImageVector by lazy { icon("Down", "M6,9 L12,15 L18,9") }

    /** react-icons: fi/FiArchive */
    val Archive: ImageVector by lazy { icon("Archive", "M21,8 L21,21 L3,21 L3,8 M1,3 h22 v5 h-22 Z M10,12 L14,12") }

    /** react-icons: fi/FiX */
    val Close: ImageVector by lazy { icon("Close", "M18,6 L6,18 M6,6 L18,18") }

    /** react-icons: fi/FiMaximize */
    val Fullscreen: ImageVector by lazy { icon("Fullscreen", "M8 3H5a2 2 0 0 0-2 2v3m18 0V5a2 2 0 0 0-2-2h-3m0 18h3a2 2 0 0 0 2-2v-3M3 16v3a2 2 0 0 0 2 2h3") }

    /** react-icons: fi/FiClock */
    val Clock: ImageVector by lazy { icon("Clock", "M2,12 a10,10 0 1,0 20,0 a10,10 0 1,0 -20,0 M12,6 L12,12 L16,14") }

    /** react-icons: fi/FiUser */
    val User: ImageVector by lazy { icon("User", "M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2 M8,7 a4,4 0 1,0 8,0 a4,4 0 1,0 -8,0") }

    /** react-icons: fi/FiFolder */
    val Folder: ImageVector by lazy { icon("Folder", "M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z") }

    /** react-icons: fi/FiSliders */
    val Sliders: ImageVector by lazy { icon("Sliders", "M4,21 L4,14 M4,10 L4,3 M12,21 L12,12 M12,8 L12,3 M20,21 L20,16 M20,12 L20,3 M1,14 L7,14 M9,8 L15,8 M17,16 L23,16") }

    /** react-icons: fi/FiStar */
    val Star: ImageVector by lazy { icon("Star", "M12,2 L15.09,8.26 L22,9.27 L17,14.14 L18.18,21.02 L12,17.77 L5.82,21.02 L7,14.14 L2,9.27 L8.91,8.26 L12,2 Z") }

    /** react-icons: fi/FiCheck */
    val Check: ImageVector by lazy { icon("Check", "M20,6 L9,17 L4,12") }

    /** react-icons: fi/FiMinus */
    val Minus: ImageVector by lazy { icon("Minus", "M5,12 L19,12") }

    /** react-icons: fi/FiChevronRight */
    val Forward: ImageVector by lazy { icon("Forward", "M9,18 L15,12 L9,6") }

    /** react-icons: fi/FiInbox */
    val Inbox: ImageVector by lazy { icon("Inbox", "M22,12 L16,12 L14,15 L10,15 L8,12 L2,12 M5.45 5.11L2 12v6a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-6l-3.45-6.89A2 2 0 0 0 16.76 4H7.24a2 2 0 0 0-1.79 1.11z") }

    /** react-icons: fi/FiGlobe */
    val Globe: ImageVector by lazy { icon("Globe", "M2,12 a10,10 0 1,0 20,0 a10,10 0 1,0 -20,0 M2,12 L22,12 M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z") }

    /** react-icons: fi/FiLogOut */
    val LogOut: ImageVector by lazy { icon("LogOut", "M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4 M16,17 L21,12 L16,7 M21,12 L9,12") }

    /** react-icons: fi/FiTv */
    val Tv: ImageVector by lazy { icon("Tv", "M2,7 h20 v15 h-20 Z M17,2 L12,7 L7,2") }

    /** react-icons: fi/FiPlus */
    val Plus: ImageVector by lazy { icon("Plus", "M12,5 L12,19 M5,12 L19,12") }

    /** react-icons: lu/LuCaptions */
    val Captions: ImageVector by lazy { icon("Captions", "M3,5 h18 v14 h-18 Z M7 15h4M15 15h2M7 11h2M13 11h4") }

    /** react-icons: lu/LuGauge */
    val Gauge: ImageVector by lazy { icon("Gauge", "m12 14 4-4 M3.34 19a10 10 0 1 1 17.32 0") }

    /** react-icons: lu/LuListVideo */
    val ListVideo: ImageVector by lazy { icon("ListVideo", "M12 12H3 M16 6H3 M12 18H3 m16 12 5 3-5 3v-6Z") }

    /** react-icons: lu/LuMonitorPlay */
    val ScreenPlay: ImageVector by lazy { icon("ScreenPlay", "M10 7.75a.75.75 0 0 1 1.142-.638l3.664 2.249a.75.75 0 0 1 0 1.278l-3.664 2.25a.75.75 0 0 1-1.142-.64z M12 17v4 M8 21h8 M2,3 h20 v14 h-20 Z") }

    /** react-icons: lu/LuLanguages */
    val Language: ImageVector by lazy { icon("Language", "m5 8 6 6 m4 14 6-6 2-3 M2 5h12 M7 2h1 m22 22-5-10-5 10 M14 18h6") }

    /** react-icons: lu/LuSettings2 */
    val Tune: ImageVector by lazy { icon("Tune", "M20 7h-9 M14 17H5 M14,17 a3,3 0 1,0 6,0 a3,3 0 1,0 -6,0 M4,7 a3,3 0 1,0 6,0 a3,3 0 1,0 -6,0") }

    /** react-icons: lu/LuCast */
    val CastSecondary: ImageVector by lazy { icon("CastSecondary", "M2 8V6a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-6 M2 12a9 9 0 0 1 8 8 M2 16a5 5 0 0 1 4 4 M2,20 L2.01,20") }

    /** react-icons: lu/LuPictureInPicture2 */
    val Pip: ImageVector by lazy { icon("Pip", "M21 9V6a2 2 0 0 0-2-2H4a2 2 0 0 0-2 2v10c0 1.1.9 2 2 2h4 M12,13 h10 v7 h-10 Z") }
}

private fun icon(name: String, pathData: String): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        addPath(
            pathData = addPathNodes(pathData),
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
    }.build()
