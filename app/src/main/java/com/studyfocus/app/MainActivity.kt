package com.studyfocus.app

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private val ticker = object : Runnable {
        override fun run() {
            refresh()
            handler.postDelayed(this, 1000)
        }
    }

    private lateinit var setupGroup: View
    private lateinit var lockedGroup: View
    private lateinit var tvAccessStatus: TextView
    private lateinit var btnAccess: MaterialButton
    private lateinit var tvDuration: TextView
    private lateinit var seekDuration: SeekBar
    private lateinit var tvAppsCount: TextView
    private lateinit var btnChooseApps: MaterialButton
    private lateinit var tvAdminStatus: TextView
    private lateinit var btnAdmin: MaterialButton
    private lateinit var btnStart: MaterialButton
    private lateinit var tvCountdown: TextView
    private lateinit var tvLockedApps: TextView

    private val adminComponent by lazy { ComponentName(this, FocusAdminReceiver::class.java) }
    private val dpm by lazy { getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupGroup = findViewById(R.id.setupGroup)
        lockedGroup = findViewById(R.id.lockedGroup)
        tvAccessStatus = findViewById(R.id.tvAccessStatus)
        btnAccess = findViewById(R.id.btnAccess)
        tvDuration = findViewById(R.id.tvDuration)
        seekDuration = findViewById(R.id.seekDuration)
        tvAppsCount = findViewById(R.id.tvAppsCount)
        btnChooseApps = findViewById(R.id.btnChooseApps)
        tvAdminStatus = findViewById(R.id.tvAdminStatus)
        btnAdmin = findViewById(R.id.btnAdmin)
        btnStart = findViewById(R.id.btnStart)
        tvCountdown = findViewById(R.id.tvCountdown)
        tvLockedApps = findViewById(R.id.tvLockedApps)

        // First launch: tick common distracting apps that are installed.
        val installed = AppUtils.launchableApps(this).map { it.activityInfo.packageName }.toSet()
        FocusPrefs.initDefaultsIfNeeded(this, installed)

        // 5 to 240 minutes in steps of 5.
        seekDuration.max = 47
        seekDuration.progress = 8 // 45 minutes
        updateDurationLabel()
        seekDuration.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updateDurationLabel()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnAccess.setOnClickListener { showAccessibilityDisclosure() }

        btnChooseApps.setOnClickListener {
            if (FocusPrefs.isActive(this)) {
                Toast.makeText(this, "Locked during focus mode", Toast.LENGTH_SHORT).show()
            } else {
                startActivity(Intent(this, AppPickerActivity::class.java))
            }
        }

        btnAdmin.setOnClickListener { toggleAdmin() }
        btnStart.setOnClickListener { onStartClicked() }
    }

    override fun onResume() {
        super.onResume()
        handler.post(ticker)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(ticker)
    }

    // ------------------------------------------------------------------ UI

    private fun minutes(): Int = (seekDuration.progress + 1) * 5

    private fun updateDurationLabel() {
        tvDuration.text = TimeFormat.minutesLabel(minutes())
    }

    private fun refresh() {
        val remaining = FocusPrefs.remainingMillis(this)
        val blockedCount = FocusPrefs.getBlocked(this).size

        if (remaining > 0L) {
            setupGroup.visibility = View.GONE
            lockedGroup.visibility = View.VISIBLE
            tvCountdown.text = TimeFormat.clock(remaining)
            tvLockedApps.text = if (blockedCount == 1) "1 app blocked" else "$blockedCount apps blocked"
            return
        }

        setupGroup.visibility = View.VISIBLE
        lockedGroup.visibility = View.GONE

        val accessOn = isAccessibilityEnabled()
        tvAccessStatus.text = if (accessOn) "Blocking permission: ON" else "Blocking permission: OFF"
        btnAccess.visibility = if (accessOn) View.GONE else View.VISIBLE

        tvAppsCount.text = when (blockedCount) {
            0 -> "No apps selected"
            1 -> "1 app will be blocked"
            else -> "$blockedCount apps will be blocked"
        }

        val adminOn = dpm.isAdminActive(adminComponent)
        tvAdminStatus.text = if (adminOn) "Extra protection: ON" else "Extra protection: OFF"
        btnAdmin.text = if (adminOn) "Turn off extra protection" else "Turn on extra protection"
    }

    // ------------------------------------------------------------- actions

    private fun onStartClicked() {
        if (!isAccessibilityEnabled()) {
            showAccessibilityDisclosure()
            return
        }
        if (FocusPrefs.getBlocked(this).isEmpty()) {
            Toast.makeText(this, "Choose at least one app to block", Toast.LENGTH_SHORT).show()
            return
        }

        val mins = minutes()
        AlertDialog.Builder(this)
            .setTitle("Start focus mode?")
            .setMessage(
                "For the next ${TimeFormat.minutesLabel(mins)} your chosen apps will be blocked.\n\n" +
                    "You will NOT be able to cancel, change the app list or open Settings " +
                    "until the timer ends. Phone calls still work."
            )
            .setPositiveButton("Start") { _, _ ->
                FocusPrefs.start(this, mins)
                refresh()
            }
            .setNegativeButton("Not now", null)
            .show()
    }

    /** Prominent disclosure shown before sending the user to Accessibility settings. */
    private fun showAccessibilityDisclosure() {
        AlertDialog.Builder(this)
            .setTitle("Allow app blocking")
            .setMessage(
                "Study Focus uses Android's Accessibility Service only to detect which app " +
                    "is opened, so it can block distracting apps during a focus session.\n\n" +
                    "It does not read your screen, messages or passwords, and nothing leaves your phone.\n\n" +
                    "On the next screen, find \"Study Focus\" and turn it on."
            )
            .setPositiveButton("Agree and continue") { _, _ ->
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun toggleAdmin() {
        if (dpm.isAdminActive(adminComponent)) {
            if (FocusPrefs.isActive(this)) {
                Toast.makeText(this, "Locked during focus mode", Toast.LENGTH_SHORT).show()
            } else {
                dpm.removeActiveAdmin(adminComponent)
                refresh()
            }
        } else {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                .putExtra(
                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    "Stops Study Focus from being uninstalled while a focus session is running."
                )
            startActivity(intent)
        }
    }

    private fun isAccessibilityEnabled(): Boolean {
        val component = ComponentName(this, BlockerAccessibilityService::class.java)
        val enabled = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabled.split(':').any {
            it.equals(component.flattenToString(), ignoreCase = true) ||
                it.equals(component.flattenToShortString(), ignoreCase = true)
        }
    }
}
