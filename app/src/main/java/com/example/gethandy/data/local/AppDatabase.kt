package com.example.gethandy.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.gethandy.data.local.dao.AppointmentDao
import com.example.gethandy.data.local.dao.BusinessDao
import com.example.gethandy.data.local.dao.ProfessionDao
import com.example.gethandy.data.local.dao.UserDao
import com.example.gethandy.data.local.dao.ReviewDao
import com.example.gethandy.data.model.Appointment
import com.example.gethandy.data.model.Business
import com.example.gethandy.data.model.Profession
import com.example.gethandy.data.model.User
import com.example.gethandy.data.model.Review
import com.example.gethandy.utils.Converters

@Database(
    entities = [
        User::class,
        Business::class,
        Profession::class,
        Appointment::class,
        Review:: class

    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun businessDao(): BusinessDao
    abstract fun professionDao(): ProfessionDao
    abstract fun appointmentDao(): AppointmentDao
    abstract fun reviewDao(): ReviewDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gethandy_db_fresh2"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

    }
}