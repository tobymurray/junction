package com.technicallyrural.junction.app.ui

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.technicallyrural.junction.app.R
import com.technicallyrural.junction.app.databinding.ActivityMatrixConfigBinding
import com.technicallyrural.junction.app.matrix.MatrixConfig
import com.technicallyrural.junction.app.matrix.MatrixConfigRepository
import com.technicallyrural.junction.app.service.MatrixSyncService
import com.technicallyrural.junction.matrix.impl.TrixnityClientManager
import com.technicallyrural.junction.matrix.impl.TrixnityMatrixBridge
import com.technicallyrural.junction.matrix.impl.SimpleRoomMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    private lateinit var clientManager: TrixnityClientManager
    private var currentConfig: MatrixConfig = MatrixConfig.EMPTY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMatrixConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Enable up navigation
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Matrix Configuration"

        repository = MatrixConfigRepository.getInstance(this)
        clientManager = TrixnityClientManager(applicationContext)

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
        // Determine status and color
        val (statusText, indicatorColor) = when {
            !config.isConfigured() -> {
                "Not configured" to Color.parseColor("#9E9E9E") // Gray
            }
            !config.isAuthenticated() -> {
                "Configured (not logged in)" to Color.parseColor("#FF9800") // Amber/Orange
            }
            config.enabled -> {
                "Authenticated and enabled" to Color.parseColor("#4CAF50") // Green
            }
            else -> {
                "Authenticated (disabled)" to Color.parseColor("#FF9800") // Amber/Orange
            }
        }

        binding.statusText.text = statusText
        binding.statusIndicator.setColorFilter(indicatorColor)

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

        // Start/stop Matrix sync service based on enabled state
        updateServiceState(updatedConfig)

        // If password was entered, show login prompt
        if (password.isNotBlank()) {
            showLoginPrompt(password)
        }
    }

    private fun testConnection() {
        val serverUrl = binding.serverUrlInput.text?.toString()?.trim() ?: ""
        val username = binding.usernameInput.text?.toString()?.trim() ?: ""
        val password = binding.passwordInput.text?.toString()?.trim() ?: ""

        Log.d(TAG, "testConnection: serverUrl=$serverUrl, username=$username")

        if (serverUrl.isBlank()) {
            binding.serverUrlInput.error = "Server URL is required"
            return
        }

        if (!serverUrl.startsWith("http://") && !serverUrl.startsWith("https://")) {
            binding.serverUrlInput.error = "Server URL must start with http:// or https://"
            return
        }

        if (username.isBlank()) {
            binding.usernameInput.error = "Username is required for connection test"
            return
        }

        if (password.isBlank()) {
            binding.passwordInput.error = "Password is required for connection test"
            return
        }

        Log.d(TAG, "testConnection: Starting login attempt...")

        // Show progress
        binding.progressIndicator.visibility = View.VISIBLE
        binding.testConnectionButton.isEnabled = false
        binding.saveButton.isEnabled = false

        // Test connection in background
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    Log.d(TAG, "testConnection: Calling clientManager.login()...")
                    val loginResult = clientManager.login(serverUrl, username, password)
                    Log.d(TAG, "testConnection: Login result: $loginResult")
                    loginResult
                } catch (e: Exception) {
                    Log.e(TAG, "testConnection: Exception during login", e)
                    TrixnityClientManager.LoginResult.Error(e.message ?: "Unknown error")
                }
            }

            // Hide progress
            binding.progressIndicator.visibility = View.GONE
            binding.testConnectionButton.isEnabled = true
            binding.saveButton.isEnabled = true

            // Show result
            Log.d(TAG, "testConnection: Showing result dialog...")
            when (result) {
                is TrixnityClientManager.LoginResult.Success -> {
                    Log.d(TAG, "testConnection: Success! userId=${result.userId}")
                    MaterialAlertDialogBuilder(this@MatrixConfigActivity)
                        .setTitle("✓ Connection Successful")
                        .setMessage(
                            "Successfully connected to Matrix homeserver!\n\n" +
                                    "Server: $serverUrl\n" +
                                    "User ID: ${result.userId}\n" +
                                    "Device ID: ${result.deviceId}\n\n" +
                                    "Click Save to store these credentials."
                        )
                        .setPositiveButton("OK") { _, _ ->
                            // Save authenticated configuration
                            val authenticatedConfig = currentConfig.copy(
                                userId = result.userId,
                                accessToken = result.accessToken,
                                deviceId = result.deviceId,
                                lastConnectedTimestamp = System.currentTimeMillis()
                            )
                            repository.saveConfig(authenticatedConfig)
                            currentConfig = authenticatedConfig

                            updateConnectionStatus(currentConfig)
                            binding.userIdInput.setText(result.userId)
                            updateUserIdVisibility()

                            // Start sync service if enabled
                            updateServiceState(authenticatedConfig)

                            // Trigger control room creation for testing
                            createControlRoomForTesting()
                        }
                        .show()
                }
                is TrixnityClientManager.LoginResult.Error -> {
                    Log.e(TAG, "testConnection: Login failed: ${result.message}")
                    MaterialAlertDialogBuilder(this@MatrixConfigActivity)
                        .setTitle("✗ Connection Failed")
                        .setMessage(
                            "Failed to connect to Matrix homeserver.\n\n" +
                                    "Server: $serverUrl\n" +
                                    "Error: ${result.message}\n\n" +
                                    "Please check your server URL, username, and password."
                        )
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
        }
    }

    private fun showLoginPrompt(password: String) {
        val serverUrl = currentConfig.serverUrl
        val username = currentConfig.username

        if (serverUrl.isBlank() || username.isBlank()) {
            Toast.makeText(this, "Server URL and username are required", Toast.LENGTH_SHORT).show()
            return
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Matrix Login")
            .setMessage(
                "Authenticate with Matrix homeserver?\n\n" +
                        "Server: $serverUrl\n" +
                        "Username: $username\n\n" +
                        "This will test the connection and store your credentials."
            )
            .setPositiveButton("Login") { _, _ ->
                performLogin(serverUrl, username, password)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performLogin(serverUrl: String, username: String, password: String) {
        // Show progress
        binding.progressIndicator.visibility = View.VISIBLE
        binding.saveButton.isEnabled = false
        binding.testConnectionButton.isEnabled = false

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    clientManager.login(serverUrl, username, password)
                } catch (e: Exception) {
                    TrixnityClientManager.LoginResult.Error(e.message ?: "Unknown error")
                }
            }

            // Hide progress
            binding.progressIndicator.visibility = View.GONE
            binding.saveButton.isEnabled = true
            binding.testConnectionButton.isEnabled = true

            when (result) {
                is TrixnityClientManager.LoginResult.Success -> {
                    // Save the authenticated configuration
                    val authenticatedConfig = currentConfig.copy(
                        userId = result.userId,
                        accessToken = result.accessToken,
                        deviceId = result.deviceId,
                        lastConnectedTimestamp = System.currentTimeMillis()
                    )
                    repository.saveConfig(authenticatedConfig)
                    currentConfig = authenticatedConfig

                    Toast.makeText(
                        this@MatrixConfigActivity,
                        "Login successful! User: ${result.userId}",
                        Toast.LENGTH_LONG
                    ).show()

                    // Update UI
                    binding.userIdInput.setText(result.userId)
                    updateUserIdVisibility()
                    updateConnectionStatus(authenticatedConfig)

                    // Clear password field
                    binding.passwordInput.setText("")

                    // Start sync service if enabled
                    updateServiceState(authenticatedConfig)

                    // Trigger control room creation for testing
                    createControlRoomForTesting()
                }
                is TrixnityClientManager.LoginResult.Error -> {
                    MaterialAlertDialogBuilder(this@MatrixConfigActivity)
                        .setTitle("Login Failed")
                        .setMessage("Failed to authenticate with Matrix homeserver.\n\nError: ${result.message}")
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
        }
    }

    /**
     * Create control room for testing purposes.
     * This demonstrates the control room creation feature.
     */
    private fun createControlRoomForTesting() {
        lifecycleScope.launch {
            try {
                // Create bridge instance (normally done by MatrixRegistry)
                val serverDomain = currentConfig.serverUrl
                    .removePrefix("https://")
                    .removePrefix("http://")
                    .substringBefore(":")
                    .substringBefore("/")

                val roomMapper = SimpleRoomMapper(
                    context = applicationContext,
                    clientManager = clientManager,
                    homeserverDomain = serverDomain
                )

                val bridge = TrixnityMatrixBridge(
                    context = applicationContext,
                    clientManager = clientManager,
                    roomMapper = roomMapper
                )

                // Trigger control room creation
                val controlRoomId = withContext(Dispatchers.IO) {
                    bridge.getControlRoomId()
                }

                if (controlRoomId != null) {
                    Log.d(TAG, "Control room created: $controlRoomId")
                    Toast.makeText(
                        this@MatrixConfigActivity,
                        "Control room created: $controlRoomId",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Log.w(TAG, "Control room creation returned null")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create control room", e)
            }
        }
    }

    /**
     * Start or stop Matrix sync service based on configuration.
     */
    private fun updateServiceState(config: MatrixConfig) {
        if (config.enabled && config.isAuthenticated()) {
            // Start sync service
            Log.d(TAG, "Starting Matrix sync service")
            MatrixSyncService.start(this)
            Toast.makeText(this, "Matrix sync started", Toast.LENGTH_SHORT).show()
        } else {
            // Stop sync service
            Log.d(TAG, "Stopping Matrix sync service")
            MatrixSyncService.stop(this)
        }
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

    companion object {
        private const val TAG = "MatrixConfigActivity"
    }
}
