package com.example.stockkeeper.ui.directories

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.stockkeeper.R

class DirectoryAdapter(
    private val onClick: (DirectoryEntry) -> Unit,
    private val onDelete: (DirectoryEntry) -> Unit,
) :
    ListAdapter<DirectoryEntry, DirectoryAdapter.Holder>(Diff) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_directory, parent, false) as ViewGroup,
    )

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(getItem(position))

    inner class Holder(private val root: ViewGroup) : RecyclerView.ViewHolder(root) {
        fun bind(item: DirectoryEntry) {
            root.findViewById<TextView>(R.id.directoryTitle).text = item.title
            root.findViewById<TextView>(R.id.directorySubtitle).apply {
                text = item.subtitle
                visibility = if (item.subtitle.isBlank()) android.view.View.GONE else android.view.View.VISIBLE
            }
            root.setOnClickListener { onClick(item) }
            root.findViewById<android.view.View>(R.id.deleteDirectoryEntry).setOnClickListener { onDelete(item) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<DirectoryEntry>() {
        override fun areItemsTheSame(old: DirectoryEntry, new: DirectoryEntry) = old::class == new::class && old.id == new.id
        override fun areContentsTheSame(old: DirectoryEntry, new: DirectoryEntry) = old == new
    }
}
