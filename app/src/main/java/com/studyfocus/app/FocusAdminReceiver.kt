package com.studyfocus.app

import android.app.admin.DeviceAdminReceiver

/**
 * Empty device-admin receiver. While it is active, Android refuses to uninstall
 * the app until the admin is switched off in Settings (which is itself blocked
 * during a focus session).
 */
class FocusAdminReceiver : DeviceAdminReceiver()
