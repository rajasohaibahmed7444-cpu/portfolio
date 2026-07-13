package com.notifycalc.app.ui.calculator

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.notifycalc.app.data.model.HistoryEntry
import com.notifycalc.app.data.preferences.AppPreferences
import com.notifycalc.app.domain.CalculatorEngine
import com.notifycalc.app.domain.CalculatorException
import com.notifycalc.app.domain.DivisionByZeroException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** User-visible error categories produced by the calculator. */
enum class CalculatorError {
    DIVISION_BY_ZERO,
    INVALID_EXPRESSION
}

/** Immutable UI state for the calculator screen. */
data class CalculatorUiState(
    /** The expression as typed, using display symbols (`+ − × ÷ %`). */
    val expression: String = "",
    /** Live result preview shown under the expression, empty when n/a. */
    val preview: String = "",
    /** True when [expression] is a committed result of pressing equals. */
    val isResult: Boolean = false,
    /** Non-null when the last equals press failed. */
    val error: CalculatorError? = null
)

/**
 * ViewModel driving the calculator screen. Holds the current expression,
 * a live preview of its value, and the persisted calculation history.
 */
class CalculatorViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = AppPreferences(application)

    private val _uiState = MutableStateFlow(CalculatorUiState())
    val uiState: StateFlow<CalculatorUiState> = _uiState.asStateFlow()

    private val _history = MutableStateFlow<List<HistoryEntry>>(emptyList())
    val history: StateFlow<List<HistoryEntry>> = _history.asStateFlow()

    init {
        _history.value = preferences.loadHistory()
    }

    // ------------------------------------------------------------------
    // Key handlers
    // ------------------------------------------------------------------

    fun onDigit(digit: Char) {
        val state = _uiState.value
        if (state.isResult) {
            setExpression(digit.toString())
            return
        }
        val expr = state.expression
        if (expr.length >= MAX_EXPRESSION_LENGTH) return
        // A digit may not directly follow a percent sign.
        if (expr.lastOrNull() == CalculatorEngine.PERCENT) return
        setExpression(expr + digit)
    }

    fun onOperator(op: Char) {
        val state = _uiState.value
        val expr = state.expression
        if (expr.isEmpty()) {
            // Only a leading unary minus makes sense on an empty display.
            if (op == CalculatorEngine.MINUS) setExpression(op.toString())
            return
        }
        val lastIndex = expr.lastIndex
        val last = expr[lastIndex]
        when {
            last == CalculatorEngine.DECIMAL -> return
            CalculatorEngine.isBinaryOperator(last) -> {
                // Keep a pending unary minus; otherwise replace the operator.
                if (isUnaryMinusAt(expr, lastIndex)) return
                setExpression(expr.substring(0, lastIndex) + op)
            }

            else -> {
                if (expr.length >= MAX_EXPRESSION_LENGTH) return
                setExpression(expr + op)
            }
        }
    }

    fun onDecimal() {
        val state = _uiState.value
        if (state.isResult) {
            setExpression("0.")
            return
        }
        val expr = state.expression
        if (expr.length >= MAX_EXPRESSION_LENGTH) return
        if (expr.lastOrNull() == CalculatorEngine.PERCENT) return
        val currentNumber = expr.substring(currentNumberStart(expr))
        if (currentNumber.contains(CalculatorEngine.DECIMAL)) return
        val last = expr.lastOrNull()
        val addition = if (last == null || CalculatorEngine.isBinaryOperator(last)) "0." else "."
        setExpression(expr + addition)
    }

    fun onPercent() {
        val state = _uiState.value
        val expr = state.expression
        if (expr.isEmpty() || expr.length >= MAX_EXPRESSION_LENGTH) return
        // Percent only makes sense directly after a digit.
        if (!expr.last().isDigit()) return
        setExpression(expr + CalculatorEngine.PERCENT)
    }

    fun onToggleSign() {
        val state = _uiState.value
        val expr = state.expression
        if (expr.isEmpty()) {
            setExpression(CalculatorEngine.MINUS.toString())
            return
        }
        val numberStart = currentNumberStart(expr)
        if (numberStart == expr.length) {
            // The expression ends with an operator: toggle a pending unary minus.
            if (isUnaryMinusAt(expr, expr.lastIndex)) {
                setExpression(expr.dropLast(1))
            } else if (expr.length < MAX_EXPRESSION_LENGTH) {
                setExpression(expr + CalculatorEngine.MINUS)
            }
            return
        }
        if (numberStart > 0 && isUnaryMinusAt(expr, numberStart - 1)) {
            setExpression(expr.removeRange(numberStart - 1, numberStart))
        } else if (expr.length < MAX_EXPRESSION_LENGTH) {
            setExpression(
                expr.substring(0, numberStart) + CalculatorEngine.MINUS + expr.substring(numberStart)
            )
        }
    }

    fun onDelete() {
        val expr = _uiState.value.expression
        if (expr.isEmpty()) return
        setExpression(expr.dropLast(1))
    }

    fun onClear() {
        setExpression("")
    }

    fun onEquals() {
        val state = _uiState.value
        if (state.isResult) return
        val expr = sanitizeForEvaluation(state.expression)
        if (expr.isEmpty()) return
        try {
            val result = CalculatorEngine.evaluate(expr)
            val formatted = CalculatorEngine.format(result)
            if (CalculatorEngine.isMeaningfulCalculation(expr)) {
                addHistoryEntry(expr, formatted)
            }
            _uiState.value = CalculatorUiState(expression = formatted, isResult = true)
        } catch (_: DivisionByZeroException) {
            _uiState.value = state.copy(error = CalculatorError.DIVISION_BY_ZERO)
        } catch (_: CalculatorException) {
            _uiState.value = state.copy(error = CalculatorError.INVALID_EXPRESSION)
        }
    }

    /** Replaces the current expression with a result picked from history. */
    fun onHistorySelected(entry: HistoryEntry) {
        _uiState.value = CalculatorUiState(expression = entry.result, isResult = true)
    }

    fun clearHistory() {
        _history.value = emptyList()
        persistHistory(emptyList())
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    private fun setExpression(expression: String) {
        _uiState.value = CalculatorUiState(
            expression = expression,
            preview = CalculatorEngine.preview(expression).orEmpty()
        )
    }

    /** Index where the trailing number (digits, decimal point, percent) begins. */
    private fun currentNumberStart(expr: String): Int {
        var i = expr.length
        while (i > 0) {
            val c = expr[i - 1]
            if (c.isDigit() || c == CalculatorEngine.DECIMAL || c == CalculatorEngine.PERCENT) {
                i--
            } else {
                break
            }
        }
        return i
    }

    private fun isUnaryMinusAt(expr: String, index: Int): Boolean =
        expr[index] == CalculatorEngine.MINUS &&
            (index == 0 || CalculatorEngine.isBinaryOperator(expr[index - 1]))

    /** Drops dangling operators and decimal points before evaluating. */
    private fun sanitizeForEvaluation(expression: String): String {
        var expr = expression
        while (expr.isNotEmpty() &&
            (CalculatorEngine.isBinaryOperator(expr.last()) || expr.last() == CalculatorEngine.DECIMAL)
        ) {
            expr = expr.dropLast(1)
        }
        return expr
    }

    private fun addHistoryEntry(expression: String, result: String) {
        val updated = (
            listOf(HistoryEntry(expression, result, System.currentTimeMillis())) + _history.value
            ).take(MAX_HISTORY_ENTRIES)
        _history.value = updated
        persistHistory(updated)
    }

    private fun persistHistory(entries: List<HistoryEntry>) {
        viewModelScope.launch(Dispatchers.IO) {
            preferences.saveHistory(entries)
        }
    }

    private companion object {
        const val MAX_EXPRESSION_LENGTH = 100
        const val MAX_HISTORY_ENTRIES = 50
    }
}
