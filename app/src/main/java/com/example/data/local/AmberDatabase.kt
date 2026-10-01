package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.DownloadJob
import com.example.data.model.TrackEntity

@Database(
    entities = [TrackEntity::class, DownloadJob::class],
    version = 1,
    exportSchema = false
)
abstract class AmberDatabase : RoomDatabase() {

    abstract fun trackDao(): TrackDao
    abstract fun downloadJobDao(): DownloadJobDao

    companion object {
        @Volatile
        private var INSTANCE: AmberDatabase? = null

        fun getInstance(context: Context): AmberDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AmberDatabase::class.java,
                    "amber_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
