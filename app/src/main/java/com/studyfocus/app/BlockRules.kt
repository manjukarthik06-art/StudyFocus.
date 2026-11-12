package com.studyfocus.app

import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.telecom.TelecomManager
import android.view.inputmethod.InputMethodManager

object BlockRules {

    /**
     * Blocked for the whole session, even if the user didn't tick them.
     * Without this, someone could open Settings and switch the blocker off,
     * or uninstall the app, in the middle of a session.
     */
    val ALWAYS_BLOCKED_DURING_FOCUS = setOf(
        "com.android.settings",
        "com.android.packageinstaller",
        "com.google.android.packageinstaller",
        "com.samsung.android.packageinstaller",
        "com.android.vending"
    )

    /** Phone and emergency apps that must never be blocked. */
    private val STATIC_ALLOWED = setOf(
        "com.android.systemui",
        "com.android.phone",
        "com.android.server.telecom",
        "com.android.incallui",
        "com.android.dialer",
        "com.google.android.dialer",
        "com.samsung.android.dialer",
        "com.samsung.android.incallui",
        "com.android.emergency"
    )

    /**
     * Packages that are never blocked: this app, the home launcher(s), the
     * default dialer, keyboards and core system UI.
     */
    fun allowedPackages(context: Context): Set<String> {
        val result = HashSet<String>(STATIC_ALLOWED)
        result.add(context.packageName)

        try {
            val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            context.packageManager.queryIntentActivities(homeIntent, 0)
                .forEach { result.add(it.activityInfo.packageName) }
        } catch (e: Exception) {
            // ignore, keep what we have
        }

        try {
            val telecom = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            telecom?.defaultDialerPackage?.let { result.add(it) }
        } catch (e: Exception) {
            // ignore
        }

        try {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.enabledInputMethodList?.forEach { result.add(it.packageName) }
        } catch (e: Exception) {
            // ignore
        }

        return result
    }
}

object AppUtils {
    /** All apps that have a launcher icon. */
    fun launchableApps(context: Context): List<ResolveInfo> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return context.packageManager.queryIntentActivities(intent, 0)
    }
}
