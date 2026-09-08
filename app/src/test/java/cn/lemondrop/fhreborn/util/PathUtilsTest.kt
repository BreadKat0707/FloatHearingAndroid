package cn.lemondrop.fhreborn.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PathUtilsTest {
    @Test
    fun hiddenFolderDoesNotMatchSimilarSibling() {
        assertTrue(PathUtils.isPathUnderFolder("/storage/Music", "/storage/Music"))
        assertTrue(PathUtils.isPathUnderFolder("/storage/Music/song.mp3", "/storage/Music"))
        assertFalse(PathUtils.isPathUnderFolder("/storage/Music2", "/storage/Music"))
        assertFalse(PathUtils.isPathUnderFolder("/storage/Music2/song.mp3", "/storage/Music"))
        assertFalse(PathUtils.isPathHiddenByFolders("/storage/Music2/song.mp3", listOf("/storage/Music")))
    }

    @Test
    fun directorySongIdIsStableAndNegative() {
        val first = PathUtils.stableDirectorySongId("/storage/emulated/0/Music/a.mp3")
        val second = PathUtils.stableDirectorySongId("/storage/emulated/0/Music/a.mp3")
        val other = PathUtils.stableDirectorySongId("/storage/emulated/0/Music/b.mp3")

        assertTrue(first < 0L)
        assertEquals(first, second)
        assertNotEquals(first, other)
    }
}
