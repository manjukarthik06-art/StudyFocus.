package com.studyfocus.app

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

/** Full-screen "stay focused" screen shown when a blocked app is opened. */
class BlockedActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PACKAGE = "extra_package"
    }

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var tvMessage: TextView
    private lateinit var tvTime: TextView

    private val ticker = object : Runnable {
        override fun run() {
            updateTime()
            if (!isFinishing) handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blocked)

        tvMessage = findViewById(R.id.tvMessage)
        tvTime = findViewById(R.id.tvTime)
        findViewById<MaterialButton>(R.id.btnHome).setOnClickListener { goHome() }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                goHome()
            }
        })

        showBlockedApp(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        showBlockedApp(intent)
    }

    override fun onResume() {
        super.onResume()
        handler.post(ticker)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(ticker)
    }

    private fun showBlockedApp(intent: Intent?) {
        val pkg = intent?.getStringExtra(EXTRA_PACKAGE)
        val label = if (pkg == null) {
            "This app"
        } else {
            try {
                @Suppress("DEPRECATION")
                packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
            } catch (e: PackageManager.NameNotFoundException) {
                pkg
            }
        }
        tvMessage.text = "$label is blocked during focus mode"
        updateTime()
    }

    private fun updateTime() {
        val remaining = FocusPrefs.remainingMillis(this)
        if (remaining <= 0L) {
            finish()
            return
        }
        tvTime.text = TimeFormat.clock(remaining)
    }

    private fun goHome() {
        val home = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_HOME)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(home)
        finish()
    }
}
