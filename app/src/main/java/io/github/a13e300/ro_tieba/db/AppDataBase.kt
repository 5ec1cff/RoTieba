package io.github.a13e300.ro_tieba.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [Account::class, HistoryEntry::class],
    version = 3,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3)]
)
abstract class AppDataBase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun historyDao(): HistoryDao
}
