package com.semester7.quatet.ui.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.semester7.quatet.data.model.StoreLocationDTO
import com.semester7.quatet.databinding.ActivityStoreLocationMapBinding
import com.semester7.quatet.viewmodel.StoreLocationViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class StoreLocationMapActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStoreLocationMapBinding
    private val viewModel: StoreLocationViewModel by viewModels()

    private var map: MapView? = null
    private var selectedStore: StoreLocationDTO? = null
    private var selectedOriginLat: Double? = null
    private var selectedOriginLng: Double? = null
    private var selectedOriginText: String = "Điểm xuất phát: chưa chọn"

    private val travelModes = listOf(
        "Lái xe" to "driving",
        "Đi bộ" to "walking",
        "Xe đạp" to "bicycling",
        "Công cộng" to "transit"
    )

    private val pickOriginLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                val data = result.data!!
                val address = data.getStringExtra(AddressActivity.RESULT_ADDRESS).orEmpty()
                val lat = data.getDoubleExtra(AddressActivity.RESULT_LAT, 0.0)
                val lng = data.getDoubleExtra(AddressActivity.RESULT_LNG, 0.0)

                if (lat == 0.0 && lng == 0.0) {
                    Toast.makeText(this, "Địa chỉ này chưa có tọa độ, vui lòng cập nhật lại.", Toast.LENGTH_LONG).show()
                    return@registerForActivityResult
                }

                selectedOriginLat = lat
                selectedOriginLng = lng
                selectedOriginText = "Điểm xuất phát: $address"
                binding.tvOriginAddress.text = selectedOriginText
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStoreLocationMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        BottomTabNavigator.setup(this, BottomTabNavigator.Tab.STORE_LOCATION)
        setupMap()
        setupUi()
        observeViewModel()
        viewModel.fetchLocations()
    }

    private fun setupMap() {
        Configuration.getInstance().userAgentValue = packageName
        map = binding.storeMap.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(5.5)
            controller.setCenter(GeoPoint(16.3, 106.2))
            minZoomLevel = 4.0
            maxZoomLevel = 19.0
        }
    }

    private fun setupUi() {
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            travelModes.map { it.first }
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerTravelMode.adapter = adapter

        binding.btnPickOrigin.setOnClickListener {
            pickOriginLauncher.launch(
                Intent(this, AddressActivity::class.java).apply {
                    putExtra(AddressActivity.EXTRA_PICK_ADDRESS_MODE, true)
                }
            )
        }

        binding.btnNavigate.setOnClickListener {
            val store = selectedStore
            val fromLat = selectedOriginLat
            val fromLng = selectedOriginLng
            if (store == null) {
                Toast.makeText(this, "Vui lòng chọn cửa hàng trên bản đồ.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (fromLat == null || fromLng == null) {
                Toast.makeText(this, "Vui lòng chọn địa chỉ xuất phát.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedMode = travelModes[binding.spinnerTravelMode.selectedItemPosition].second
            viewModel.requestDirections(
                storeLocationId = store.storeLocationId,
                fromLat = fromLat,
                fromLng = fromLng,
                travelMode = selectedMode
            )
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
            renderMarkers(locations)
        }

        viewModel.directionUrl.observe(this) { url ->
            if (!url.isNullOrBlank()) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                viewModel.clearMessages()
            }
        }
    }

    private fun renderMarkers(locations: List<StoreLocationDTO>) {
        val mapView = map ?: return
        mapView.overlays.clear()

        if (locations.isEmpty()) {
            mapView.controller.setZoom(5.5)
            mapView.controller.setCenter(GeoPoint(16.3, 106.2))
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
                        updateSelectedStore(clicked)
                    }
                    m.showInfoWindow()
                    mapView.controller.animateTo(m.position)
                    true
                }
                relatedObject = store
            }
            mapView.overlays.add(marker)
        }

        mapView.controller.setZoom(5.5)
        mapView.controller.setCenter(GeoPoint(16.3, 106.2))
        mapView.invalidate()

        updateSelectedStore(locations.first())
    }

    private fun updateSelectedStore(store: StoreLocationDTO) {
        selectedStore = store
        binding.tvStoreName.text = store.name
        binding.tvStoreAddress.text = "Địa chỉ: ${store.addressLine}"
        binding.tvStorePhone.text = "SĐT: ${store.phoneNumber ?: "Đang cập nhật"}"
        binding.tvStoreHours.text = "Giờ làm việc: ${store.openHoursText ?: "Đang cập nhật"}"
        binding.tvOriginAddress.text = selectedOriginText
    }

    override fun onResume() {
        super.onResume()
        map?.onResume()
        BottomTabNavigator.refreshChatBadge(this)
    }

    override fun onPause() {
        map?.onPause()
        super.onPause()
    }
}
