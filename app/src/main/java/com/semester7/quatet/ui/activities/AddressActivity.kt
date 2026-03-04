package com.semester7.quatet.ui.activities

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.semester7.quatet.data.model.AccountAddressDTO
import com.semester7.quatet.data.model.AddressRequest
import com.semester7.quatet.databinding.ActivityAddressBinding
import com.semester7.quatet.databinding.DialogAddressFormBinding
import com.semester7.quatet.ui.adapters.AddressAdapter
import com.semester7.quatet.viewmodel.AddressViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class AddressActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_PICK_ADDRESS_MODE = "extra_pick_address_mode"
        const val RESULT_NAME = "result_name"
        const val RESULT_PHONE = "result_phone"
        const val RESULT_EMAIL = "result_email"
        const val RESULT_ADDRESS = "result_address"
        const val RESULT_LAT = "result_lat"
        const val RESULT_LNG = "result_lng"
    }

    private lateinit var binding: ActivityAddressBinding
    private lateinit var adapter: AddressAdapter
    private val viewModel: AddressViewModel by viewModels()
    private val fusedLocationClient by lazy { LocationServices.getFusedLocationProviderClient(this) }

    private var addressSuggestionJob: Job? = null
    private var activeDialogBinding: DialogAddressFormBinding? = null

    private var placesClient: PlacesClient? = null
    private var autocompleteSessionToken: AutocompleteSessionToken? = null
    private var selectedSuggestionAddress: String? = null
    private var selectedSuggestionLatLng: Pair<Double, Double>? = null
    private var suppressAddressWatcher = false
    private val isPickMode: Boolean by lazy {
        intent.getBooleanExtra(EXTRA_PICK_ADDRESS_MODE, false)
    }

    private val requestLocationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                fetchCurrentLocationAndFill(activeDialogBinding)
            } else {
                Toast.makeText(this, "Cần cấp quyền vị trí để lấy địa chỉ", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddressBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initPlacesSdk()
        setupRecyclerView()
        setupListeners()
        observeViewModel()
        setupModeUi()

        viewModel.fetchAddresses()
    }

    private fun setupModeUi() {
        if (!isPickMode) return
        binding.tvPickHint.visibility = View.VISIBLE
        binding.tvTitle.text = "Chọn địa chỉ giao hàng"
        binding.tvSubtitle.text = "Chọn nhanh để điền thông tin vào đơn hàng"
    }

    private fun initPlacesSdk() {
        val apiKey = getMapsApiKeyFromManifest()
        if (apiKey.isNullOrBlank()) {
            return
        }

        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, apiKey, Locale("vi", "VN"))
        }
        placesClient = Places.createClient(this)
    }

    private fun getMapsApiKeyFromManifest(): String? {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            appInfo.metaData?.getString("com.google.android.geo.API_KEY")
        } catch (_: Exception) {
            null
        }
    }

    private fun setupRecyclerView() {
        binding.rvAddresses.layoutManager = LinearLayoutManager(this)
        adapter = AddressAdapter(
            items = emptyList(),
            isPickMode = isPickMode,
            onEdit = { showAddressDialog(it) },
            onDelete = { showDeleteConfirm(it) },
            onSetDefault = { viewModel.setDefaultAddress(it.accountAddressId) },
            onSelect = { returnSelectedAddress(it) }
        )
        binding.rvAddresses.adapter = adapter
    }

    private fun returnSelectedAddress(address: AccountAddressDTO) {
        setResult(
            RESULT_OK,
            Intent().apply {
                putExtra(RESULT_NAME, address.customername ?: "")
                putExtra(RESULT_PHONE, address.customerphone ?: "")
                putExtra(RESULT_EMAIL, address.customeremail ?: "")
                putExtra(RESULT_ADDRESS, address.addressLine)
                putExtra(RESULT_LAT, address.latitude)
                putExtra(RESULT_LNG, address.longitude)
            }
        )
        finish()
    }

    private fun setupListeners() {
        binding.ivBack.setOnClickListener { finish() }
        binding.btnAddAddress.setOnClickListener { showAddressDialog(null) }
    }

    private fun observeViewModel() {
        viewModel.addresses.observe(this) { list ->
            val addresses = list ?: emptyList()
            adapter.updateData(addresses)

            val isEmpty = addresses.isEmpty()
            binding.layoutEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
            binding.rvAddresses.visibility = if (isEmpty) View.GONE else View.VISIBLE
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnAddAddress.isEnabled = !isLoading
        }

        viewModel.errorMessage.observe(this) { message ->
            if (!message.isNullOrEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                viewModel.clearMessages()
            }
        }

        viewModel.successMessage.observe(this) { message ->
            if (!message.isNullOrEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                viewModel.clearMessages()
            }
        }
    }

    private fun showDeleteConfirm(address: AccountAddressDTO) {
        AlertDialog.Builder(this)
            .setTitle("Xóa địa chỉ")
            .setMessage("Bạn có chắc chắn muốn xóa địa chỉ '${address.label}'?")
            .setPositiveButton("Xóa") { _, _ ->
                viewModel.deleteAddress(address.accountAddressId)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun showAddressDialog(address: AccountAddressDTO?) {
        val dialogBinding = DialogAddressFormBinding.inflate(LayoutInflater.from(this))
        activeDialogBinding = dialogBinding
        selectedSuggestionAddress = null
        selectedSuggestionLatLng = null

        if (address != null) {
            dialogBinding.edtLabel.setText(address.label)
            dialogBinding.edtCustomerName.setText(address.customername ?: "")
            dialogBinding.edtCustomerPhone.setText(address.customerphone ?: "")
            dialogBinding.edtCustomerEmail.setText(address.customeremail ?: "")
            dialogBinding.edtAddressLine.setText(address.addressLine)
            if (address.latitude != 0.0) dialogBinding.edtLatitude.setText(address.latitude.toString())
            if (address.longitude != 0.0) dialogBinding.edtLongitude.setText(address.longitude.toString())
            dialogBinding.switchDefault.isChecked = address.isDefault
            dialogBinding.switchActive.isChecked = address.isActive

            if (address.latitude != 0.0 || address.longitude != 0.0) {
                selectedSuggestionAddress = address.addressLine
                selectedSuggestionLatLng = Pair(address.latitude, address.longitude)
            }
        }

        setupAddressAutocomplete(dialogBinding)

        dialogBinding.btnUseCurrentLocation.setOnClickListener {
            requestAndUseCurrentLocation(dialogBinding)
        }

        dialogBinding.btnOpenGoogleMaps.setOnClickListener {
            openGoogleMaps(dialogBinding)
        }

        AlertDialog.Builder(this)
            .setTitle(if (address == null) "Thêm địa chỉ mới" else "Cập nhật địa chỉ")
            .setView(dialogBinding.root)
            .setNegativeButton("Hủy", null)
            .setPositiveButton(if (address == null) "Lưu" else "Cập nhật", null)
            .create()
            .also { dialog ->
                dialog.setOnShowListener {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val label = dialogBinding.edtLabel.text.toString().trim()
                        val customerName = dialogBinding.edtCustomerName.text.toString().trim().ifEmpty { null }
                        val customerPhone = dialogBinding.edtCustomerPhone.text.toString().trim().ifEmpty { null }
                        val customerEmail = dialogBinding.edtCustomerEmail.text.toString().trim().ifEmpty { null }
                        val addressLine = dialogBinding.edtAddressLine.text.toString().trim()
                        val isDefault = dialogBinding.switchDefault.isChecked
                        val isActive = dialogBinding.switchActive.isChecked

                        if (label.isEmpty()) {
                            dialogBinding.edtLabel.error = "Vui lòng nhập nhãn địa chỉ"
                            return@setOnClickListener
                        }

                        if (addressLine.isEmpty()) {
                            dialogBinding.edtAddressLine.error = "Vui lòng nhập địa chỉ"
                            return@setOnClickListener
                        }

                        val cachedLatLng = selectedSuggestionLatLng
                        if (selectedSuggestionAddress == addressLine && cachedLatLng != null) {
                            submitAddress(
                                address = address,
                                label = label,
                                customerName = customerName,
                                customerPhone = customerPhone,
                                customerEmail = customerEmail,
                                addressLine = addressLine,
                                lat = cachedLatLng.first,
                                lng = cachedLatLng.second,
                                isDefault = isDefault,
                                isActive = isActive
                            )
                            dialog.dismiss()
                            return@setOnClickListener
                        }

                        geocodeAddressToCoordinates(
                            addressLine,
                            onSuccess = { lat, lng ->
                                submitAddress(
                                    address = address,
                                    label = label,
                                    customerName = customerName,
                                    customerPhone = customerPhone,
                                    customerEmail = customerEmail,
                                    addressLine = addressLine,
                                    lat = lat,
                                    lng = lng,
                                    isDefault = isDefault,
                                    isActive = isActive
                                )
                                dialog.dismiss()
                            },
                            onError = {
                                Toast.makeText(
                                    this,
                                    "Không thể lấy tọa độ từ địa chỉ. Vui lòng kiểm tra lại địa chỉ.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        )
                    }
                }
                dialog.setOnDismissListener {
                    activeDialogBinding = null
                    addressSuggestionJob?.cancel()
                }
                dialog.show()
            }
    }

    private fun submitAddress(
        address: AccountAddressDTO?,
        label: String,
        customerName: String?,
        customerPhone: String?,
        customerEmail: String?,
        addressLine: String,
        lat: Double,
        lng: Double,
        isDefault: Boolean,
        isActive: Boolean
    ) {
        val request = AddressRequest(
            label = label,
            customername = customerName,
            customerphone = customerPhone,
            customeremail = customerEmail,
            customerName = customerName,
            customerPhone = customerPhone,
            customerEmail = customerEmail,
            addressLine = addressLine,
            latitude = lat,
            longitude = lng,
            isDefault = isDefault,
            isActive = isActive
        )

        if (address == null) {
            viewModel.createAddress(request)
        } else {
            viewModel.updateAddress(address.accountAddressId, request)
        }
    }

    private fun setupAddressAutocomplete(dialogBinding: DialogAddressFormBinding) {
        val suggestions = mutableListOf<String>()
        val suggestionAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            suggestions
        )
        val placeIdByText = mutableMapOf<String, String>()
        autocompleteSessionToken = AutocompleteSessionToken.newInstance()

        dialogBinding.edtAddressLine.setAdapter(suggestionAdapter)
        dialogBinding.edtAddressLine.setOnItemClickListener { _, _, position, _ ->
            val selectedText = suggestionAdapter.getItem(position) ?: return@setOnItemClickListener
            val placeId = placeIdByText[selectedText]

            if (placeId == null) {
                selectedSuggestionAddress = selectedText
                return@setOnItemClickListener
            }

            fetchPlaceDetail(placeId,
                onSuccess = { resolvedAddress, lat, lng ->
                    selectedSuggestionAddress = resolvedAddress
                    selectedSuggestionLatLng = Pair(lat, lng)
                    suppressAddressWatcher = true
                    dialogBinding.edtAddressLine.setText(resolvedAddress)
                    dialogBinding.edtAddressLine.setSelection(resolvedAddress.length)
                    suppressAddressWatcher = false
                    dialogBinding.edtLatitude.setText(lat.toString())
                    dialogBinding.edtLongitude.setText(lng.toString())
                },
                onError = {
                    selectedSuggestionAddress = selectedText
                }
            )
        }

        dialogBinding.edtAddressLine.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun afterTextChanged(s: Editable?) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (suppressAddressWatcher) return

                val keyword = s?.toString()?.trim().orEmpty()
                selectedSuggestionAddress = null
                selectedSuggestionLatLng = null

                if (keyword.length < 2) return

                addressSuggestionJob?.cancel()
                addressSuggestionJob = lifecycleScope.launch {
                    delay(300)
                    fetchAddressSuggestions(keyword) { results ->
                        suggestions.clear()
                        placeIdByText.clear()
                        results.forEach {
                            suggestions.add(it.first)
                            val placeId = it.second
                            if (!placeId.isNullOrBlank()) {
                                placeIdByText[it.first] = placeId
                            }
                        }
                        suggestionAdapter.notifyDataSetChanged()
                        if (results.isNotEmpty() && dialogBinding.edtAddressLine.hasFocus()) {
                            dialogBinding.edtAddressLine.showDropDown()
                        }
                    }
                }
            }
        })
    }

    private fun fetchAddressSuggestions(
        keyword: String,
        onResult: (List<Pair<String, String?>>) -> Unit
    ) {
        val client = placesClient
        val token = autocompleteSessionToken

        if (client != null && token != null) {
            val request = FindAutocompletePredictionsRequest.builder()
                .setSessionToken(token)
                .setQuery(keyword)
                .setCountries(listOf("VN"))
                .build()

            client.findAutocompletePredictions(request)
                .addOnSuccessListener { response ->
                    val results = response.autocompletePredictions.map { prediction ->
                        prediction.getFullText(null).toString() to prediction.placeId
                    }.distinctBy { it.first }
                    onResult(results)
                }
                .addOnFailureListener {
                    fetchAddressSuggestionsByGeocoder(keyword, onResult)
                }
            return
        }

        fetchAddressSuggestionsByGeocoder(keyword, onResult)
    }

    private fun fetchAddressSuggestionsByGeocoder(
        keyword: String,
        onResult: (List<Pair<String, String?>>) -> Unit
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(this@AddressActivity, Locale("vi", "VN"))
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocationName(keyword, 5)

                val formatted = addresses
                    ?.mapNotNull { item ->
                        item.getAddressLine(0)
                            ?: listOfNotNull(
                                item.featureName,
                                item.subAdminArea,
                                item.adminArea
                            ).joinToString(", ").ifBlank { null }
                    }
                    ?.distinct()
                    ?.map { it to null }
                    ?: emptyList()

                withContext(Dispatchers.Main) {
                    onResult(formatted)
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(emptyList())
                }
            }
        }
    }

    private fun fetchPlaceDetail(
        placeId: String,
        onSuccess: (String, Double, Double) -> Unit,
        onError: () -> Unit
    ) {
        val client = placesClient ?: run {
            onError()
            return
        }

        val fields = listOf(Place.Field.ADDRESS, Place.Field.LAT_LNG)
        val request = FetchPlaceRequest.builder(placeId, fields).build()

        client.fetchPlace(request)
            .addOnSuccessListener { response ->
                val place = response.place
                val address = place.address
                val latLng = place.latLng
                if (!address.isNullOrBlank() && latLng != null) {
                    onSuccess(address, latLng.latitude, latLng.longitude)
                } else {
                    onError()
                }
            }
            .addOnFailureListener {
                onError()
            }
    }

    private fun requestAndUseCurrentLocation(dialogBinding: DialogAddressFormBinding) {
        activeDialogBinding = dialogBinding
        if (isLocationPermissionGranted()) {
            fetchCurrentLocationAndFill(dialogBinding)
        } else {
            requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun isLocationPermissionGranted(): Boolean {
        val fine = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarse = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fine || coarse
    }

    private fun fetchCurrentLocationAndFill(dialogBinding: DialogAddressFormBinding?) {
        val safeBinding = dialogBinding ?: return

        if (!isLocationPermissionGranted()) return

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    applyLocationToForm(safeBinding, location.latitude, location.longitude)
                } else {
                    val tokenSource = CancellationTokenSource()
                    fusedLocationClient.getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        tokenSource.token
                    ).addOnSuccessListener { currentLocation ->
                        if (currentLocation != null) {
                            applyLocationToForm(
                                safeBinding,
                                currentLocation.latitude,
                                currentLocation.longitude
                            )
                        } else {
                            Toast.makeText(this, "Không lấy được vị trí hiện tại", Toast.LENGTH_SHORT).show()
                        }
                    }.addOnFailureListener {
                        Toast.makeText(this, "Lỗi lấy vị trí: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Lỗi lấy vị trí: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun applyLocationToForm(dialogBinding: DialogAddressFormBinding, lat: Double, lng: Double) {
        dialogBinding.edtLatitude.setText(lat.toString())
        dialogBinding.edtLongitude.setText(lng.toString())

        reverseGeocodeCoordinates(
            lat,
            lng,
            onSuccess = { resolvedAddress ->
                selectedSuggestionAddress = resolvedAddress
                selectedSuggestionLatLng = Pair(lat, lng)
                suppressAddressWatcher = true
                dialogBinding.edtAddressLine.setText(resolvedAddress)
                suppressAddressWatcher = false
                Toast.makeText(this, "Đã cập nhật địa chỉ từ vị trí", Toast.LENGTH_SHORT).show()
            },
            onError = {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun geocodeAddressToCoordinates(
        address: String,
        onSuccess: (Double, Double) -> Unit,
        onError: (String) -> Unit
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(this@AddressActivity, Locale("vi", "VN"))
                @Suppress("DEPRECATION")
                val results = geocoder.getFromLocationName(address, 1)
                val first = results?.firstOrNull()

                withContext(Dispatchers.Main) {
                    if (first != null) {
                        onSuccess(first.latitude, first.longitude)
                    } else {
                        onError("Không tìm thấy tọa độ cho địa chỉ này")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(e.message ?: "Lỗi geocode địa chỉ")
                }
            }
        }
    }

    private fun reverseGeocodeCoordinates(
        latitude: Double,
        longitude: Double,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(this@AddressActivity, Locale("vi", "VN"))
                @Suppress("DEPRECATION")
                val results = geocoder.getFromLocation(latitude, longitude, 1)
                val first = results?.firstOrNull()

                withContext(Dispatchers.Main) {
                    if (first != null) {
                        val fullAddress = first.getAddressLine(0)
                            ?: listOfNotNull(
                                first.thoroughfare,
                                first.subAdminArea,
                                first.adminArea,
                                first.countryName
                            ).joinToString(", ")
                        onSuccess(fullAddress)
                    } else {
                        onError("Không tìm thấy địa chỉ từ tọa độ")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(e.message ?: "Lỗi reverse geocode")
                }
            }
        }
    }

    private fun openGoogleMaps(dialogBinding: DialogAddressFormBinding) {
        val lat = dialogBinding.edtLatitude.text.toString().trim().toDoubleOrNull()
        val lng = dialogBinding.edtLongitude.text.toString().trim().toDoubleOrNull()
        val addressLine = dialogBinding.edtAddressLine.text.toString().trim()

        val uri = when {
            lat != null && lng != null -> Uri.parse("geo:$lat,$lng?q=$lat,$lng")
            addressLine.isNotBlank() -> Uri.parse("geo:0,0?q=${Uri.encode(addressLine)}")
            else -> {
                Toast.makeText(this, "Nhập địa chỉ hoặc lat/lng trước", Toast.LENGTH_SHORT).show()
                return
            }
        }

        val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
        }

        if (mapIntent.resolveActivity(packageManager) != null) {
            startActivity(mapIntent)
        } else {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
    }
}
