package com.technicallyrural.junction.app.ui

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.text.format.DateUtils
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.TooltipCompat
import androidx.core.content.ContextCompat
import com.technicallyrural.junction.app.R

/**
 * Material 3 status indicator for Matrix connection.
 *
 * Displays:
 * - Colored status dot (green/yellow/red/gray)
 * - "Matrix" label
 * - Tooltip on long-press with connection details
 *
 * Visual design:
 * - Compact chip-like appearance
 * - 8dp status dot with subtle shadow
 * - 12sp text label
 * - 4dp spacing
 * - Ripple effect on press
 */
class MatrixStatusView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val statusDot: View
    private val statusLabel: TextView

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(12.dpToPx(), 6.dpToPx(), 12.dpToPx(), 6.dpToPx())

        // Enable ripple effect
        isClickable = true
        isFocusable = true
        setBackgroundResource(selectableItemBackground())

        // Status dot (8dp circle)
        statusDot = View(context).apply {
            layoutParams = LayoutParams(8.dpToPx(), 8.dpToPx())
            background = createCircleDrawable(R.color.matrix_status_gray)
        }
        addView(statusDot)

        // Status label
        statusLabel = TextView(context).apply {
            layoutParams = LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = 6.dpToPx()
            }
            text = "Matrix"
            textSize = 12f
            setTextColor(ContextCompat.getColor(context, android.R.color.white))
        }
        addView(statusLabel)

        // Set initial tooltip
        TooltipCompat.setTooltipText(this, "Matrix: Not configured")
    }

    /**
     * Update visual state based on MatrixStatus.
     */
    fun setStatus(status: MatrixStatus) {
        when (status) {
            is MatrixStatus.Loading -> {
                setDotColor(R.color.matrix_status_gray)
                setTooltip("Matrix: Loading...")
            }
            is MatrixStatus.NotConfigured -> {
                setDotColor(R.color.matrix_status_gray)
                setTooltip("Matrix: Not configured\n\nTap settings to configure Matrix bridge")
            }
            is MatrixStatus.ConfiguredNotRunning -> {
                setDotColor(R.color.matrix_status_yellow)
                setTooltip(
                    "Matrix: Configured but not running\n\n" +
                    "Server: ${status.serverUrl}\n" +
                    "Status: Service not started (possible bug)"
                )
            }
            is MatrixStatus.Connected -> {
                setDotColor(R.color.matrix_status_green)
                val lastSyncText = formatTimestamp(status.lastMessageTime)
                setTooltip(
                    "Matrix: Connected\n\n" +
                    "Server: ${status.serverUrl}\n" +
                    "Last sync: $lastSyncText"
                )
            }
            is MatrixStatus.Disconnected -> {
                setDotColor(R.color.matrix_status_red)
                setTooltip(
                    "Matrix: Disconnected\n\n" +
                    "Server: ${status.serverUrl}\n" +
                    "Status: Not syncing"
                )
            }
        }
    }

    private fun setDotColor(colorResId: Int) {
        val color = ContextCompat.getColor(context, colorResId)
        statusDot.background = createCircleDrawable(colorResId)
    }

    private fun setTooltip(text: String) {
        TooltipCompat.setTooltipText(this, text)
    }

    private fun createCircleDrawable(colorResId: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(ContextCompat.getColor(context, colorResId))
            setSize(8.dpToPx(), 8.dpToPx())
        }
    }

    private fun formatTimestamp(timestampMs: Long): String {
        val now = System.currentTimeMillis()
        return DateUtils.getRelativeTimeSpanString(
            timestampMs,
            now,
            DateUtils.SECOND_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_RELATIVE
        ).toString()
    }

    private fun selectableItemBackground(): Int {
        val attrs = intArrayOf(android.R.attr.selectableItemBackground)
        val ta = context.obtainStyledAttributes(attrs)
        val resId = ta.getResourceId(0, 0)
        ta.recycle()
        return resId
    }

    private fun Int.dpToPx(): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }
}
