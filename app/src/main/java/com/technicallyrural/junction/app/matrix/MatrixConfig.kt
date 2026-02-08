package com.technicallyrural.junction.app.matrix

import android.os.Parcel
import android.os.Parcelable

/**
 * Matrix server configuration.
 *
 * Stores the connection details for a Matrix homeserver including
 * authentication credentials and connection status.
 */
data class MatrixConfig(
    /**
     * Matrix homeserver URL (e.g., "https://matrix.org")
     */
    val serverUrl: String = "",

    /**
     * Matrix user ID (e.g., "@user:matrix.org")
     * This is set after successful login.
     */
    val userId: String = "",

    /**
     * Matrix username for login (without @ or :server)
     */
    val username: String = "",

    /**
     * Matrix access token obtained after login.
     * This should be stored securely.
     */
    val accessToken: String = "",

    /**
     * Device ID assigned by the Matrix server.
     */
    val deviceId: String = "",

    /**
     * Whether Matrix integration is enabled.
     */
    val enabled: Boolean = false,

    /**
     * Timestamp of last successful connection (milliseconds since epoch).
     * 0 means never connected.
     */
    val lastConnectedTimestamp: Long = 0L
) : Parcelable {

    /**
     * Returns true if configuration has minimum required fields for connection attempt.
     */
    fun isConfigured(): Boolean {
        return serverUrl.isNotBlank() &&
               (accessToken.isNotBlank() || username.isNotBlank())
    }

    /**
     * Returns true if we have valid authentication credentials.
     */
    fun isAuthenticated(): Boolean {
        return userId.isNotBlank() && accessToken.isNotBlank()
    }

    // Parcelable implementation
    constructor(parcel: Parcel) : this(
        serverUrl = parcel.readString() ?: "",
        userId = parcel.readString() ?: "",
        username = parcel.readString() ?: "",
        accessToken = parcel.readString() ?: "",
        deviceId = parcel.readString() ?: "",
        enabled = parcel.readByte() != 0.toByte(),
        lastConnectedTimestamp = parcel.readLong()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(serverUrl)
        parcel.writeString(userId)
        parcel.writeString(username)
        parcel.writeString(accessToken)
        parcel.writeString(deviceId)
        parcel.writeByte(if (enabled) 1 else 0)
        parcel.writeLong(lastConnectedTimestamp)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<MatrixConfig> {
        override fun createFromParcel(parcel: Parcel): MatrixConfig {
            return MatrixConfig(parcel)
        }

        override fun newArray(size: Int): Array<MatrixConfig?> {
            return arrayOfNulls(size)
        }

        /**
         * Default empty configuration.
         */
        val EMPTY = MatrixConfig()
    }
}
