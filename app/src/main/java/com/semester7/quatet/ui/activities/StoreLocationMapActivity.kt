package com.semester7.quatet.ui.activities

import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.TypeFilter
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.semester7.quatet.data.model.StoreLocationDTO
import com.semester7.quatet.databinding.ActivityStoreLocationMapBinding
import com.semester7.quatet.viewmodel.StoreLocationViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.util.Locale

class StoreLocationMapActivity : AppCompatActivity() {

    private data class OriginSuggestion(
        val address: String,
        val placeId: String? = null,
        val lat: Double? = null,
        val lng: Double? = null
    )

    companion object {
        private const val HCMC_LAT = 10.7769
        private const val HCMC_LNG = 106.7009
        private const val CITY_ZOOM = 11.8
        private const val STORE_ZOOM = 16.0
    }

    private lateinit var binding: ActivityStoreLocationMapBinding
    private val viewModel: StoreLocationViewModel by viewModels()

    private var map: MapView? = null
    private var selectedStore: StoreLocationDTO? = null
    private var selectedOriginLat: Double? = null
    private var selectedOriginLng: Double? = null
    private var geocodedOriginKey: String? = null

    private val allStores = mutableListOf<StoreLocationDTO>()
    private lateinit var storeSpinnerAdapter: ArrayAdapter<String>
    private var ignoreStoreSpinnerEvent = false

    private var placesClient: PlacesClient? = null
    private var autocompleteSessionToken: AutocompleteSessionToken? = null

    private var originSuggestionJob: Job? = null
    private var suppressOriginWatcher = false
    private val originSuggestions = mutableListOf<String>()
    private val originSuggestionByText = mutableMapOf<String, OriginSuggestion>()

    private val travelModes = listOf(
        "Lái xe" to "driving",
        "Đi bộ" to "walking",
        "Xe đạp" to "bicycling",
        "Phương tiện công cộng" to "transit"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStoreLocationMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initPlacesSdk()
        BottomTabNavigator.setup(this, BottomTabNavigator.Tab.STORE_LOCATION)
        setupMap()
        setupUi()
        observeViewModel()
        viewModel.fetchLocations()
    }

    private fun initPlacesSdk() {
        val apiKey = getMapsApiKeyFromManifest()
        if (apiKey.isNullOrBlank()) return

        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, apiKey, Locale("vi", "VN"))
        }
        placesClient = Places.createClient(this)
        autocompleteSessionToken = AutocompleteSessionToken.newInstance()
    }

    private fun getMapsApiKeyFromManifest(): String? {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            appInfo.metaData?.getString("com.google.android.geo.API_KEY")
        } catch (_: Exception) {
            null
        }
    }

    private fun setupMap() {
        Configuration.getInstance().userAgentValue = packageName
        map = binding.storeMap.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(CITY_ZOOM)
            controller.setCenter(GeoPoint(HCMC_LAT, HCMC_LNG))
            minZoomLevel = 4.0
            maxZoomLevel = 19.0
        }
    }

    private fun setupUi() {
        val travelModeAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            travelModes.map { it.first }
        )
        travelModeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerTravelMode.adapter = travelModeAdapter

        storeSpinnerAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            mutableListOf("Chọn cửa hàng")
        )
        storeSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerStore.adapter = storeSpinnerAdapter
        binding.spinnerStore.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (ignoreStoreSpinnerEvent) return
                val storeIndex = position - 1
                if (storeIndex !in allStores.indices) return
                updateSelectedStore(allStores[storeIndex], animateToMap = true, syncDropdown = false)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        setupOriginAddressSuggestion()

        binding.btnNavigate.setOnClickListener {
            val store = selectedStore
            if (store == null) {
                Toast.makeText(this, "Vui lòng chọn cửa hàng.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val originAddress = binding.edtOriginAddress.text.toString().trim()
            if (originAddress.isBlank()) {
                Toast.makeText(this, "Vui lòng nhập địa chỉ xuất phát.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedMode = travelModes[binding.spinnerTravelMode.selectedItemPosition].second
            requestDirectionsWithOrigin(store, originAddress, selectedMode)
        }
    }

    private fun setupOriginAddressSuggestion() {
        val suggestionAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            originSuggestions
        )
        binding.edtOriginAddress.setAdapter(suggestionAdapter)

        binding.edtOriginAddress.setOnItemClickListener { _, _, position, _ ->
            val selectedText = suggestionAdapter.getItem(position) ?: return@setOnItemClickListener
            val selectedSuggestion = originSuggestionByText[selectedText] ?: return@setOnItemClickListener

            if (selectedSuggestion.placeId != null) {
                fetchPlaceDetail(
                    placeId = selectedSuggestion.placeId,
                    onSuccess = { resolvedAddress, lat, lng ->
                        selectedOriginLat = lat
                        selectedOriginLng = lng
                        geocodedOriginKey = normalizeAddress(resolvedAddress)

                        suppressOriginWatcher = true
                        binding.edtOriginAddress.setText(resolvedAddress)
                        binding.edtOriginAddress.setSelection(resolvedAddress.length)
                        suppressOriginWatcher = false
                    },
                    onError = {
                        selectedOriginLat = null
                        selectedOriginLng = null
                        geocodedOriginKey = null
                    }
                )
            } else if (selectedSuggestion.lat != null && selectedSuggestion.lng != null) {
                selectedOriginLat = selectedSuggestion.lat
                selectedOriginLng = selectedSuggestion.lng
                geocodedOriginKey = normalizeAddress(selectedSuggestion.address)
            }
        }

        binding.edtOriginAddress.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun afterTextChanged(s: Editable?) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (suppressOriginWatcher) return

                val keyword = s?.toString()?.trim().orEmpty()
                selectedOriginLat = null
                selectedOriginLng = null
                geocodedOriginKey = null

                if (keyword.length < 2) {
                    originSuggestions.clear()
                    originSuggestionByText.clear()
                    suggestionAdapter.notifyDataSetChanged()
                    return
                }

                originSuggestionJob?.cancel()
                originSuggestionJob = lifecycleScope.launch {
                    delay(250)
                    fetchOriginSuggestions(keyword) { suggestions ->
                        originSuggestions.clear()
                        originSuggestionByText.clear()
                        suggestions.forEach { item ->
                            originSuggestions.add(item.address)
                            originSuggestionByText[item.address] = item
                        }
                        suggestionAdapter.notifyDataSetChanged()

                        if (suggestions.isNotEmpty() && binding.edtOriginAddress.hasFocus()) {
                            binding.edtOriginAddress.showDropDown()
                        }
                    }
                }
            }
        })
    }

    private fun fetchOriginSuggestions(
        keyword: String,
        onResult: (List<OriginSuggestion>) -> Unit
    ) {
        val client = placesClient
        val token = autocompleteSessionToken

        if (client != null && token != null) {
            val request = FindAutocompletePredictionsRequest.builder()
                .setSessionToken(token)
                .setTypeFilter(TypeFilter.ADDRESS)
                .setCountries(listOf("VN"))
                .setQuery(keyword)
                .build()

            client.findAutocompletePredictions(request)
                .addOnSuccessListener { response ->
                    val suggestions = response.autocompletePredictions
                        .map { prediction ->
                            OriginSuggestion(
                                address = prediction.getFullText(null).toString(),
                                placeId = prediction.placeId
                            )
                        }
                        .distinctBy { it.address }
                    onResult(suggestions)
                }
                .addOnFailureListener {
                    fetchOriginSuggestionsByGeocoder(keyword, onResult)
                }
            return
        }

        fetchOriginSuggestionsByGeocoder(keyword, onResult)
    }

    private fun fetchOriginSuggestionsByGeocoder(
        keyword: String,
        onResult: (List<OriginSuggestion>) -> Unit
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(this@StoreLocationMapActivity, Locale("vi", "VN"))
                @Suppress("DEPRECATION")
                val results = geocoder.getFromLocationName(keyword, 8)

                val suggestions = results
                    ?.mapNotNull { item ->
                        val fullAddress = item.getAddressLine(0)
                            ?: listOfNotNull(
                                item.featureName,
                                item.thoroughfare,
                                item.subAdminArea,
                                item.adminArea,
                                item.countryName
                            ).joinToString(", ").ifBlank { null }

                        if (fullAddress.isNullOrBlank()) return@mapNotNull null

                        OriginSuggestion(
                            address = fullAddress,
                            lat = item.latitude,
                            lng = item.longitude
                        )
                    }
                    ?.distinctBy { it.address }
                    ?: emptyList()

                withContext(Dispatchers.Main) {
                    onResult(suggestions)
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

    private fun requestDirectionsWithOrigin(
        store: StoreLocationDTO,
        originAddress: String,
        travelMode: String
    ) {
        val normalizedKey = normalizeAddress(originAddress)
        val cachedLat = selectedOriginLat
        val cachedLng = selectedOriginLng
        if (cachedLat != null && cachedLng != null && geocodedOriginKey == normalizedKey) {
            viewModel.requestDirections(
                storeLocationId = store.storeLocationId,
                fromLat = cachedLat,
                fromLng = cachedLng,
                travelMode = travelMode
            )
            return
        }

        geocodeAddress(
            address = originAddress,
            onSuccess = { lat, lng, resolvedAddress ->
                selectedOriginLat = lat
                selectedOriginLng = lng
                geocodedOriginKey = normalizeAddress(resolvedAddress)

                suppressOriginWatcher = true
                binding.edtOriginAddress.setText(resolvedAddress)
                binding.edtOriginAddress.setSelection(resolvedAddress.length)
                suppressOriginWatcher = false

                viewModel.requestDirections(
                    storeLocationId = store.storeLocationId,
                    fromLat = lat,
                    fromLng = lng,
                    travelMode = travelMode
                )
            },
            onError = { message ->
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
        )
    }

    private fun normalizeAddress(value: String): String {
        return value.trim().lowercase(Locale.ROOT)
    }

    private fun geocodeAddress(
        address: String,
        onSuccess: (Double, Double, String) -> Unit,
        onError: (String) -> Unit
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(this@StoreLocationMapActivity, Locale("vi", "VN"))
                @Suppress("DEPRECATION")
                val results = geocoder.getFromLocationName(address, 1)
                val first = results?.firstOrNull()

                withContext(Dispatchers.Main) {
                    if (first != null) {
                        val resolved = first.getAddressLine(0) ?: address
                        onSuccess(first.latitude, first.longitude, resolved)
                    } else {
                        onError("Không tìm thấy tọa độ cho địa chỉ này.")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(e.message ?: "Lỗi geocode địa chỉ.")
                }
            }
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.errorMessage.observe(this) { message ->
            if (!message.isNullOrBlank()) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                viewModel.clearMessages()
            }
        }

        viewModel.locations.observe(this) { locations ->
            setupStoreDropdown(locations)
            renderMarkers(locations)
        }

        viewModel.directionUrl.observe(this) { url ->
            if (!url.isNullOrBlank()) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                viewModel.clearMessages()
            }
        }
    }

    private fun setupStoreDropdown(locations: List<StoreLocationDTO>) {
        allStores.clear()
        allStores.addAll(locations)

        val labels = locations.map { store -> store.addressLine }

        ignoreStoreSpinnerEvent = true
        storeSpinnerAdapter.clear()
        storeSpinnerAdapter.add("Chọn cửa hàng")
        storeSpinnerAdapter.addAll(labels)
        storeSpinnerAdapter.notifyDataSetChanged()
        binding.spinnerStore.setSelection(0, false)
        binding.spinnerStore.isEnabled = locations.isNotEmpty()
        ignoreStoreSpinnerEvent = false
    }

    private fun renderMarkers(locations: List<StoreLocationDTO>) {
        val mapView = map ?: return
        mapView.overlays.clear()

        if (locations.isEmpty()) {
            mapView.controller.setZoom(CITY_ZOOM)
            mapView.controller.setCenter(GeoPoint(HCMC_LAT, HCMC_LNG))
            mapView.invalidate()
            return
        }

        locations.forEach { store ->
            val point = GeoPoint(store.latitude, store.longitude)
            val marker = Marker(mapView).apply {
                position = point
                title = store.name
                snippet = buildString {
                    append(store.addressLine)
                    if (!store.phoneNumber.isNullOrBlank()) append("\nSĐT: ${store.phoneNumber}")
                    if (!store.openHoursText.isNullOrBlank()) append("\nGiờ: ${store.openHoursText}")
                }
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                setOnMarkerClickListener { m, _ ->
                    val clicked = m.relatedObject as? StoreLocationDTO
                    if (clicked != null) {
                        updateSelectedStore(clicked, animateToMap = true, syncDropdown = true)
                    }
                    m.showInfoWindow()
                    true
                }
                relatedObject = store
            }
            mapView.overlays.add(marker)
        }

        mapView.controller.setZoom(CITY_ZOOM)
        mapView.controller.setCenter(GeoPoint(HCMC_LAT, HCMC_LNG))

        val initialStore = findClosestStoreToHcm(locations) ?: locations.first()
        updateSelectedStore(initialStore, animateToMap = false, syncDropdown = true)
        mapView.invalidate()
    }

    private fun findClosestStoreToHcm(locations: List<StoreLocationDTO>): StoreLocationDTO? {
        return locations.minByOrNull { store ->
            val dLat = store.latitude - HCMC_LAT
            val dLng = store.longitude - HCMC_LNG
            dLat * dLat + dLng * dLng
        }
    }

    private fun selectStoreInDropdown(store: StoreLocationDTO) {
        val index = allStores.indexOfFirst { it.storeLocationId == store.storeLocationId }
        if (index < 0) return

        val spinnerPosition = index + 1
        if (binding.spinnerStore.selectedItemPosition == spinnerPosition) return

        ignoreStoreSpinnerEvent = true
        binding.spinnerStore.setSelection(spinnerPosition, false)
        ignoreStoreSpinnerEvent = false
    }

    private fun updateSelectedStore(store: StoreLocationDTO, animateToMap: Boolean, syncDropdown: Boolean) {
        selectedStore = store
        binding.tvStoreName.text = store.name
        binding.tvStoreAddress.text = "Địa chỉ: ${store.addressLine}"
        binding.tvStorePhone.text = "SĐT: ${store.phoneNumber ?: "Đang cập nhật"}"
        binding.tvStoreHours.text = "Giờ làm việc: ${store.openHoursText ?: "Đang cập nhật"}"

        if (syncDropdown) {
            selectStoreInDropdown(store)
        }

        if (animateToMap) {
            val mapView = map ?: return
            mapView.controller.setZoom(STORE_ZOOM)
            mapView.controller.animateTo(GeoPoint(store.latitude, store.longitude))
        }
    }

    override fun onResume() {
        super.onResume()
        map?.onResume()
        BottomTabNavigator.refreshChatBadge(this)
    }

    override fun onPause() {
        originSuggestionJob?.cancel()
        map?.onPause()
        super.onPause()
    }
}
