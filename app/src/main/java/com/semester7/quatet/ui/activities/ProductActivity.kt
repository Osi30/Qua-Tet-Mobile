package com.semester7.quatet.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.SearchView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.semester7.quatet.data.local.SessionManager
import com.semester7.quatet.data.remote.RetrofitClient
import com.semester7.quatet.databinding.ActivityProductBinding
import com.semester7.quatet.ui.adapters.ProductAdapter
import com.semester7.quatet.ui.screens.ProductDetailFragment
import com.semester7.quatet.ui.screens.ProductFilterSheet
import com.semester7.quatet.ui.screens.ProductSortSheet
import com.semester7.quatet.viewmodel.ProductViewModel

class ProductActivity : AppCompatActivity() {
    // lateinit: không khởi tạo ngay khi khai báo
    private lateinit var binding: ActivityProductBinding
    private lateinit var adapter: ProductAdapter

    private val viewModel: ProductViewModel by viewModels()
    private var filterSheet: ProductFilterSheet? = null
    private var sortSheet: ProductSortSheet? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 0. Khởi tạo RetrofitClient với Context (để gắn JWT token)
        RetrofitClient.init(this)

        // 1. Khởi tạo binding (Kết nối với file XML)
        binding = ActivityProductBinding.inflate(layoutInflater)

        // 2. Thiết lập nội dung hiển thị là cái "gốc" của file binding đó
        setContentView(binding.root)

        setupRecyclerView()
        setupSearch()
        setupLogout()
        observeViewModel()

        // 3. Thiết lập event cho các button
        binding.tvFilter.setOnClickListener { showFilterSheet() }
        binding.tvSort.setOnClickListener { showSortSheet() }

        // 4. Icon giỏ hàng → mở CartActivity
        binding.ivCart.setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
        }

        viewModel.fetchProducts()
    }

    override fun onResume() {
        super.onResume()
        // Cập nhật icon logout mỗi khi quay lại màn hình
        updateLogoutVisibility()
    }

    // Gắn Adapter
    private fun setupRecyclerView() {
        adapter = ProductAdapter(emptyList()) { productId ->
            navigateToDetail(productId)
        }
        binding.rvProducts.adapter = adapter
    }

    // Kích hoạt mở detail đè lên list
    private fun navigateToDetail(productId: Int) {
        // 1. Tạo Fragment mới
        val detailFragment = ProductDetailFragment()

        // 2. Đóng gói ID sản phẩm vào Bundle để gắn vào argument
        val bundle = Bundle().apply {
            putInt("PRODUCT_ID", productId)
        }
        detailFragment.arguments = bundle

        // 3. Thực hiện chuyển Fragment
        supportFragmentManager.beginTransaction()
            // ID của FrameLayout "gốc" sẽ đè lên toàn bộ Activity
            // -> Hiển thị detail đè lên trang list
            .replace(android.R.id.content, detailFragment)
            // Cho phép nhấn Back để quay lại list
            .addToBackStack(null)
            // Thêm hiệu ứng trượt
            .setCustomAnimations(
                android.R.anim.slide_in_left,
                android.R.anim.fade_out,
                android.R.anim.fade_in,
                android.R.anim.slide_out_right
            )
            .commit()
    }

    // Khi có thay đổi -> kích hoạt event
    private fun observeViewModel() {
        viewModel.products.observe(this) { productList ->
            if (productList.isNullOrEmpty()) {
                // TH KHÔNG có sản phẩm
                binding.rvProducts.visibility = View.GONE
                binding.layoutEmpty.visibility = View.VISIBLE
            } else {
                // TH CÓ sản phẩm
                binding.rvProducts.visibility = View.VISIBLE
                binding.layoutEmpty.visibility = View.GONE

                adapter.updateData(productList)

                // Cuộn lên đầu trang
                binding.rvProducts.post {
                    binding.rvProducts.scrollToPosition(0)
                }
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            // Nếu đang load thì tạm thời ẩn thông báo trống để tránh bị nháy
            if (isLoading) binding.layoutEmpty.visibility = View.GONE
        }
    }

    private fun showFilterSheet() {
        // Khởi tạo class
        if (filterSheet == null) {
            filterSheet = ProductFilterSheet()
        }

        // Kiểm tra xem đã đang hiển thị trên màn hình chưa để tránh crash
        if (!filterSheet!!.isAdded) {
            filterSheet!!.show(supportFragmentManager, "ProductFilterSheet")
        }
    }

    private fun showSortSheet() {
        // Khởi tạo class
        if (sortSheet == null) {
            sortSheet = ProductSortSheet()
        }

        // Kiểm tra xem đã đang hiển thị trên màn hình chưa để tránh crash
        if (!sortSheet!!.isAdded) {
            sortSheet!!.show(supportFragmentManager, "ProductSortSheet")
        }
    }

    // Xử lí tìm kiếm
    private fun setupSearch() {
        binding.svProduct.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

            // Chạy khi nhấn nút "Tìm kiếm" (Kính lúp) trên bàn phím
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrBlank()) {
                    viewModel.updateFilters(search = query.trim())
                }

                // Ẩn bàn phím sau khi nhấn tìm
                binding.svProduct.clearFocus()
                return true
            }

            // Chạy MỖI KHI gõ hoặc xóa 1 ký tự
            override fun onQueryTextChange(newText: String?): Boolean {
                // Gõ đến đâu, lọc đến đó:
                viewModel.updateFilters(search = newText)

                // Reset kết quả tìm kiếm về ban đầu khi xóa hết chữ
                if (newText.isNullOrEmpty()) {
                    viewModel.updateFilters(search = "")
                }
                return true
            }
        })
    }

    // Logout
    private fun setupLogout() {
        updateLogoutVisibility()

        binding.ivLogout.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Đăng xuất")
                .setMessage("Bạn có muốn đăng xuất không?")
                .setPositiveButton("Đăng xuất") { _, _ ->
                    SessionManager.clearSession(this)
                    updateLogoutVisibility()
                    Toast.makeText(this, "Đã đăng xuất", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Hủy", null)
                .show()
        }
    }

    private fun updateLogoutVisibility() {
        binding.ivLogout.visibility =
            if (SessionManager.isLoggedIn(this)) View.VISIBLE else View.GONE
    }
}