package com.technicallyrural.junction.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.util.Log

/**
 * Helper for requesting battery optimization exemption.
 *
 * Battery optimization exemption allows the MatrixSyncService to:
 * - Run continuously in the background
 * - Maintain network connections during Doze mode
 * - Receive messages with minimal delay
 *
 * This is necessary for real-time SMS ↔ Matrix bridging.
 * Without exemption, Android will kill the service during Doze mode.
 */
object BatteryOptimizationHelper {

    private const val TAG = "BatteryOptimization"

    /**
     * Check if battery optimization is currently disabled for this app.
     *
     * @return true if app is exempt from battery optimization (can run freely)
     */
    fun isOptimizationDisabled(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val packageName = context.packageName

        return powerManager.isIgnoringBatteryOptimizations(packageName).also {
            Log.d(TAG, "Battery optimization disabled: $it")
        }
    }

    /**
     * Open system settings to request battery optimization exemption.
     *
     * This opens the battery optimization settings for the app, where the user
     * can manually disable optimization.
     *
     * Note: Apps cannot programmatically disable optimization without user consent.
     * The REQUEST_IGNORE_BATTERY_OPTIMIZATIONS permission allows opening this
     * screen directly, but the user must still manually grant exemption.
     */
    fun requestExemption(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
            Log.d(TAG, "Opened battery optimization settings")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open battery optimization settings", e)

            // Fallback: Open general battery optimization settings
            try {
                val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                context.startActivity(fallbackIntent)
                Log.d(TAG, "Opened general battery optimization settings (fallback)")
            } catch (e2: Exception) {
                Log.e(TAG, "Failed to open battery settings (fallback)", e2)
            }
        }
    }

    /**
     * Get a user-friendly explanation for why exemption is needed.
     */
    fun getExemptionRationale(): String {
        return """
            Junction needs to run continuously in the background to bridge SMS and Matrix messages in real-time.

            Without battery optimization exemption:
            • Matrix messages may be delayed by hours
            • SMS → Matrix bridging may fail during Doze mode
            • The service may be killed unexpectedly

            With exemption:
            • Messages bridge instantly
            • Service runs reliably 24/7
            • Battery impact is minimized with smart idle detection

            Estimated battery usage: 3-6% per day
        """.trimIndent()
    }

    /**
     * Check if the device manufacturer has custom battery optimization settings
     * that may interfere with background services.
     *
     * Some manufacturers (Xiaomi, Huawei, Samsung, etc.) have aggressive battery
     * optimization that requires additional whitelisting.
     */
    fun hasCustomBatteryOptimization(): Boolean {
        val manufacturer = android.os.Build.MANUFACTURER.lowercase()
        return manufacturer in listOf(
            "xiaomi",
            "huawei",
            "oppo",
            "vivo",
            "oneplus",
            "samsung",
            "asus",
            "nokia"
        )
    }

    /**
     * Get manufacturer-specific instructions for battery optimization.
     */
    fun getManufacturerInstructions(): String? {
        val manufacturer = android.os.Build.MANUFACTURER.lowercase()

        return when {
            manufacturer == "xiaomi" ->
                "Go to Security > Battery > Manage apps battery usage > Select 'Junction' > No restrictions"

            manufacturer == "huawei" ->
                "Go to Settings > Battery > App launch > Junction > Manage manually > Allow all"

            manufacturer == "samsung" ->
                "Go to Settings > Apps > Junction > Battery > Optimize battery usage > All > Junction > Disable"

            manufacturer == "oppo" || manufacturer == "realme" ->
                "Go to Settings > Battery > More Battery Settings > Junction > Allow background activity"

            manufacturer == "oneplus" ->
                "Go to Settings > Battery > Battery optimization > Junction > Don't optimize"

            else -> null
        }
    }
}
