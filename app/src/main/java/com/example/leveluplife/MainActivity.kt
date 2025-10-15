package com.example.leveluplife

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import com.example.leveluplife.ui.DashboardFragment
import com.example.leveluplife.ui.AddQuestFragment

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Transparent status bar so header gradient දාගෙන යනවා
        window.statusBarColor = android.graphics.Color.TRANSPARENT

        // Icons WHITE
        ViewCompat.getWindowInsetsController(window.decorView)
            ?.isAppearanceLightStatusBars = false

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.main_fragment_container, DashboardFragment())
                .commit()
        }
    }

    fun navigateTo(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.main_fragment_container, fragment)
            .addToBackStack(null) // ගමන මතක තියාගන්න කියනවා
            .commit()
    }
}