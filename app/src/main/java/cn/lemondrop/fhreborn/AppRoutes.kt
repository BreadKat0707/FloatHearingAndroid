package cn.lemondrop.fhreborn

import android.net.Uri
import kotlinx.serialization.Serializable
import top.yukonga.miuix.kmp.nav.core.NavKey

@Serializable
sealed interface AppRoute : NavKey {
    @Serializable
    data object Onboarding : AppRoute

    @Serializable
    data object Shell : AppRoute

    @Serializable
    data object Library : AppRoute

    @Serializable
    data object Playlists : AppRoute

    @Serializable
    data class PlaylistDetail(val playlistId: Long) : AppRoute

    @Serializable
    data object FolderBrowser : AppRoute

    @Serializable
    data class FolderDetail(val folderPath: String) : AppRoute

    @Serializable
    data object HiddenFolders : AppRoute

    @Serializable
    data object Ideas : AppRoute

    @Serializable
    data object Statistics : AppRoute

    @Serializable
    data object Settings : AppRoute

    @Serializable
    data class AlbumDetail(
        val albumName: String,
        val albumArtist: String?
    ) : AppRoute

    @Serializable
    data class ArtistDetail(val artistName: String) : AppRoute
}

/**
 * Legacy string routes are kept so screens and AppShell can navigate without changing their
 * current callbacks. New navigation code should construct [AppRoute] values directly.
 */
sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Library : Screen("library")
    data object Playlists : Screen("playlists")
    data object FolderBrowser : Screen("folder_browser")
    data object FolderDetail : Screen("folder_detail/{folderPath}") {
        fun createRoute(folderPath: String) = "folder_detail/${Uri.encode(folderPath)}"
    }
    data object HiddenFolders : Screen("hidden_folders") {
        fun createRoute() = "hidden_folders"
    }
    data object Ideas : Screen("ideas")
    data object Settings : Screen("settings")
    data object Statistics : Screen("statistics")
    data object Player : Screen("player")
    data object ArtistDetail : Screen("artist/{artistName}") {
        fun createRoute(artistName: String) = "artist/${Uri.encode(artistName)}"
    }
    data object AlbumDetail : Screen("album/{albumName}/{albumArtist}") {
        fun createRoute(albumName: String, albumArtist: String?): String {
            val artistPart = if (albumArtist.isNullOrBlank()) "_null_" else Uri.encode(albumArtist)
            return "album/${Uri.encode(albumName)}/$artistPart"
        }
    }
    data object PlaylistDetail : Screen("playlist/{playlistId}") {
        fun createRoute(playlistId: Long) = "playlist/$playlistId"
    }
}

fun AppRoute.toScreenRoute(): String = when (this) {
    AppRoute.Onboarding -> Screen.Onboarding.route
    AppRoute.Shell -> Screen.Library.route
    AppRoute.Library -> Screen.Library.route
    AppRoute.Playlists -> Screen.Playlists.route
    is AppRoute.PlaylistDetail -> Screen.PlaylistDetail.createRoute(playlistId)
    AppRoute.FolderBrowser -> Screen.FolderBrowser.route
    is AppRoute.FolderDetail -> Screen.FolderDetail.createRoute(folderPath)
    AppRoute.HiddenFolders -> Screen.HiddenFolders.createRoute()
    AppRoute.Ideas -> Screen.Ideas.route
    AppRoute.Statistics -> Screen.Statistics.route
    AppRoute.Settings -> Screen.Settings.route
    is AppRoute.AlbumDetail -> Screen.AlbumDetail.createRoute(albumName, albumArtist)
    is AppRoute.ArtistDetail -> Screen.ArtistDetail.createRoute(artistName)
}

fun String.toAppRouteOrNull(): AppRoute? {
    if (this == Screen.Onboarding.route) return AppRoute.Onboarding
    if (this == Screen.Library.route) return AppRoute.Library
    if (this == Screen.Playlists.route) return AppRoute.Playlists
    if (this == Screen.FolderBrowser.route) return AppRoute.FolderBrowser
    if (this == Screen.HiddenFolders.createRoute()) return AppRoute.HiddenFolders
    if (this == Screen.Ideas.route) return AppRoute.Ideas
    if (this == Screen.Statistics.route) return AppRoute.Statistics
    if (this == Screen.Settings.route) return AppRoute.Settings

    if (startsWith("playlist/")) {
        val id = removePrefix("playlist/").toLongOrNull()
        if (id != null) return AppRoute.PlaylistDetail(id)
    }

    if (startsWith("folder_detail/")) {
        val path = removePrefix("folder_detail/").decoded()
        if (path != null) return AppRoute.FolderDetail(path)
    }

    if (startsWith("artist/")) {
        val artist = removePrefix("artist/").decoded()
        if (artist != null) return AppRoute.ArtistDetail(artist)
    }

    if (startsWith("album/")) {
        val body = removePrefix("album/")
        val split = body.indexOf('/')
        if (split > 0) {
            val name = body.substring(0, split).decoded()
            val rawArtist = body.substring(split + 1)
            val artist = when {
                rawArtist == "_null_" -> null
                rawArtist.isEmpty() -> null
                else -> rawArtist.decoded()
            }
            if (name != null && (artist != null || rawArtist == "_null_" || rawArtist.isEmpty())) {
                return AppRoute.AlbumDetail(
                    albumName = name,
                    albumArtist = artist
                )
            }
        }
    }
    return null
}

private fun String.decoded(): String? = try {
    Uri.decode(this)
} catch (_: IllegalArgumentException) {
    null
}
