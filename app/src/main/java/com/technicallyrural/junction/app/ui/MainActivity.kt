package com.technicallyrural.junction.app.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.android.messaging.ui.conversationlist.ConversationListActivity

/**
 * Main entry point for the messaging application.
 *
 * This activity serves as a trampoline to the upstream AOSP Messaging UI.
 * It redirects to ConversationListActivity which shows the list of conversations.
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Launch the upstream conversation list activity
        val intent = Intent(this, ConversationListActivity::class.java)

        // Forward any extras from the launching intent
        this.intent?.extras?.let { extras ->
            intent.putExtras(extras)
        }

        startActivity(intent)
        finish() // Close this trampoline activity
    }
}
