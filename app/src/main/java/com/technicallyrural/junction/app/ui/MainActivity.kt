package com.technicallyrural.junction.app.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * Main entry point for the messaging application.
 *
 * This activity serves as a trampoline to the upstream AOSP Messaging UI.
 * It uses an implicit intent action to launch the conversation list without
 * requiring a direct dependency on the sms-upstream module's classes.
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Launch the conversation list via implicit intent action
        val intent = Intent("com.technicallyrural.junction.action.CONVERSATION_LIST")

        // Forward any extras from the launching intent
        this.intent?.extras?.let { extras ->
            intent.putExtras(extras)
        }

        startActivity(intent)
        finish() // Close this trampoline activity
    }
}
