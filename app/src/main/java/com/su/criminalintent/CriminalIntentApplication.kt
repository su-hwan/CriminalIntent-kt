package com.su.criminalintent

import android.app.Application
import com.su.criminalintent.CrimeRepository

class CriminalIntentApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        CrimeRepository.initialize(this)
    }
}