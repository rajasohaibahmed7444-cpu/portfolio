package com.notifycalc.app.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.color.MaterialColors
import com.notifycalc.app.R
import com.notifycalc.app.data.preferences.AppPreferences
import com.notifycalc.app.databinding.ActivityMainBinding
import com.notifycalc.app.databinding.DialogHistoryBinding
import com.notifycalc.app.domain.CalculatorEngine
import com.notifycalc.app.ui.calculator.CalculatorError
import com.notifycalc.app.ui.calculator.CalculatorUiState
import com.notifycalc.app.ui.calculator.CalculatorViewModel
import com.notifycalc.app.ui.history.HistoryAdapter
import com.notifycalc.app.ui.settings.SettingsActivity
import com.notifycalc.app.ui.welcome.WelcomeActivity
import kotlinx.coroutines.launch

/**
 * The calculator screen and launcher activity.
 *
 * On the very first start it forwards to [WelcomeActivity] so the user can
 * decide about the optional Notification Backup feature; afterwards it always
 * opens straight into the calculator.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: CalculatorViewModel by viewModels()

    private var historyDialog: BottomSheetDialog? = null
    private var wasResult = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!AppPreferences(this).isWelcomeCompleted) {
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupTopBar()
        setupKeypad()
        observeUiState()
    }

    override fun onDestroy() {
        historyDialog?.dismiss()
        historyDialog = null
        super.onDestroy()
    }

    private fun setupTopBar() {
        binding.btnHistory.setOnClickListener { showHistorySheet() }
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun setupKeypad() {
        val digitButtons = mapOf(
            binding.btn0 to '0', binding.btn1 to '1', binding.btn2 to '2',
            binding.btn3 to '3', binding.btn4 to '4', binding.btn5 to '5',
            binding.btn6 to '6', binding.btn7 to '7', binding.btn8 to '8',
            binding.btn9 to '9'
        )
        digitButtons.forEach { (button, digit) ->
            button.setOnClickListener { viewModel.onDigit(digit) }
        }

        binding.btnAdd.setOnClickListener { viewModel.onOperator(CalculatorEngine.PLUS) }
        binding.btnSubtract.setOnClickListener { viewModel.onOperator(CalculatorEngine.MINUS) }
        binding.btnMultiply.setOnClickListener { viewModel.onOperator(CalculatorEngine.MULTIPLY) }
        binding.btnDivide.setOnClickListener { viewModel.onOperator(CalculatorEngine.DIVIDE) }

        binding.btnDecimal.setOnClickListener { viewModel.onDecimal() }
        binding.btnPercent.setOnClickListener { viewModel.onPercent() }
        binding.btnSign.setOnClickListener { viewModel.onToggleSign() }
        binding.btnDelete.setOnClickListener { viewModel.onDelete() }
        binding.btnClear.setOnClickListener { viewModel.onClear() }
        binding.btnEquals.setOnClickListener { viewModel.onEquals() }
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state -> render(state) }
            }
        }
    }

    private fun render(state: CalculatorUiState) {
        binding.textExpression.text = state.expression.ifEmpty { getString(R.string.calc_zero) }
        binding.scrollExpression.post {
            binding.scrollExpression.fullScroll(View.FOCUS_RIGHT)
        }

        val previewColor: Int
        val previewText: String
        if (state.error != null) {
            previewText = getString(
                when (state.error) {
                    CalculatorError.DIVISION_BY_ZERO -> R.string.error_divide_by_zero
                    CalculatorError.INVALID_EXPRESSION -> R.string.error_invalid_expression
                }
            )
            previewColor = MaterialColors.getColor(
                binding.textPreview, com.google.android.material.R.attr.colorError
            )
        } else {
            previewText = state.preview
            previewColor = MaterialColors.getColor(
                binding.textPreview, com.google.android.material.R.attr.colorOnSurfaceVariant
            )
        }
        binding.textPreview.text = previewText
        binding.textPreview.setTextColor(previewColor)

        if (state.isResult && !wasResult) {
            animateResultCommit()
        }
        wasResult = state.isResult
    }

    /** Small pop animation when an expression collapses into its result. */
    private fun animateResultCommit() {
        binding.textExpression.apply {
            scaleX = 0.92f
            scaleY = 0.92f
            alpha = 0.5f
            pivotX = width.toFloat()
            pivotY = height.toFloat()
            animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(200L)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }

    private fun showHistorySheet() {
        val dialog = BottomSheetDialog(this)
        val sheetBinding = DialogHistoryBinding.inflate(layoutInflater)

        val adapter = HistoryAdapter { entry ->
            viewModel.onHistorySelected(entry)
            dialog.dismiss()
        }
        sheetBinding.recyclerHistory.adapter = adapter

        val entries = viewModel.history.value
        adapter.submitList(entries)
        sheetBinding.textHistoryEmpty.visibility =
            if (entries.isEmpty()) View.VISIBLE else View.GONE
        sheetBinding.btnClearHistory.visibility =
            if (entries.isEmpty()) View.GONE else View.VISIBLE

        sheetBinding.btnClearHistory.setOnClickListener {
            viewModel.clearHistory()
            dialog.dismiss()
        }

        dialog.setContentView(sheetBinding.root)
        dialog.setOnDismissListener { historyDialog = null }
        historyDialog = dialog
        dialog.show()
    }
}
