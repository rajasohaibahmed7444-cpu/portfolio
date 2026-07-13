package com.notifycalc.app.data.model

/**
 * A single completed calculation shown in the calculator history list.
 */
data class HistoryEntry(
    /** The expression as the user typed it, e.g. "12×3+4". */
    val expression: String,
    /** The formatted result of the expression, e.g. "40". */
    val result: String,
    /** When the calculation was performed, in epoch milliseconds. */
    val timestamp: Long
)
