package com.su.criminalintent

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.Room
import com.su.criminalintent.database.CrimeDatabase
import com.su.criminalintent.database.migration_1_2
import java.io.File
import java.util.*
import java.util.concurrent.Executors

private const val TAG = "CrimeRepository"
private const val DATABASE_NAME = "crime-database"
class CrimeRepository private constructor(context: Context) {
    private val filesDir = context.applicationContext.filesDir

    companion object {
        private var INSTANCE: CrimeRepository? = null

        fun initialize(context: Context) {
            if (INSTANCE == null) {
                INSTANCE = CrimeRepository(context)
            }
        }

        fun get(): CrimeRepository {
            return INSTANCE ?: throw IllegalStateException("CrimeRepository must be initialized")
        }
    }

    private val database: CrimeDatabase = Room.databaseBuilder(
        context.applicationContext,
        CrimeDatabase::class.java,
        DATABASE_NAME
    ).addMigrations(migration_1_2)
        .build()
    private val crimeDao = database.crimeDao()

    private val executor = Executors.newSingleThreadExecutor()

    fun getCrimes(): LiveData<List<Crime>> = crimeDao.getCrimes()

    fun getCrime(id: UUID): LiveData<Crime?> = crimeDao.getCrime(id)

    fun getPhotoFile(crime: Crime) : File = File(filesDir, crime.photoFileName)

    fun actionCrime(crime: Crime, action: Action) {
        //Log.d(TAG, "action: ${action}, crime: ${crime}")
        executor.execute {
            when (action) {
                Action.UPDATE -> crimeDao.updateCrime(crime)
                Action.DELETE -> crimeDao.deleteCrime(crime)
                Action.INSERT -> crimeDao.insertCrime(crime)
            }
        }
    }

    enum class Action {
        UPDATE, INSERT, DELETE
    }
}