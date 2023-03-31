package com.su.criminalintent.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.su.criminalintent.Crime
import java.util.*

@Dao
interface CrimeDao {

    @Query("SELECT * FROM crime")
    fun getCrimes(): LiveData<List<Crime>>

    @Query("SELECT * FROM crime WHERE id=(:id)")
    fun getCrime(id: UUID): LiveData<Crime?>

    @Update
    fun updateCrime(crime: Crime)

    @Insert
    fun insertCrime(crime: Crime)

    @Delete
    fun deleteCrime(crime: Crime)
}