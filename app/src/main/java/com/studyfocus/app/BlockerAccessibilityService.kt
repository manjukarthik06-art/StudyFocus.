package com.studyfocus.app

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent

/**
 * Watches which app comes to the front. During a focus session, if the app is
 * on the block list, the user is sent to the home screen and a "stay focused"
 * screen is shown on top.
 *
 * It only looks at the package name of the window. It never reads screen
 * content (canRetrieveWindowContent = false in accessibility_service_config.xml).
 */
class BlockerAccessibilityService : AccessibilityService() {

    private var lastBlockAt = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) return
        if (!FocusPrefs.isActive(this)) return

        val shouldBlock = pkg in BlockRules.ALWAYS_BLOCKED_DURING_FOCUS ||
            pkg in FocusPrefs.getBlocked(this)
        if (!shouldBlock) return

        // Never block the launcher, dialer, keyboard, system UI or this app.
        if (pkg in BlockRules.allowedPackages(this)) return

        val now = SystemClock.elapsedRealtime()
        if (now - lastBlockAt < 600) return
        lastBlockAt = now

        performGlobalAction(GLOBAL_ACTION_HOME)

        val intent = Intent(this, BlockedActivity::class.java)
            .addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION
            )
            .putExtra(BlockedActivity.EXTRA_PACKAGE, pkg)
        startActivity(intent)
    }

    override fun onInterrupt() {
        // Nothing to clean up.
    }
}
