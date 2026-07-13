package com.notifycalc.app.ui.history

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.notifycalc.app.data.model.HistoryEntry
import com.notifycalc.app.databinding.ItemHistoryBinding

/**
 * RecyclerView adapter for the calculation history bottom sheet.
 * Tapping an entry hands it back through [onEntryClick].
 */
class HistoryAdapter(
    private val onEntryClick: (HistoryEntry) -> Unit
) : ListAdapter<HistoryEntry, HistoryAdapter.HistoryViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemHistoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class HistoryViewHolder(
        private val binding: ItemHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(entry: HistoryEntry) {
            binding.textItemExpression.text = entry.expression
            binding.textItemResult.text = entry.result
            binding.textItemTime.text = DateUtils.getRelativeTimeSpanString(
                entry.timestamp,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
            )
            binding.root.setOnClickListener { onEntryClick(entry) }
        }
    }

    private companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<HistoryEntry>() {
            override fun areItemsTheSame(oldItem: HistoryEntry, newItem: HistoryEntry): Boolean =
                oldItem.timestamp == newItem.timestamp && oldItem.expression == newItem.expression

            override fun areContentsTheSame(oldItem: HistoryEntry, newItem: HistoryEntry): Boolean =
                oldItem == newItem
        }
    }
}
