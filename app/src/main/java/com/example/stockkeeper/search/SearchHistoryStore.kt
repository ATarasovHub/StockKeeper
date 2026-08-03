package com.example.stockkeeper.search

import android.content.Context

class SearchHistoryStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun get(): List<String> = preferences.getString(KEY_HISTORY, null)
        ?.split(SEPARATOR)
        ?.map(String::trim)
        ?.filter(String::isNotEmpty)
        ?.take(MAX_ITEMS)
        .orEmpty()

    fun add(query: String): List<String> {
        val cleanQuery = query.trim().replace(SEPARATOR, " ")
        if (cleanQuery.isEmpty()) return get()
        val updated = buildList {
            add(cleanQuery)
            addAll(get().filterNot { it.equals(cleanQuery, ignoreCase = true) })
        }.take(MAX_ITEMS)
        preferences.edit().putString(KEY_HISTORY, updated.joinToString(SEPARATOR)).apply()
        return updated
    }

    companion object {
        private const val PREFERENCES_NAME = "stockkeeper_search"
        private const val KEY_HISTORY = "recent_queries"
        private const val SEPARATOR = "\n"
        const val MAX_ITEMS = 10
    }
}
