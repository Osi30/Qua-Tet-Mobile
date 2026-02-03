package com.semester7.quatet.ui.activities

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.semester7.quatet.R
import com.semester7.quatet.databinding.ActivityProductBinding
import com.semester7.quatet.ui.adapters.ProductAdapter
import com.semester7.quatet.viewmodel.ProductViewModel

class ProductActivity : AppCompatActivity() {
    // lateinit: không khởi tạo ngay khi khai báo mà chờ onCreate()
    private lateinit var binding: ActivityProductBinding
    private val viewModel: ProductViewModel by viewModels()
    private lateinit var adapter: ProductAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Khởi tạo binding (Kết nối với file XML)
        binding = ActivityProductBinding.inflate(layoutInflater)

        // 2. Thiết lập nội dung hiển thị là cái "gốc" của file binding đó
        setContentView(binding.root)

        setupRecyclerView()
        observeViewModel()

        // Lệnh bắt đầu gọi API
        viewModel.fetchProducts()
    }

    private fun setupRecyclerView() {
        adapter = ProductAdapter(emptyList())
        binding.rvProducts.adapter = adapter
    }

    // Khi có thay đổi -> kích hoạt event
    private fun observeViewModel() {
        // Quan sát danh sách sản phẩm
        viewModel.products.observe(this) { productList ->
            if (productList != null) {
                adapter.updateData(productList)
            }
        }

        // Quan sát trạng thái Loading
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }
}