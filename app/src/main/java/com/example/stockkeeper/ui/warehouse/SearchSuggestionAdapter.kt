package com.example.stockkeeper.ui.warehouse

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.example.stockkeeper.R

data class SearchSuggestion(
    val label: String,
    val isRecent: Boolean,
) {
    override fun toString(): String = label
}

class SearchSuggestionAdapter(context: Context) :
    ArrayAdapter<SearchSuggestion>(context, R.layout.item_search_suggestion, mutableListOf()) {

    fun submit(items: List<SearchSuggestion>) {
        clear()
        addAll(items)
        notifyDataSetChanged()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
        bind(position, convertView, parent)

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View =
        bind(position, convertView, parent)

    private fun bind(position: Int, recycled: View?, parent: ViewGroup): View {
        val view = recycled ?: LayoutInflater.from(context)
            .inflate(R.layout.item_search_suggestion, parent, false)
        val item = getItem(position) ?: return view
        view.findViewById<TextView>(R.id.suggestionText).text = item.label
        view.findViewById<ImageView>(R.id.recentSearchIcon).visibility =
            if (item.isRecent) View.VISIBLE else View.GONE
        return view
    }
}
