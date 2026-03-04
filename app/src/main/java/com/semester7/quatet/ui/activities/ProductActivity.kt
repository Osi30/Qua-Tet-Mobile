package com.semester7.quatet.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.PopupMenu
import android.widget.SearchView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.semester7.quatet.R
import com.semester7.quatet.data.local.SessionManager
import com.semester7.quatet.data.remote.RetrofitClient
import com.semester7.quatet.databinding.ActivityProductBinding
import com.semester7.quatet.ui.adapters.ProductAdapter
import com.semester7.quatet.ui.adapters.QuickCategoryAdapter
import com.semester7.quatet.ui.adapters.QuickCategoryItem
import com.semester7.quatet.ui.screens.ProductDetailFragment
import com.semester7.quatet.ui.screens.ProductFilterSheet
import com.semester7.quatet.ui.screens.ProductSortSheet
import com.semester7.quatet.utils.NotificationHelper
import com.semester7.quatet.viewmodel.CartViewModel
import com.semester7.quatet.viewmodel.ProductViewModel

class ProductActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProductBinding
    private lateinit var adapter: ProductAdapter
    private lateinit var quickCategoryAdapter: QuickCategoryAdapter

    private val viewModel: ProductViewModel by viewModels()
    private val cartViewModel: CartViewModel by viewModels()

    private var filterSheet: ProductFilterSheet? = null
    private var sortSheet: ProductSortSheet? = null
    private var selectedQuickCategoryId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        RetrofitClient.init(this)
        binding = ActivityProductBinding.inflate(layoutInflater)
        setContentView(binding.root)

        NotificationHelper.createNotificationChannel(this)

        setupRecyclerView()
        setupQuickCategories()
        setupSearch()
        setupHeaderMenu()
        BottomTabNavigator.setup(this, BottomTabNavigator.Tab.HOME)
        setupLogout()
        observeViewModel()
        observeCart()

        binding.tvFilter.setOnClickListener { showFilterSheet() }
        binding.tvSort.setOnClickListener { showSortSheet() }

        binding.layoutCart.setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
        }

        viewModel.fetchProducts()

        if (SessionManager.isLoggedIn(this)) {
            cartViewModel.fetchCart()
        }
    }

    override fun onResume() {
        super.onResume()
        updateLogoutVisibility()
        if (SessionManager.isLoggedIn(this)) {
            cartViewModel.fetchCart()
        }
    }

    private fun observeCart() {
        cartViewModel.cart.observe(this) { cart ->
            val count = cart?.itemCount ?: 0

            if (count > 0) {
                // Hien thi so luong trong app
                binding.tvCartBadge.visibility = View.VISIBLE
                binding.tvCartBadge.text = if (count > 99) "99+" else count.toString()

                // Hien thi so luong ngoai icon app thong qua thu vien
                NotificationHelper.showCartNotification(
                    this,
                    getString(R.string.app_name),
                    "Ban dang co $count san pham trong gio hang.",
                    count
                )
            } else {
                // An so luong trong app
                binding.tvCartBadge.visibility = View.GONE

                // Don dep so luong ngoai icon app va tat thong bao
                NotificationHelper.clearBadge(this)
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = ProductAdapter(emptyList()) { productId ->
            navigateToDetail(productId)
        }
        binding.rvProducts.adapter = adapter
    }

    private fun setupQuickCategories() {
        quickCategoryAdapter = QuickCategoryAdapter(emptyList()) { categoryId ->
            selectedQuickCategoryId = categoryId
            val selectedIds = categoryId?.let { listOf(it) } ?: emptyList()
            viewModel.currentSelectedCategoryIds = selectedIds
            viewModel.updateFilters(categories = selectedIds)
        }

        binding.rvQuickCategories.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvQuickCategories.adapter = quickCategoryAdapter
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
        viewModel.categories.observe(this) { categories ->
            val quickItems = mutableListOf(QuickCategoryItem(id = null, label = "All"))
            quickItems.addAll(
                categories.orEmpty().map { category ->
                    QuickCategoryItem(
                        id = category.categoryid,
                        label = category.categoryname
                    )
                }
            )

            val validSelection = selectedQuickCategoryId?.takeIf { selectedId ->
                categories.orEmpty().any { it.categoryid == selectedId }
            }
            selectedQuickCategoryId = validSelection
            quickCategoryAdapter.updateData(quickItems, selectedQuickCategoryId)
        }

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

    private fun setupHeaderMenu() {
        binding.ivMenu.setOnClickListener { anchor ->
            val popup = PopupMenu(this, anchor)
            popup.menuInflater.inflate(R.menu.menu_header_options, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_address_book -> {
                        startActivity(Intent(this, AddressActivity::class.java))
                        true
                    }

                    R.id.menu_store_locations -> {
                        startActivity(Intent(this, StoreLocationMapActivity::class.java))
                        true
                    }

                    R.id.menu_chat_support -> {
                        startActivity(Intent(this, ChatActivity::class.java))
                        true
                    }

                    else -> false
                }
            }
            popup.show()
        }
    }

    private fun setupLogout() {
        // Trang homepage hien tai khong su dung nut dang xuat o header
        binding.ivLogout.visibility = View.GONE
    }

    private fun updateLogoutVisibility() {
        binding.ivLogout.visibility = View.GONE
    }
}
