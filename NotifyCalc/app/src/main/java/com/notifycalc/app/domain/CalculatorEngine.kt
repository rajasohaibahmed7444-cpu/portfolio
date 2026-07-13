package com.notifycalc.app.domain

import java.math.BigDecimal
import java.math.MathContext

/** Base class for every error the calculator engine can report. */
sealed class CalculatorException : Exception()

/** Thrown when an expression divides by zero. */
class DivisionByZeroException : CalculatorException()

/** Thrown when an expression is incomplete or syntactically invalid. */
class MalformedExpressionException : CalculatorException()

/**
 * Stateless expression evaluator for the calculator.
 *
 * Expressions are plain strings built from digits, one optional decimal
 * point per number, the postfix percent sign and the four binary operators
 * (using the typographic symbols the UI displays: `+ − × ÷`). A `−` at the
 * start of the expression or directly after a binary operator is treated as
 * a unary minus. Evaluation uses [BigDecimal] with standard operator
 * precedence (`×`/`÷` before `+`/`−`).
 */
object CalculatorEngine {

    const val PLUS = '+'
    const val MINUS = '−' // U+2212
    const val MULTIPLY = '×'
    const val DIVIDE = '÷'
    const val PERCENT = '%'
    const val DECIMAL = '.'

    private val MATH_CONTEXT = MathContext.DECIMAL64
    private val ONE_HUNDRED = BigDecimal(100)
    private const val RESULT_PRECISION = 12
    private const val MAX_PLAIN_RESULT_LENGTH = 100

    /** True for the four binary operator symbols. */
    fun isBinaryOperator(c: Char): Boolean =
        c == PLUS || c == MINUS || c == MULTIPLY || c == DIVIDE

    /**
     * Evaluates a complete expression.
     *
     * @throws MalformedExpressionException if the expression is not complete
     *         and valid.
     * @throws DivisionByZeroException on division by zero.
     */
    fun evaluate(expression: String): BigDecimal =
        evaluateTokens(tokenize(expression))

    /**
     * Returns the formatted live-preview result for a partially typed
     * expression, or null when the expression is incomplete, invalid, not yet
     * a "real" calculation, or divides by zero.
     */
    fun preview(expression: String): String? = try {
        val tokens = tokenize(expression)
        val hasOperator = tokens.any { it is Token.Operator }
        if (hasOperator || expression.contains(PERCENT)) {
            format(evaluateTokens(tokens))
        } else {
            null
        }
    } catch (_: CalculatorException) {
        null
    }

    /**
     * True when the expression contains at least one binary operator or a
     * percent sign, i.e. when evaluating it produces something worth adding
     * to the history.
     */
    fun isMeaningfulCalculation(expression: String): Boolean = try {
        tokenize(expression).any { it is Token.Operator } || expression.contains(PERCENT)
    } catch (_: CalculatorException) {
        false
    }

    /**
     * Formats a result for display. The output only ever contains digits, a
     * decimal point and the typographic minus, so a formatted result can be
     * fed back into [evaluate] as the start of a new expression.
     */
    fun format(value: BigDecimal): String {
        val rounded = value.round(MathContext(RESULT_PRECISION)).stripTrailingZeros()
        val plain = rounded.toPlainString().replace('-', MINUS)
        if (plain.length <= MAX_PLAIN_RESULT_LENGTH) return plain
        // Extremely large or small magnitudes: fall back to scientific
        // notation rather than an unreadable wall of zeros.
        return rounded.toString().replace('-', MINUS)
    }

    // ------------------------------------------------------------------
    // Tokenizer
    // ------------------------------------------------------------------

    private sealed interface Token {
        data class Number(val value: BigDecimal) : Token
        data class Operator(val symbol: Char) : Token
    }

    private fun tokenize(expression: String): List<Token> {
        // Accept the ASCII hyphen-minus as a synonym for the display minus.
        val expr = expression.replace('-', MINUS)
        val tokens = mutableListOf<Token>()
        var i = 0
        while (i < expr.length) {
            val c = expr[i]
            when {
                c == MINUS && expectsOperand(tokens) -> {
                    // Unary minus: must be immediately followed by a number.
                    val (value, next) = readNumber(expr, i + 1)
                        ?: throw MalformedExpressionException()
                    tokens += Token.Number(value.negate())
                    i = next
                }

                c.isDigit() || c == DECIMAL -> {
                    if (!expectsOperand(tokens)) throw MalformedExpressionException()
                    val (value, next) = readNumber(expr, i)
                        ?: throw MalformedExpressionException()
                    tokens += Token.Number(value)
                    i = next
                }

                isBinaryOperator(c) -> {
                    if (expectsOperand(tokens)) throw MalformedExpressionException()
                    tokens += Token.Operator(c)
                    i++
                }

                else -> throw MalformedExpressionException()
            }
        }
        if (tokens.isEmpty() || tokens.last() is Token.Operator) {
            throw MalformedExpressionException()
        }
        return tokens
    }

    private fun expectsOperand(tokens: List<Token>): Boolean =
        tokens.isEmpty() || tokens.last() is Token.Operator

    /**
     * Reads one number (digits with at most one decimal point, followed by
     * optional percent signs) starting at [start]. Returns the parsed value
     * and the index of the first unconsumed character, or null when no valid
     * number starts at [start].
     */
    private fun readNumber(expr: String, start: Int): Pair<BigDecimal, Int>? {
        var i = start
        var digits = 0
        var dots = 0
        while (i < expr.length && (expr[i].isDigit() || expr[i] == DECIMAL)) {
            if (expr[i] == DECIMAL) dots++ else digits++
            i++
        }
        if (digits == 0 || dots > 1) return null
        var value = BigDecimal(expr.substring(start, i))
        while (i < expr.length && expr[i] == PERCENT) {
            value = value.divide(ONE_HUNDRED, MATH_CONTEXT)
            i++
        }
        return value to i
    }

    // ------------------------------------------------------------------
    // Evaluator
    // ------------------------------------------------------------------

    private fun evaluateTokens(tokens: List<Token>): BigDecimal {
        // First pass: collapse × and ÷ (higher precedence).
        val numbers = mutableListOf((tokens[0] as Token.Number).value)
        val pendingOps = mutableListOf<Char>()
        var i = 1
        while (i < tokens.size) {
            val op = (tokens[i] as Token.Operator).symbol
            val value = (tokens[i + 1] as Token.Number).value
            if (op == MULTIPLY || op == DIVIDE) {
                val left = numbers.removeAt(numbers.lastIndex)
                numbers += if (op == MULTIPLY) {
                    left.multiply(value, MATH_CONTEXT)
                } else {
                    if (value.compareTo(BigDecimal.ZERO) == 0) throw DivisionByZeroException()
                    left.divide(value, MATH_CONTEXT)
                }
            } else {
                pendingOps += op
                numbers += value
            }
            i += 2
        }
        // Second pass: apply + and − left to right.
        var result = numbers[0]
        for (j in pendingOps.indices) {
            result = if (pendingOps[j] == PLUS) {
                result.add(numbers[j + 1], MATH_CONTEXT)
            } else {
                result.subtract(numbers[j + 1], MATH_CONTEXT)
            }
        }
        return result
    }
}
