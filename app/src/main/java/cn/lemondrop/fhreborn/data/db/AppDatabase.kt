package cn.lemondrop.fhreborn.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import cn.lemondrop.fhreborn.data.db.dao.HiddenFolderDao
import cn.lemondrop.fhreborn.data.db.dao.PlayRecordDao
import cn.lemondrop.fhreborn.data.db.dao.PlaybackStateDao
import cn.lemondrop.fhreborn.data.db.dao.ScanDirectoryDao
import cn.lemondrop.fhreborn.data.db.dao.SongDao
import cn.lemondrop.fhreborn.data.db.entity.HiddenFolder
import cn.lemondrop.fhreborn.data.db.entity.PlayRecord
import cn.lemondrop.fhreborn.data.db.entity.PlaybackState
import cn.lemondrop.fhreborn.data.db.entity.ScanDirectory
import cn.lemondrop.fhreborn.data.db.entity.Song

@Database(
    entities = [Song::class, HiddenFolder::class, ScanDirectory::class, PlaybackState::class, PlayRecord::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun hiddenFolderDao(): HiddenFolderDao
    abstract fun scanDirectoryDao(): ScanDirectoryDao
    abstract fun playbackStateDao(): PlaybackStateDao
    abstract fun playRecordDao(): PlayRecordDao

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

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fh_reborn_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
