package com.example.stockkeeper.ui.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.stockkeeper.R
import com.example.stockkeeper.data.local.entity.StockTransactionType
import com.example.stockkeeper.data.local.model.StockTransactionDetails
import java.text.DateFormat
import java.util.Date

class HistoryAdapter : ListAdapter<StockTransactionDetails, HistoryAdapter.Holder>(Diff) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_history, parent, false),
    )

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(getItem(position))

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(item: StockTransactionDetails) = with(itemView.context) {
            itemView.findViewById<TextView>(R.id.operationType).setText(when (item.type) {
                StockTransactionType.RECEIPT -> R.string.operation_receipt
                StockTransactionType.SALE -> R.string.operation_sale
                StockTransactionType.WRITE_OFF -> R.string.operation_write_off
                StockTransactionType.ADJUSTMENT -> R.string.operation_adjustment
            })
            itemView.findViewById<TextView>(R.id.operationQuantity).text =
                getString(R.string.operation_quantity_format, item.quantityDelta)
            itemView.findViewById<TextView>(R.id.productName).text =
                "${item.productName} · ${item.article}"
            itemView.findViewById<TextView>(R.id.operationDetails).text = when (item.type) {
                StockTransactionType.SALE -> getString(
                    R.string.customer_format,
                    item.customerName ?: getString(R.string.unknown_customer),
                )
                StockTransactionType.WRITE_OFF -> getString(
                    R.string.reason_format,
                    item.reason ?: getString(R.string.unknown_reason),
                )
                else -> item.reason?.let { getString(R.string.note_format, it) }.orEmpty()
            }
            itemView.findViewById<TextView>(R.id.operationDate).text =
                dateFormat.format(Date(item.occurredAt))
        }
    }

    private object Diff : DiffUtil.ItemCallback<StockTransactionDetails>() {
        override fun areItemsTheSame(oldItem: StockTransactionDetails, newItem: StockTransactionDetails) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: StockTransactionDetails, newItem: StockTransactionDetails) = oldItem == newItem
    }

    private companion object {
        private val dateFormat: DateFormat =
            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
    }
}
