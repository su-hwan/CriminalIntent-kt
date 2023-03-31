package com.su.criminalintent

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.su.criminalintent.CrimeListFragment
import java.util.*

class MainActivity : AppCompatActivity(), CrimeListFragment.Callbacks {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val currentFragment =
            supportFragmentManager.findFragmentById(R.id.fragment_container)

        if (currentFragment == null) {
            val fragment = CrimeListFragment.newInstance()
            supportFragmentManager
                .beginTransaction()
                .add(R.id.fragment_container, fragment)
                .commit()
        }
    }

    override fun onCrimeSelected(crimeId: UUID) {
        val fragment = CrimeFragment.newInstance(crimeId)
        supportFragmentManager
            .beginTransaction()
            //.add(R.id.fragment_container, fragment)
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null) //backStack를 생성해 줌. 이전 화면으로 이동
            .commit()
    }
}