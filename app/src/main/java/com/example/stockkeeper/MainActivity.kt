package com.example.stockkeeper

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.stockkeeper.ui.history.HistoryFragment
import com.example.stockkeeper.ui.settings.SettingsFragment
import com.example.stockkeeper.ui.warehouse.WarehouseFragment
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    private lateinit var toolbar: MaterialToolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        toolbar = findViewById(R.id.topAppBar)
        val navigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        navigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_warehouse -> show(WarehouseFragment(), R.string.warehouse)
                R.id.navigation_history -> show(HistoryFragment(), R.string.history)
                R.id.navigation_settings -> show(SettingsFragment(), R.string.settings)
                else -> false
            }
        }

        if (savedInstanceState == null) {
            navigation.selectedItemId = R.id.navigation_warehouse
        }
    }

    private fun show(fragment: Fragment, title: Int): Boolean {
        toolbar.setTitle(title)
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.fragmentContainer, fragment)
            .commit()
        return true
    }
}
