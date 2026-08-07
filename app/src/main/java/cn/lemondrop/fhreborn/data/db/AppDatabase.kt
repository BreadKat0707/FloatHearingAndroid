package cn.lemondrop.fhreborn.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import cn.lemondrop.fhreborn.data.db.dao.HiddenFolderDao
import cn.lemondrop.fhreborn.data.db.dao.PlaylistDao
import cn.lemondrop.fhreborn.data.db.dao.PlayRecordDao
import cn.lemondrop.fhreborn.data.db.dao.PlaybackStateDao
import cn.lemondrop.fhreborn.data.db.dao.ScanDirectoryDao
import cn.lemondrop.fhreborn.data.db.dao.SongDao
import cn.lemondrop.fhreborn.data.db.entity.HiddenFolder
import cn.lemondrop.fhreborn.data.db.entity.Playlist
import cn.lemondrop.fhreborn.data.db.entity.PlaylistSong
import cn.lemondrop.fhreborn.data.db.entity.PlayRecord
import cn.lemondrop.fhreborn.data.db.entity.PlaybackState
import cn.lemondrop.fhreborn.data.db.entity.ScanDirectory
import cn.lemondrop.fhreborn.data.db.entity.Song

@Database(
    entities = [Song::class, HiddenFolder::class, ScanDirectory::class, PlaybackState::class, PlayRecord::class, Playlist::class, PlaylistSong::class],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun hiddenFolderDao(): HiddenFolderDao
    abstract fun scanDirectoryDao(): ScanDirectoryDao
    abstract fun playbackStateDao(): PlaybackStateDao
    abstract fun playRecordDao(): PlayRecordDao
    abstract fun playlistDao(): PlaylistDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE play_records (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        songId INTEGER NOT NULL,
                        playDuration INTEGER NOT NULL,
                        timestamp INTEGER NOT NULL
                    )"""
                )
                db.execSQL("CREATE INDEX idx_play_records_timestamp ON play_records(timestamp)")
                db.execSQL("CREATE INDEX idx_play_records_songId ON play_records(songId)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE songs ADD COLUMN year INTEGER")
                db.execSQL("ALTER TABLE songs ADD COLUMN discNumber INTEGER")
                db.execSQL("ALTER TABLE songs ADD COLUMN trackNumber INTEGER")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS playlists (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        description TEXT,
                        coverPath TEXT,
                        coverSource INTEGER NOT NULL DEFAULT 0,
                        defaultPlayMode INTEGER NOT NULL DEFAULT 0,
                        tags TEXT,
                        sortType INTEGER NOT NULL DEFAULT 0,
                        isSystem INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )"""
                )
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS playlist_songs (
                        playlistId INTEGER NOT NULL,
                        songId INTEGER NOT NULL,
                        sortOrder INTEGER NOT NULL,
                        addedAt INTEGER NOT NULL,
                        PRIMARY KEY(playlistId, songId)
                    )"""
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_playlist_songs_playlistId ON playlist_songs(playlistId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_playlist_songs_songId ON playlist_songs(songId)")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE playlists ADD COLUMN playCount INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fh_reborn_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
