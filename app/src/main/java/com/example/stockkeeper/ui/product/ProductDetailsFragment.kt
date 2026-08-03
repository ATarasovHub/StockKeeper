package com.example.stockkeeper.ui.product

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.stockkeeper.R
import com.example.stockkeeper.StockKeeperApplication
import com.example.stockkeeper.data.local.model.ProductStockItem
import com.example.stockkeeper.data.photo.ProductPhotoStore
import com.example.stockkeeper.ui.common.bindDirectorySuggestions
import com.example.stockkeeper.ui.common.bindCustomerSuggestions
import com.example.stockkeeper.ui.history.HistoryAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class ProductDetailsFragment : Fragment() {
    private val productId: Long by lazy { requireArguments().getLong(ARG_PRODUCT_ID) }
    private val viewModel: ProductDetailsViewModel by viewModels {
        val app = requireActivity().application as StockKeeperApplication
        ProductDetailsViewModelFactory(productId, app.stockRepository)
    }
    private var currentProduct: ProductStockItem? = null
    private var pendingCameraPath: String? = null

    private val photoPicker = registerForActivityResult(PickVisualMedia()) { uri ->
        if (uri != null) savePhoto(uri)
    }
    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val path = pendingCameraPath
        pendingCameraPath = null
        if (saved && path != null) savePhotoPath(path) else ProductPhotoStore.delete(requireContext(), path)
    }
    private val cameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchCamera() else view?.let {
            Snackbar.make(it, R.string.camera_permission_denied, Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View =
        inflater.inflate(R.layout.fragment_product_details, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val historyAdapter = HistoryAdapter()
        val historyList = view.findViewById<RecyclerView>(R.id.detailHistoryList)
        val emptyHistory = view.findViewById<View>(R.id.detailEmptyHistory)
        historyList.layoutManager = LinearLayoutManager(requireContext())
        historyList.adapter = historyAdapter

        view.findViewById<View>(R.id.changePhotoButton).setOnClickListener {
            showPhotoSource()
        }
        view.findViewById<View>(R.id.editButton).setOnClickListener { currentProduct?.let(::showEditDialog) }
        view.findViewById<View>(R.id.archiveButton).setOnClickListener { confirmArchive() }
        view.findViewById<View>(R.id.receiveButton).setOnClickListener { showOperationDialog(0) }
        view.findViewById<View>(R.id.sellButton).setOnClickListener { showOperationDialog(1) }
        view.findViewById<View>(R.id.writeOffButton).setOnClickListener { showOperationDialog(2) }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.product.collect { product ->
                        currentProduct = product
                        product?.let { bindProduct(view, it) }
                    }
                }
                launch {
                    viewModel.history.collect { history ->
                        historyAdapter.submitList(history)
                        historyList.isVisible = history.isNotEmpty()
                        emptyHistory.isVisible = history.isEmpty()
                    }
                }
                launch {
                    viewModel.events.collect { event -> handleEvent(view, event) }
                }
            }
        }
    }

    private fun bindProduct(view: View, product: ProductStockItem) {
        view.findViewById<TextView>(R.id.detailName).text = product.name
        view.findViewById<TextView>(R.id.detailArticle).text = getString(R.string.article_format, product.article)
        view.findViewById<TextView>(R.id.detailQuantity).text = getString(R.string.quantity_format, product.quantity)
        view.findViewById<TextView>(R.id.detailManufacturer).text = getString(
            R.string.manufacturer_format,
            product.manufacturerName ?: getString(R.string.manufacturer_not_set),
        )
        view.findViewById<TextView>(R.id.detailLocation).text =
            if (!product.rack.isNullOrBlank() || !product.shelf.isNullOrBlank()) {
                getString(R.string.location_format, product.rack.orEmpty(), product.shelf.orEmpty())
            } else getString(R.string.location_not_set)
        view.findViewById<TextView>(R.id.detailNote).apply {
            isVisible = !product.note.isNullOrBlank()
            text = product.note
        }
        val photo = view.findViewById<ImageView>(R.id.productPhoto)
        val file = ProductPhotoStore.file(requireContext(), product.photoPath)
        if (file != null) {
            photo.setImageURI(file.toUri())
        } else {
            photo.setImageResource(R.drawable.ic_inventory)
        }
    }

    private fun savePhoto(uri: Uri) {
        val product = currentProduct ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching { ProductPhotoStore.copyIntoApp(requireContext(), uri) }
                .onSuccess { path ->
                    viewModel.update(
                        product.article,
                        product.name,
                        product.manufacturerName,
                        product.rack,
                        product.shelf,
                        product.note,
                        path,
                    )
                }
                .onFailure { view?.let { Snackbar.make(it, R.string.photo_failed, Snackbar.LENGTH_LONG).show() } }
        }
    }

    private fun savePhotoPath(path: String) {
        val product = currentProduct ?: return
        viewModel.update(
            product.article,
            product.name,
            product.manufacturerName,
            product.rack,
            product.shelf,
            product.note,
            path,
        )
    }

    private fun showPhotoSource() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.photo_source_title)
            .setItems(arrayOf(getString(R.string.take_photo), getString(R.string.choose_gallery))) { _, choice ->
                if (choice == 0) requestCamera() else photoPicker.launch(
                    PickVisualMediaRequest(PickVisualMedia.ImageOnly),
                )
            }
            .show()
    }

    private fun requestCamera() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCamera()
        } else {
            cameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchCamera() {
        val destination = ProductPhotoStore.createCameraDestination(requireContext())
        pendingCameraPath = destination.relativePath
        takePicture.launch(destination.uri)
    }

    private fun showEditDialog(product: ProductStockItem) {
        val content = layoutInflater.inflate(R.layout.dialog_add_product, null)
        content.findViewById<TextInputEditText>(R.id.articleInput).setText(product.article)
        content.findViewById<TextInputEditText>(R.id.nameInput).setText(product.name)
        content.findViewById<TextView>(R.id.manufacturerInput).text = product.manufacturerName
        content.findViewById<TextView>(R.id.rackInput).text = product.rack
        content.findViewById<TextView>(R.id.shelfInput).text = product.shelf
        content.findViewById<TextInputEditText>(R.id.noteInput).setText(product.note)
        content.findViewById<View>(R.id.addProductPhoto).isVisible = false
        content.findViewById<View>(R.id.addProductPhotoButton).isVisible = false
        content.findViewById<TextInputLayout>(R.id.quantityLayout).isVisible = false
        val suggestionsJob = bindDirectorySuggestions(
            content = content,
            manufacturerSuggestions = viewModel.manufacturerSuggestions,
            locations = viewModel.locations,
            onManufacturerQueryChanged = viewModel::searchManufacturers,
        )
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.edit)
            .setView(content)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.save, null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val article = content.text(R.id.articleInput)
                val name = content.text(R.id.nameInput)
                content.findViewById<TextInputLayout>(R.id.articleLayout).error =
                    if (article.isBlank()) getString(R.string.required_field) else null
                content.findViewById<TextInputLayout>(R.id.nameLayout).error =
                    if (name.isBlank()) getString(R.string.required_field) else null
                if (article.isBlank() || name.isBlank()) return@setOnClickListener
                viewModel.update(
                    article,
                    name,
                    content.text(R.id.manufacturerInput),
                    content.text(R.id.rackInput),
                    content.text(R.id.shelfInput),
                    content.text(R.id.noteInput),
                    product.photoPath,
                )
                dialog.dismiss()
            }
        }
        dialog.setOnDismissListener {
            suggestionsJob.cancel()
            viewModel.searchManufacturers("")
        }
        dialog.show()
    }

    private fun showOperationDialog(operation: Int) {
        val content = layoutInflater.inflate(R.layout.dialog_stock_operation, null)
        val customerLayout = content.findViewById<TextInputLayout>(R.id.customerLayout)
        val reasonLayout = content.findViewById<TextInputLayout>(R.id.reasonLayout)
        customerLayout.isVisible = operation == 1
        reasonLayout.isVisible = operation == 2
        val customerSuggestionsJob = if (operation == 1) {
            bindCustomerSuggestions(content, viewModel.customers)
        } else {
            null
        }
        val title = when (operation) {
            0 -> R.string.receive
            1 -> R.string.sell
            else -> R.string.write_off
        }
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setView(content)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.save, null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val quantity = content.text(R.id.operationQuantityInput).toIntOrNull()
                val customer = content.text(R.id.customerInput)
                val reason = content.text(R.id.reasonInput)
                content.findViewById<TextInputLayout>(R.id.operationQuantityLayout).error =
                    if (quantity == null || quantity <= 0) getString(R.string.invalid_quantity) else null
                customerLayout.error = if (operation == 1 && customer.isBlank()) getString(R.string.required_field) else null
                reasonLayout.error = if (operation == 2 && reason.isBlank()) getString(R.string.required_field) else null
                if (quantity == null || quantity <= 0 || (operation == 1 && customer.isBlank()) || (operation == 2 && reason.isBlank())) return@setOnClickListener
                when (operation) {
                    0 -> viewModel.receive(quantity)
                    1 -> viewModel.sell(quantity, customer)
                    2 -> viewModel.writeOff(quantity, reason)
                }
                dialog.dismiss()
            }
        }
        dialog.setOnDismissListener { customerSuggestionsJob?.cancel() }
        dialog.show()
    }

    private fun confirmArchive() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.archive_product_title)
            .setMessage(R.string.archive_product_message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.confirm) { _, _ -> viewModel.archive() }
            .show()
    }

    private fun handleEvent(anchor: View, event: ProductEvent) {
        when (event) {
            ProductEvent.Updated -> Snackbar.make(anchor, R.string.product_updated, Snackbar.LENGTH_SHORT).show()
            ProductEvent.OperationSaved -> Snackbar.make(anchor, R.string.operation_saved, Snackbar.LENGTH_SHORT).show()
            ProductEvent.Archived -> {
                Snackbar.make(anchor, R.string.product_archived, Snackbar.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
            is ProductEvent.Error -> Snackbar.make(
                anchor,
                getString(R.string.operation_failed, event.message),
                Snackbar.LENGTH_LONG,
            ).show()
        }
    }

    private fun View.text(id: Int): String =
        findViewById<TextView>(id).text?.toString()?.trim().orEmpty()

    companion object {
        private const val ARG_PRODUCT_ID = "product_id"

        fun newInstance(productId: Long) = ProductDetailsFragment().apply {
            arguments = Bundle().apply { putLong(ARG_PRODUCT_ID, productId) }
        }
    }
}
