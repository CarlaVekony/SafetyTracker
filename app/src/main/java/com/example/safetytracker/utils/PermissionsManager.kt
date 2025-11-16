package com.example.safetytracker.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Simple permissions helper for core app permissions.
 * Shows the system permission dialogs when the app is opened (if needed).
 */
object PermissionsManager {
	private const val CORE_PERMISSIONS_REQUEST_CODE = 1001

	// Permissions required for full functionality
	private val corePermissions = arrayOf(
		Manifest.permission.RECORD_AUDIO,
		Manifest.permission.SEND_SMS,
		Manifest.permission.ACCESS_FINE_LOCATION,
		Manifest.permission.ACCESS_COARSE_LOCATION,
	)

	/**
	 * Check if all core permissions are already granted.
	 */
	fun hasAllCorePermissions(context: Context): Boolean {
		return corePermissions.all { perm ->
			ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
		}
	}

	/**
	 * Request any missing core permissions. If everything is already granted,
	 * this does nothing.
	 *
	 * Should be called from an Activity (e.g. on first app launch).
	 */
	fun requestCorePermissionsIfNeeded(activity: Activity) {
		val missing = corePermissions.filter { perm ->
			ContextCompat.checkSelfPermission(activity, perm) != PackageManager.PERMISSION_GRANTED
		}

		if (missing.isNotEmpty()) {
			ActivityCompat.requestPermissions(
				activity,
				missing.toTypedArray(),
				CORE_PERMISSIONS_REQUEST_CODE
			)
		}
	}
}
