package com.example.stockkeeper

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.stockkeeper.ui.history.HistoryFragment
import com.example.stockkeeper.ui.archive.ArchiveFragment
import com.example.stockkeeper.ui.directories.DirectoriesFragment
import com.example.stockkeeper.ui.product.ProductDetailsFragment
import com.example.stockkeeper.ui.settings.SettingsFragment
import com.example.stockkeeper.ui.warehouse.WarehouseFragment
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.navigation.NavigationBarView

class MainActivity : AppCompatActivity() {
    private lateinit var toolbar: MaterialToolbar
    private lateinit var bottomNavigation: NavigationBarView

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
        bottomNavigation = findViewById(R.id.bottomNavigation)
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_warehouse -> show(WarehouseFragment(), R.string.warehouse)
                R.id.navigation_history -> show(HistoryFragment(), R.string.history)
                R.id.navigation_directories -> show(DirectoriesFragment(), R.string.directories)
                R.id.navigation_settings -> show(SettingsFragment(), R.string.settings)
                else -> false
            }
        }
        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount == 0) {
                toolbar.navigationIcon = null
                toolbar.setNavigationOnClickListener(null)
                toolbar.setTitle(R.string.warehouse)
                bottomNavigation.visibility = android.view.View.VISIBLE
            }
        }

        if (savedInstanceState == null) {
            bottomNavigation.selectedItemId = R.id.navigation_warehouse
        }
    }

    private fun show(fragment: Fragment, title: Int): Boolean {
        bottomNavigation.visibility = android.view.View.VISIBLE
        toolbar.navigationIcon = null
        toolbar.setNavigationOnClickListener(null)
        toolbar.setTitle(title)
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.fragmentContainer, fragment)
            .commit()
        return true
    }

    fun openProductDetails(productId: Long) {
        toolbar.setTitle(R.string.product_details)
        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
        toolbar.setNavigationOnClickListener { supportFragmentManager.popBackStack() }
        bottomNavigation.visibility = android.view.View.GONE
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, ProductDetailsFragment.newInstance(productId))
            .addToBackStack("product_details")
            .commit()
    }

    fun openArchive() {
        toolbar.setTitle(R.string.archive_title)
        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
        toolbar.setNavigationOnClickListener { supportFragmentManager.popBackStack() }
        bottomNavigation.visibility = android.view.View.GONE
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, ArchiveFragment())
            .addToBackStack("archive")
            .commit()
    }
}
