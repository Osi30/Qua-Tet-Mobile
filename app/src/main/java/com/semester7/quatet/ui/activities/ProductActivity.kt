package com.semester7.quatet.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.SearchView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.semester7.quatet.R
import com.semester7.quatet.data.local.SessionManager
import com.semester7.quatet.data.remote.RetrofitClient
import com.semester7.quatet.databinding.ActivityProductBinding
import com.semester7.quatet.ui.adapters.ProductAdapter
import com.semester7.quatet.ui.screens.ProductDetailFragment
import com.semester7.quatet.ui.screens.ProductFilterSheet
import com.semester7.quatet.ui.screens.ProductSortSheet
import com.semester7.quatet.utils.NotificationHelper
import com.semester7.quatet.viewmodel.CartViewModel
import com.semester7.quatet.viewmodel.ProductViewModel

class ProductActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProductBinding
    private lateinit var adapter: ProductAdapter

    private val viewModel: ProductViewModel by viewModels()

    // CHỐT CHẶN: Activity khởi tạo CartViewModel gốc.
    // Fragment sẽ dùng activityViewModels() để truy cập vào đúng Instance này.
    private val cartViewModel: CartViewModel by viewModels()

    private var filterSheet: ProductFilterSheet? = null
    private var sortSheet: ProductSortSheet? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        RetrofitClient.init(this)
        binding = ActivityProductBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Khởi tạo Kênh thông báo ngay khi mở app để sẵn sàng hiển thị Badge [cite: 26]
        NotificationHelper.createNotificationChannel(this)

        setupRecyclerView()
        setupSearch()
        setupLogout()
        observeViewModel()

        // 2. Bắt đầu quan sát giỏ hàng để cập nhật Badge ngay lập tức [cite: 24]
        observeCart()

        binding.tvFilter.setOnClickListener { showFilterSheet() }
        binding.tvSort.setOnClickListener { showSortSheet() }

        binding.layoutCart.setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
        }

        viewModel.fetchProducts()

        // Lấy dữ liệu giỏ hàng ban đầu để hiển thị Badge nếu người dùng đã login [cite: 7]
        if (SessionManager.isLoggedIn(this)) {
            cartViewModel.fetchCart()
        }
    }

    override fun onResume() {
        super.onResume()
        updateLogoutVisibility()
        // Đảm bảo dữ liệu giỏ hàng được đồng bộ lại khi quay lại từ CartActivity
        if (SessionManager.isLoggedIn(this)) {
            cartViewModel.fetchCart()
        }
    }

    private fun observeCart() {
        // Lắng nghe mọi thay đổi từ giỏ hàng (kể cả lệnh gọi từ Fragment)
        cartViewModel.cart.observe(this) { cart ->
            val count = cart?.itemCount ?: 0

            // Cập nhật con số trên icon giỏ hàng TRONG ứng dụng
            if (count > 0) {
                binding.tvCartBadge.visibility = View.VISIBLE
                binding.tvCartBadge.text = if (count > 99) "99+" else count.toString()
            } else {
                binding.tvCartBadge.visibility = View.GONE
            }

            // Cập nhật Badge TRÊN Icon app ngoài màn hình chính [cite: 25]
            // Khi số lượng thay đổi, chúng ta cập nhật thầm lặng để Launcher hiển thị Badge mới
            NotificationHelper.showCartNotification(
                this,
                getString(R.string.app_name),
                "Bạn đang có $count sản phẩm trong giỏ hàng.",
                count
            )
        }
    }

    private fun setupRecyclerView() {
        adapter = ProductAdapter(emptyList()) { productId ->
            navigateToDetail(productId)
        }
        binding.rvProducts.adapter = adapter
    }

    private fun navigateToDetail(productId: Int) {
        val detailFragment = ProductDetailFragment()
        val bundle = Bundle().apply {
            putInt("PRODUCT_ID", productId)
        }
        detailFragment.arguments = bundle

        supportFragmentManager.beginTransaction()
            .replace(android.R.id.content, detailFragment)
            .addToBackStack(null)
            .setCustomAnimations(
                android.R.anim.slide_in_left,
                android.R.anim.fade_out,
                android.R.anim.fade_in,
                android.R.anim.slide_out_right
            )
            .commit()
    }

    private fun observeViewModel() {
        viewModel.products.observe(this) { productList ->
            if (productList.isNullOrEmpty()) {
                binding.rvProducts.visibility = View.GONE
                binding.layoutEmpty.visibility = View.VISIBLE
            } else {
                binding.rvProducts.visibility = View.VISIBLE
                binding.layoutEmpty.visibility = View.GONE
                adapter.updateData(productList)
                binding.rvProducts.post {
                    binding.rvProducts.scrollToPosition(0)
                }
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            if (isLoading) binding.layoutEmpty.visibility = View.GONE
        }
    }

    private fun showFilterSheet() {
        if (filterSheet == null) filterSheet = ProductFilterSheet()
        if (!filterSheet!!.isAdded) filterSheet!!.show(supportFragmentManager, "ProductFilterSheet")
    }

    private fun showSortSheet() {
        if (sortSheet == null) sortSheet = ProductSortSheet()
        if (!sortSheet!!.isAdded) sortSheet!!.show(supportFragmentManager, "ProductSortSheet")
    }

    private fun setupSearch() {
        binding.svProduct.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrBlank()) viewModel.updateFilters(search = query.trim())
                binding.svProduct.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.updateFilters(search = newText)
                if (newText.isNullOrEmpty()) viewModel.updateFilters(search = "")
                return true
            }
        })
    }

    private fun setupLogout() {
        updateLogoutVisibility()
        binding.ivLogout.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Đăng xuất")
                .setMessage("Bạn có muốn đăng xuất không?")
                .setPositiveButton("Đăng xuất") { _, _ ->
                    SessionManager.clearSession(this)
                    updateLogoutVisibility()
                    // Xóa hoàn toàn Badge khi logout
                    binding.tvCartBadge.visibility = View.GONE
                    NotificationHelper.showCartNotification(this, getString(R.string.app_name), "Đã đăng xuất", 0)
                    Toast.makeText(this, "Đã đăng xuất", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Hủy", null)
                .show()
        }
    }

    private fun updateLogoutVisibility() {
        binding.ivLogout.visibility = if (SessionManager.isLoggedIn(this)) View.VISIBLE else View.GONE
    }
}