package com.studyfocus.app

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import java.util.Locale
import kotlin.math.min

/**
 * Stores the focus session and the list of blocked apps.
 *
 * The session end time is saved on disk, so closing the app, force-stopping it
 * or rebooting the phone does NOT end the session.
 *
 * To resist clock tampering, the remaining time is measured with the phone's
 * monotonic clock (elapsedRealtime) while the phone has not rebooted. After a
 * reboot we fall back to the wall clock, capped at the original duration.
 */
object FocusPrefs {

    private const val FILE = "focus_prefs"
    private const val KEY_END_WALL = "end_wall"
    private const val KEY_END_ELAPSED = "end_elapsed"
    private const val KEY_BOOT = "boot_count"
    private const val KEY_DURATION = "duration_ms"
    private const val KEY_BLOCKED = "blocked_packages"
    private const val KEY_DEFAULTS_DONE = "defaults_done"

    /** Common distracting apps that are ticked by default if they are installed. */
    private val DEFAULT_APPS = setOf(
        "com.instagram.android",
        "com.google.android.youtube",
        "com.facebook.katana",
        "com.facebook.lite",
        "com.snapchat.android",
        "com.twitter.android",
        "com.reddit.frontpage",
        "com.pinterest",
        "com.netflix.mediaclient",
        "com.amazon.avod.thirdpartyclient",
        "in.startv.hotstar",
        "in.mohalla.sharechat",
        "in.mohalla.video",
        "com.zhiliaoapp.musically"
    )

    @Volatile
    private var cachedBootCount = Int.MIN_VALUE

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    private fun bootCount(context: Context): Int {
        if (cachedBootCount == Int.MIN_VALUE) {
            cachedBootCount = Settings.Global.getInt(
                context.applicationContext.contentResolver,
                Settings.Global.BOOT_COUNT,
                -1
            )
        }
        return cachedBootCount
    }

    // ---------------------------------------------------------------- session

    /** Milliseconds left in the current session, or 0 if no session is running. */
    fun remainingMillis(context: Context): Long {
        val p = prefs(context)
        val endWall = p.getLong(KEY_END_WALL, 0L)
        if (endWall == 0L) return 0L

        val duration = p.getLong(KEY_DURATION, 0L)
        val storedBoot = p.getInt(KEY_BOOT, -1)
        val sameBoot = storedBoot != -1 && storedBoot == bootCount(context)

        val remaining = if (sameBoot) {
            p.getLong(KEY_END_ELAPSED, 0L) - SystemClock.elapsedRealtime()
        } else {
            endWall - System.currentTimeMillis()
        }
        val capped = min(remaining, duration)

        if (capped <= 0L) {
            p.edit()
                .remove(KEY_END_WALL)
                .remove(KEY_END_ELAPSED)
                .remove(KEY_BOOT)
                .remove(KEY_DURATION)
                .apply()
            return 0L
        }
        return capped
    }

    fun isActive(context: Context): Boolean = remainingMillis(context) > 0L

    fun start(context: Context, minutes: Int) {
        if (isActive(context)) return
        val duration = minutes * 60_000L
        prefs(context).edit()
            .putLong(KEY_END_WALL, System.currentTimeMillis() + duration)
            .putLong(KEY_END_ELAPSED, SystemClock.elapsedRealtime() + duration)
            .putInt(KEY_BOOT, bootCount(context))
            .putLong(KEY_DURATION, duration)
            .commit()
    }

    // ----------------------------------------------------------- blocked apps

    fun getBlocked(context: Context): Set<String> =
        HashSet(prefs(context).getStringSet(KEY_BLOCKED, emptySet()) ?: emptySet())

    /** Changing the list is refused while a session is running. */
    fun setBlocked(context: Context, packageName: String, blocked: Boolean) {
        if (isActive(context)) return
        val set = HashSet(getBlocked(context))
        if (blocked) set.add(packageName) else set.remove(packageName)
        prefs(context).edit().putStringSet(KEY_BLOCKED, set).apply()
    }

    fun initDefaultsIfNeeded(context: Context, installedPackages: Set<String>) {
        val p = prefs(context)
        if (p.getBoolean(KEY_DEFAULTS_DONE, false)) return
        val defaults = DEFAULT_APPS.filter { it in installedPackages }.toSet()
        p.edit()
            .putStringSet(KEY_BLOCKED, defaults)
            .putBoolean(KEY_DEFAULTS_DONE, true)
            .apply()
    }
}

object TimeFormat {
    fun clock(millis: Long): String {
        val totalSeconds = (millis + 999) / 1000
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val s = totalSeconds % 60
        return if (h > 0) {
            String.format(Locale.US, "%d:%02d:%02d", h, m, s)
        } else {
            String.format(Locale.US, "%02d:%02d", m, s)
        }
    }

    fun minutesLabel(minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return when {
            h == 0 -> "$m min"
            m == 0 -> "$h h"
            else -> "$h h $m min"
        }
    }
}
