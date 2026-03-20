package com.semester7.quatet.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.SearchView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.semester7.quatet.R
import com.semester7.quatet.data.local.SessionManager
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
    private val cartViewModel: CartViewModel by viewModels()
    private var filterSheet: ProductFilterSheet? = null
    private var sortSheet: ProductSortSheet? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityProductBinding.inflate(layoutInflater)
        setContentView(binding.root)

        NotificationHelper.createNotificationChannel(this)

        setupRecyclerView()
        setupSearch()
        BottomTabNavigator.setup(this, BottomTabNavigator.Tab.HOME)
        observeViewModel()
        observeCart()

        binding.tvFilter.setOnClickListener { showFilterSheet() }
        binding.tvSort.setOnClickListener { showSortSheet() }

        viewModel.fetchProducts()
    }

    private fun observeCart() {
        cartViewModel.cart.observe(this) { cart ->
            val count = cart?.itemCount ?: 0

            if (count > 0) {
                // Hien thi so luong ngoai icon app thong qua thu vien
                NotificationHelper.showCartNotification(
                    this,
                    getString(R.string.app_name),
                    "Ban dang co $count san pham trong gio hang.",
                    count
                )
            } else {
                // Don dep so luong ngoai icon app va tat thong bao
                NotificationHelper.clearBadge(this)
            }
        }

        cartViewModel.successMessage.observe(this) { message ->
            if (!message.isNullOrEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                cartViewModel.clearMessages()
            }
        }

        cartViewModel.errorMessage.observe(this) { message ->
            if (!message.isNullOrEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                cartViewModel.clearMessages()
            }
        }

        cartViewModel.isLoading.observe(this) { isLoading ->
            // Mở progress bar
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE

            // Tắt trang không tìm thấy sản phẩm
            if (isLoading) binding.layoutEmpty.visibility = View.GONE

            // Khóa tương tác
            binding.rvProducts.isEnabled = !isLoading
            // Làm mờ
            binding.rvProducts.alpha = if (isLoading) 0.5f else 1.0f
        }
    }

    private fun setupRecyclerView() {
        adapter = ProductAdapter(
            products = emptyList(),
            onItemClick = { productId -> navigateToDetail(productId) },
            onAddToCartClick = { product ->
                // Đã login thì add to cart
                if (SessionManager.isLoggedIn(this))
                    cartViewModel.addItem(product.productid)
                // Đá về login
                else
                    startActivity(Intent(this, LoginActivity::class.java))
            }
        )

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
            // Mở progress bar
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE

            // Tắt trang không tìm thấy sản phẩm
            if (isLoading) binding.layoutEmpty.visibility = View.GONE

            // Khóa tương tác
            binding.rvProducts.isEnabled = !isLoading
            // Làm mờ
            binding.rvProducts.alpha = if (isLoading) 0.5f else 1.0f
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

    override fun onResume() {
        super.onResume()
        BottomTabNavigator.refreshChatBadge(this)
    }
}
