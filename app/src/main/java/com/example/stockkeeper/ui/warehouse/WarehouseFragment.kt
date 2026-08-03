package com.example.stockkeeper.ui.warehouse

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.ImageView
import android.widget.ArrayAdapter
import android.widget.TextView
import android.view.inputmethod.EditorInfo
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.stockkeeper.R
import com.example.stockkeeper.MainActivity
import com.example.stockkeeper.StockKeeperApplication
import com.example.stockkeeper.ui.common.StockViewModelFactory
import com.example.stockkeeper.ui.common.bindDirectorySuggestions
import com.example.stockkeeper.ui.common.bindCustomerSuggestions
import com.example.stockkeeper.data.local.model.ProductStockItem
import com.example.stockkeeper.data.photo.ProductPhotoStore
import com.example.stockkeeper.search.SearchHistoryStore
import com.example.stockkeeper.settings.AppSettings
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import kotlinx.coroutines.launch

class WarehouseFragment : Fragment() {
    private var addDialogContent: View? = null
    private var selectedPhotoPath: String? = null
    private var pendingCameraPath: String? = null

    private val photoPicker = registerForActivityResult(PickVisualMedia()) { uri ->
        if (uri == null) return@registerForActivityResult
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching { ProductPhotoStore.copyIntoApp(requireContext(), uri) }
                .onSuccess { path ->
                    ProductPhotoStore.delete(requireContext(), selectedPhotoPath)
                    selectedPhotoPath = path
                    ProductPhotoStore.file(requireContext(), path)?.let { file ->
                        addDialogContent?.findViewById<ImageView>(R.id.addProductPhoto)?.setImageURI(file.toUri())
                    }
                }
                .onFailure {
                    view?.let { anchor -> Snackbar.make(anchor, R.string.photo_failed, Snackbar.LENGTH_LONG).show() }
                }
        }
    }
    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val path = pendingCameraPath
        pendingCameraPath = null
        if (saved && path != null) {
            ProductPhotoStore.delete(requireContext(), selectedPhotoPath)
            selectedPhotoPath = path
            ProductPhotoStore.file(requireContext(), path)?.let { file ->
                addDialogContent?.findViewById<ImageView>(R.id.addProductPhoto)?.setImageURI(file.toUri())
            }
        } else {
            ProductPhotoStore.delete(requireContext(), path)
        }
    }
    private val cameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchCamera() else view?.let {
            Snackbar.make(it, R.string.camera_permission_denied, Snackbar.LENGTH_LONG).show()
        }
    }
    private val viewModel: WarehouseViewModel by viewModels {
        val app = requireActivity().application as StockKeeperApplication
        StockViewModelFactory(app.stockRepository)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View =
        inflater.inflate(R.layout.fragment_warehouse, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val showManufacturer = AppSettings.showManufacturerFilter(requireContext())
        val showAvailability = AppSettings.showAvailabilityFilter(requireContext())
        val showRack = AppSettings.showRackFilter(requireContext())
        val showShelf = AppSettings.showShelfFilter(requireContext())
        view.findViewById<View>(R.id.manufacturerFilterLayout).isVisible = showManufacturer
        view.findViewById<View>(R.id.availabilityFilterLayout).isVisible = showAvailability
        view.findViewById<View>(R.id.rackFilterLayout).isVisible = showRack
        view.findViewById<View>(R.id.shelfFilterLayout).isVisible = showShelf
        view.findViewById<View>(R.id.topFilterRow).isVisible = showManufacturer || showAvailability
        view.findViewById<View>(R.id.bottomFilterRow).isVisible = showRack || showShelf

        val adapter = ProductAdapter { product ->
            (requireActivity() as MainActivity).openProductDetails(product.id)
        }
        val historyStore = SearchHistoryStore(requireContext())
        var history = historyStore.get()
        var candidates = emptyList<String>()
        val suggestionAdapter = SearchSuggestionAdapter(requireContext())
        val list = view.findViewById<RecyclerView>(R.id.productList)
        val empty = view.findViewById<View>(R.id.emptyState)
        list.layoutManager = LinearLayoutManager(requireContext())
        list.adapter = adapter

        val searchInput = view.findViewById<MaterialAutoCompleteTextView>(R.id.searchInput)
        searchInput.setAdapter(suggestionAdapter)
        fun refreshSuggestions(value: String, openMenu: Boolean = true) {
            val cleanValue = value.trim()
            val suggestions = if (cleanValue.isEmpty()) {
                history
            } else {
                candidates
                    .filter { it.contains(cleanValue, ignoreCase = true) }
                    .sortedBy { if (it.startsWith(cleanValue, ignoreCase = true)) 0 else 1 }
                    .take(SearchHistoryStore.MAX_ITEMS)
            }
            suggestionAdapter.submit(
                suggestions.map { SearchSuggestion(label = it, isRecent = cleanValue.isEmpty()) },
            )
            if (openMenu && searchInput.hasFocus() && suggestions.isNotEmpty()) searchInput.showDropDown()
        }
        searchInput.addTextChangedListener(
            object : TextWatcher {
                override fun beforeTextChanged(value: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(value: CharSequence?, start: Int, before: Int, count: Int) {
                    val query = value?.toString().orEmpty()
                    viewModel.search(query)
                    refreshSuggestions(query)
                }
                override fun afterTextChanged(value: Editable?) = Unit
            },
        )
        searchInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                refreshSuggestions(searchInput.text?.toString().orEmpty())
            } else {
                history = historyStore.add(searchInput.text?.toString().orEmpty())
            }
        }
        searchInput.setOnItemClickListener { _, _, _, _ ->
            val selected = searchInput.text?.toString().orEmpty()
            history = historyStore.add(selected)
            viewModel.search(selected)
        }
        searchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                history = historyStore.add(searchInput.text?.toString().orEmpty())
                searchInput.dismissDropDown()
                searchInput.clearFocus()
                true
            } else false
        }

        val manufacturerFilter = view.findViewById<MaterialAutoCompleteTextView>(R.id.manufacturerFilter)
        manufacturerFilter.setText(getString(R.string.all_manufacturers), false)
        manufacturerFilter.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val value = s?.toString().orEmpty()
                viewModel.searchManufacturers(
                    if (value == getString(R.string.all_manufacturers)) "" else value,
                )
                viewModel.filterManufacturer(null)
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
        manufacturerFilter.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) manufacturerFilter.showDropDown()
        }

        val availabilityFilter = view.findViewById<MaterialAutoCompleteTextView>(R.id.availabilityFilter)
        val availabilityLabels = listOf(
            getString(R.string.all_availability),
            getString(R.string.in_stock),
            getString(R.string.out_of_stock),
        )
        availabilityFilter.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, availabilityLabels),
        )
        availabilityFilter.setText(availabilityLabels.first(), false)
        availabilityFilter.setOnItemClickListener { _, _, position, _ ->
            viewModel.filterAvailability(position)
        }

        val rackFilter = view.findViewById<MaterialAutoCompleteTextView>(R.id.rackFilter)
        val shelfFilter = view.findViewById<MaterialAutoCompleteTextView>(R.id.shelfFilter)
        rackFilter.setText(getString(R.string.all_racks), false)
        shelfFilter.setText(getString(R.string.all_shelves), false)
        view.findViewById<ExtendedFloatingActionButton>(R.id.addProductButton)
            .setOnClickListener { showAddProductDialog(view) }
        view.findViewById<View>(R.id.openArchiveButton).setOnClickListener {
            (requireActivity() as MainActivity).openArchive()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.products.collect { products ->
                        adapter.submitList(products)
                        empty.isVisible = products.isEmpty()
                        list.isVisible = products.isNotEmpty()
                    }
                }
                launch {
                    viewModel.allProducts.collect { products ->
                        candidates = products
                            .flatMap { listOf(it.article, it.name, it.manufacturerName.orEmpty()) }
                            .filter(String::isNotBlank)
                            .distinctBy { it.lowercase() }
                        refreshSuggestions(searchInput.text?.toString().orEmpty(), openMenu = false)
                    }
                }
                launch {
                    viewModel.manufacturerSuggestions.collect { manufacturers ->
                        val labels = listOf(getString(R.string.all_manufacturers)) + manufacturers.map { it.name }
                        val visibleRows = labels.size.coerceIn(1, 10)
                        manufacturerFilter.dropDownHeight =
                            (48 * resources.displayMetrics.density * visibleRows).toInt()
                        manufacturerFilter.setAdapter(
                            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, labels),
                        )
                        manufacturerFilter.setOnItemClickListener { _, _, position, _ ->
                            viewModel.filterManufacturer(manufacturers.getOrNull(position - 1)?.id)
                        }
                    }
                }
                launch {
                    viewModel.locations.collect { locations ->
                        val racks = locations.map { it.rack }.filter(String::isNotBlank)
                            .distinct().sortedBy { it.lowercase() }
                        val shelves = locations.map { it.shelf }.filter(String::isNotBlank)
                            .distinct().sortedBy { it.lowercase() }
                        val rackLabels = listOf(getString(R.string.all_racks)) + racks
                        val shelfLabels = listOf(getString(R.string.all_shelves)) + shelves
                        rackFilter.setAdapter(
                            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, rackLabels),
                        )
                        shelfFilter.setAdapter(
                            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, shelfLabels),
                        )
                        rackFilter.setOnItemClickListener { _, _, position, _ ->
                            viewModel.filterRack(racks.getOrNull(position - 1).orEmpty())
                        }
                        shelfFilter.setOnItemClickListener { _, _, position, _ ->
                            viewModel.filterShelf(shelves.getOrNull(position - 1).orEmpty())
                        }
                    }
                }
                launch {
                    viewModel.messages.collect { result ->
                        result.fold(
                            onSuccess = { Snackbar.make(view, R.string.operation_saved, Snackbar.LENGTH_SHORT).show() },
                            onFailure = { error ->
                                Snackbar.make(
                                    view,
                                    getString(R.string.product_add_failed, error.message.orEmpty()),
                                    Snackbar.LENGTH_LONG,
                                ).show()
                            },
                        )
                    }
                }
            }
        }
    }

    private fun showProductActions(product: ProductStockItem) {
        val actions = arrayOf(
            getString(R.string.receive),
            getString(R.string.sell),
            getString(R.string.write_off),
        )
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("${product.name} · ${getString(R.string.quantity_format, product.quantity)}")
            .setItems(actions) { _, index -> showOperationDialog(product, index) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showOperationDialog(product: ProductStockItem, operation: Int) {
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
                val quantityLayout = content.findViewById<TextInputLayout>(R.id.operationQuantityLayout)
                val quantity = content.text(R.id.operationQuantityInput).toIntOrNull()
                val customer = content.text(R.id.customerInput)
                val reason = content.text(R.id.reasonInput)
                quantityLayout.error = if (quantity == null || quantity <= 0) getString(R.string.invalid_quantity) else null
                customerLayout.error = if (operation == 1 && customer.isBlank()) getString(R.string.required_field) else null
                reasonLayout.error = if (operation == 2 && reason.isBlank()) getString(R.string.required_field) else null
                if (quantity == null || quantity <= 0 || operation == 1 && customer.isBlank() || operation == 2 && reason.isBlank()) {
                    return@setOnClickListener
                }
                when (operation) {
                    0 -> viewModel.receive(product.id, quantity)
                    1 -> viewModel.sell(product.id, quantity, customer, null)
                    2 -> viewModel.writeOff(product.id, quantity, reason)
                }
                dialog.dismiss()
            }
        }
        dialog.setOnDismissListener { customerSuggestionsJob?.cancel() }
        dialog.show()
    }

    private fun showAddProductDialog(anchor: View) {
        val content = layoutInflater.inflate(R.layout.dialog_add_product, null)
        addDialogContent = content
        selectedPhotoPath = null
        var photoCommitted = false
        content.findViewById<View>(R.id.addProductPhotoButton).setOnClickListener {
            showPhotoSource()
        }
        val suggestionsJob = bindDirectorySuggestions(
            content = content,
            manufacturerSuggestions = viewModel.productFormManufacturerSuggestions,
            locations = viewModel.locations,
            onManufacturerQueryChanged = viewModel::searchProductFormManufacturers,
        )
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.add_product)
            .setView(content)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.save, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val articleLayout = content.findViewById<TextInputLayout>(R.id.articleLayout)
                val nameLayout = content.findViewById<TextInputLayout>(R.id.nameLayout)
                val quantityLayout = content.findViewById<TextInputLayout>(R.id.quantityLayout)
                val article = content.text(R.id.articleInput)
                val name = content.text(R.id.nameInput)
                val quantity = content.text(R.id.quantityInput).toIntOrNull()

                articleLayout.error = if (article.isBlank()) getString(R.string.required_field) else null
                nameLayout.error = if (name.isBlank()) getString(R.string.required_field) else null
                quantityLayout.error = if (quantity == null || quantity < 0) getString(R.string.invalid_quantity) else null
                if (article.isBlank() || name.isBlank() || quantity == null || quantity < 0) return@setOnClickListener

                viewModel.addProduct(
                    article = article,
                    name = name,
                    photoPath = selectedPhotoPath,
                    manufacturer = content.text(R.id.manufacturerInput),
                    rack = content.text(R.id.rackInput),
                    shelf = content.text(R.id.shelfInput),
                    note = content.text(R.id.noteInput),
                    initialQuantity = quantity,
                )
                photoCommitted = true
                dialog.dismiss()
            }
        }
        dialog.setOnDismissListener {
            suggestionsJob.cancel()
            viewModel.searchProductFormManufacturers("")
            if (!photoCommitted) ProductPhotoStore.delete(requireContext(), selectedPhotoPath)
            addDialogContent = null
            selectedPhotoPath = null
        }
        dialog.show()
    }

    private fun View.text(id: Int): String =
        findViewById<TextView>(id).text?.toString()?.trim().orEmpty()

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
}
