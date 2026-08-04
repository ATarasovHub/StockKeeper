package com.example.stockkeeper.ui.warehouse

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.dispose
import coil.load
import com.example.stockkeeper.R
import com.example.stockkeeper.data.local.model.ProductStockItem
import com.example.stockkeeper.data.photo.ProductPhotoStore

class ProductAdapter(
    private val onClick: (ProductStockItem) -> Unit,
) : ListAdapter<ProductStockItem, ProductAdapter.ProductHolder>(Diff) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductHolder = ProductHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_product, parent, false),
    )

    override fun onBindViewHolder(holder: ProductHolder, position: Int) =
        holder.bind(getItem(position), onClick)

    class ProductHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val initial: TextView = view.findViewById(R.id.productInitial)
        private val photo: ImageView = view.findViewById(R.id.productPhotoThumbnail)
        private val name: TextView = view.findViewById(R.id.productName)
        private val article: TextView = view.findViewById(R.id.productArticle)
        private val meta: TextView = view.findViewById(R.id.productMeta)
        private val quantity: TextView = view.findViewById(R.id.productQuantity)

        fun bind(item: ProductStockItem, onClick: (ProductStockItem) -> Unit) {
            val context = itemView.context
            initial.text = item.name.firstOrNull()?.uppercase() ?: "?"
            val photoFile = ProductPhotoStore.file(context, item.photoPath)
            photo.isVisible = photoFile != null
            initial.isVisible = photoFile == null
            if (photoFile != null) {
                photo.load(photoFile) {
                    crossfade(true)
                }
            } else {
                photo.dispose()
                photo.setImageDrawable(null)
            }
            name.text = item.name
            article.text = context.getString(R.string.article_format, item.article)
            val manufacturer = item.manufacturerName ?: context.getString(R.string.manufacturer_not_set)
            val location = if (!item.rack.isNullOrBlank() || !item.shelf.isNullOrBlank()) {
                context.getString(R.string.location_format, item.rack.orEmpty(), item.shelf.orEmpty())
            } else context.getString(R.string.location_not_set)
            meta.text = "$manufacturer · $location"
            quantity.text = context.getString(R.string.quantity_format, item.quantity)
            itemView.setOnClickListener { onClick(item) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<ProductStockItem>() {
        override fun areItemsTheSame(oldItem: ProductStockItem, newItem: ProductStockItem) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: ProductStockItem, newItem: ProductStockItem) = oldItem == newItem
    }
}
