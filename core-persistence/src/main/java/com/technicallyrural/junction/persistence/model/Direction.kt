package com.technicallyrural.junction.persistence.model

enum class Direction {
    /**
     * SMS/MMS received → forwarded to Matrix.
     */
    SMS_TO_MATRIX,

    /**
     * Matrix message received → sent as SMS/MMS.
     */
    MATRIX_TO_SMS
}
