package com.technicallyrural.junction.app.ui

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.technicallyrural.junction.app.R
import com.technicallyrural.junction.app.databinding.ActivityMatrixConfigBinding
import com.technicallyrural.junction.app.matrix.MatrixConfig
import com.technicallyrural.junction.app.matrix.MatrixConfigRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Activity for configuring Matrix server connection.
 *
 * Allows users to view and edit Matrix homeserver configuration,
 * test connections, and manage authentication.
 */
class MatrixConfigActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMatrixConfigBinding
    private lateinit var repository: MatrixConfigRepository
    private var currentConfig: MatrixConfig = MatrixConfig.EMPTY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMatrixConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Enable up navigation
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Matrix Configuration"

        repository = MatrixConfigRepository.getInstance(this)

        setupViews()
        loadConfiguration()
    }

    private fun setupViews() {
        binding.saveButton.setOnClickListener {
            saveConfiguration()
        }

        binding.testConnectionButton.setOnClickListener {
            testConnection()
        }

        // Show/hide user ID field based on authentication state
        updateUserIdVisibility()
    }

    private fun loadConfiguration() {
        currentConfig = repository.loadConfig()
        populateFields(currentConfig)
        updateConnectionStatus(currentConfig)
    }

    private fun populateFields(config: MatrixConfig) {
        binding.serverUrlInput.setText(config.serverUrl)
        binding.usernameInput.setText(config.username)
        binding.userIdInput.setText(config.userId)
        binding.enabledSwitch.isChecked = config.enabled

        // Don't populate password field for security
        // Clear it so user knows they need to re-enter if changing
        binding.passwordInput.setText("")

        updateUserIdVisibility()
    }

    private fun updateUserIdVisibility() {
        // Show user ID field only if we have an authenticated session
        val hasUserId = currentConfig.userId.isNotBlank()
        binding.userIdLayout.visibility = if (hasUserId) View.VISIBLE else View.GONE
    }

    private fun updateConnectionStatus(config: MatrixConfig) {
        val statusText = when {
            !config.isConfigured() -> "Not configured"
            !config.isAuthenticated() -> "Configured (not logged in)"
            config.enabled -> "Configured and enabled"
            else -> "Configured (disabled)"
        }

        binding.statusText.text = statusText

        // Format last connected timestamp
        val lastConnectedText = if (config.lastConnectedTimestamp > 0) {
            val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            "Last connected: ${dateFormat.format(Date(config.lastConnectedTimestamp))}"
        } else {
            "Last connected: Never"
        }

        binding.lastConnectedText.text = lastConnectedText
    }

    private fun saveConfiguration() {
        val serverUrl = binding.serverUrlInput.text?.toString()?.trim() ?: ""
        val username = binding.usernameInput.text?.toString()?.trim() ?: ""
        val password = binding.passwordInput.text?.toString()?.trim() ?: ""
        val enabled = binding.enabledSwitch.isChecked

        // Validate inputs
        if (enabled) {
            if (serverUrl.isBlank()) {
                binding.serverUrlInput.error = "Server URL is required"
                return
            }

            if (!serverUrl.startsWith("http://") && !serverUrl.startsWith("https://")) {
                binding.serverUrlInput.error = "Server URL must start with http:// or https://"
                return
            }

            if (username.isBlank() && currentConfig.accessToken.isBlank()) {
                binding.usernameInput.error = "Username is required"
                return
            }
        }

        // Create updated configuration
        val updatedConfig = currentConfig.copy(
            serverUrl = serverUrl,
            username = username,
            enabled = enabled,
            // Keep existing auth credentials unless changed
            // Note: Password is only used for initial login, not stored
        )

        // Save configuration
        repository.saveConfig(updatedConfig)
        currentConfig = updatedConfig

        Toast.makeText(this, "Configuration saved", Toast.LENGTH_SHORT).show()

        // Update UI
        updateConnectionStatus(updatedConfig)

        // If password was entered, show login prompt
        if (password.isNotBlank()) {
            showLoginPrompt(password)
        }
    }

    private fun testConnection() {
        val serverUrl = binding.serverUrlInput.text?.toString()?.trim() ?: ""

        if (serverUrl.isBlank()) {
            binding.serverUrlInput.error = "Server URL is required"
            return
        }

        if (!serverUrl.startsWith("http://") && !serverUrl.startsWith("https://")) {
            binding.serverUrlInput.error = "Server URL must start with http:// or https://"
            return
        }

        // Show progress
        binding.progressIndicator.visibility = View.VISIBLE
        binding.testConnectionButton.isEnabled = false

        // TODO: Implement actual Matrix server connection test
        // For now, just simulate a test
        binding.root.postDelayed({
            binding.progressIndicator.visibility = View.GONE
            binding.testConnectionButton.isEnabled = true

            // Placeholder - replace with actual connection test
            MaterialAlertDialogBuilder(this)
                .setTitle("Connection Test")
                .setMessage("Matrix connection testing will be implemented when the Matrix SDK is integrated.\n\nServer URL: $serverUrl")
                .setPositiveButton("OK", null)
                .show()
        }, 1000)
    }

    private fun showLoginPrompt(password: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Matrix Login")
            .setMessage("Password entered. Matrix authentication will be implemented when the Matrix SDK is integrated.\n\nUsername: ${currentConfig.username}\nServer: ${currentConfig.serverUrl}")
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
