package com.technicallyrural.junction.app.ui

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/**
 * Dynamically injects MatrixStatusView into ConversationListActivity's action bar.
 *
 * Uses Application.ActivityLifecycleCallbacks to detect when ConversationListActivity
 * starts, then programmatically adds the status indicator to the toolbar.
 *
 * Why this approach:
 * - ConversationListActivity is in sms-upstream (AOSP code)
 * - Cannot modify AOSP code directly per architecture rules
 * - Dynamic injection preserves clean separation
 * - Works with any activity without code changes
 *
 * Implementation:
 * 1. Register lifecycle callback in JunctionApplication
 * 2. Detect ConversationListActivity by class name
 * 3. Find ActionBar/Toolbar in view hierarchy
 * 4. Inject MatrixStatusView at end of toolbar
 * 5. Observe MatrixStatusViewModel and update view
 */
class MatrixStatusInjector : Application.ActivityLifecycleCallbacks {

    companion object {
        private const val TAG = "MatrixStatusInjector"
        private const val TARGET_ACTIVITY = "com.android.messaging.ui.conversationlist.ConversationListActivity"
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        // Only inject into ConversationListActivity
        if (activity.javaClass.name != TARGET_ACTIVITY) {
            return
        }

        Log.d(TAG, "Injecting Matrix status indicator into conversation list")

        // Get ActionBar
        if (activity !is AppCompatActivity) {
            Log.w(TAG, "Activity is not AppCompatActivity")
            return
        }

        val actionBar = activity.supportActionBar
        if (actionBar == null) {
            Log.w(TAG, "Could not get ActionBar")
            return
        }

        // Enable custom view in action bar
        actionBar.setDisplayShowCustomEnabled(true)

        // Create status view
        val statusView = MatrixStatusView(activity)

        // Set as custom view in action bar (appears on the right)
        actionBar.customView = statusView

        // Observe status changes
        val viewModel = ViewModelProvider(activity)[MatrixStatusViewModel::class.java]

        activity.lifecycleScope.launch {
            viewModel.status.collect { status ->
                statusView.setStatus(status)
            }
        }

        Log.d(TAG, "Matrix status indicator injected successfully")
    }

    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}
