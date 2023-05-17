package io.github.a13e300.ro_tieba.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Account::class], version = 1)
abstract class AppDataBase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
}
