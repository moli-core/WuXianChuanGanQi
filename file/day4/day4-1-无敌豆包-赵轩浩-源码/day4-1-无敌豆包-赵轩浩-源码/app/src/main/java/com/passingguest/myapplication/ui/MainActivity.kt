package com.passingguest.myapplication.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.passingguest.myapplication.R
import com.passingguest.myapplication.ui.budget.BudgetFragment
import com.passingguest.myapplication.ui.record.RecordListFragment
import com.passingguest.myapplication.ui.statistics.StatisticsFragment

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView

    private val fragments = mutableMapOf<Int, Fragment>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNavigation = findViewById(R.id.bottomNavigation)

        fragments[R.id.nav_records] = RecordListFragment()
        fragments[R.id.nav_statistics] = StatisticsFragment()
        fragments[R.id.nav_budget] = BudgetFragment()

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_records -> {
                    switchFragment(R.id.nav_records)
                    true
                }
                R.id.nav_statistics -> {
                    switchFragment(R.id.nav_statistics)
                    true
                }
                R.id.nav_budget -> {
                    switchFragment(R.id.nav_budget)
                    true
                }
                else -> false
            }
        }

        // Set default selection
        bottomNavigation.selectedItemId = R.id.nav_records
    }

    private fun switchFragment(fragmentId: Int): Fragment? {
        val fragment = fragments[fragmentId] ?: return null
        if (fragment.isAdded) return fragment

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()

        return fragment
    }
}
