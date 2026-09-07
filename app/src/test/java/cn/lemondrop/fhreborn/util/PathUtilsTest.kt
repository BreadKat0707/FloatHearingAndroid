package cn.lemondrop.fhreborn.util

import org.junit.Assert.assertFalse
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
}
