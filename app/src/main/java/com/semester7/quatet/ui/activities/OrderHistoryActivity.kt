package com.semester7.quatet.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.semester7.quatet.MainActivity // BỔ SUNG IMPORT NÀY ĐỂ HẾT LỖI
import com.semester7.quatet.databinding.ActivityOrderHistoryBinding
import com.semester7.quatet.ui.adapters.OrderHistoryAdapter
import com.semester7.quatet.viewmodel.OrderHistoryViewModel

class OrderHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOrderHistoryBinding
    private val viewModel: OrderHistoryViewModel by viewModels()
    private lateinit var adapter: OrderHistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrderHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupBottomNavigation()
        observeViewModel()

        // Ra lệnh gọi API ngay khi Activity vừa được tạo
        viewModel.fetchMyOrders()
    }

    private fun setupRecyclerView() {
        // Khởi tạo adapter với danh sách rỗng ban đầu
        adapter = OrderHistoryAdapter(emptyList())
        binding.rvOrderHistory.layoutManager = LinearLayoutManager(this)
        binding.rvOrderHistory.adapter = adapter
    }

    private fun observeViewModel() {
        // Lắng nghe vòng xoay Loading
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // Lắng nghe danh sách đơn hàng trả về
        viewModel.orders.observe(this) { orderList ->
            if (orderList.isNullOrEmpty()) {
                // Nếu rỗng: Hiện chữ "Chưa có đơn hàng", ẩn danh sách
                binding.rvOrderHistory.visibility = View.GONE
                binding.tvEmptyState.visibility = View.VISIBLE
            } else {
                // Nếu có data: Đẩy vào Adapter, hiện danh sách
                binding.rvOrderHistory.visibility = View.VISIBLE
                binding.tvEmptyState.visibility = View.GONE
                adapter.updateData(orderList)
            }
        }

        // Lắng nghe thông báo lỗi
        viewModel.errorMessage.observe(this) { errorMsg ->
            if (!errorMsg.isNullOrEmpty()) {
                Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show()
                viewModel.clearError() // Xóa lỗi đi để không bị Toast lại khi xoay màn hình
            }
        }
    }

    private fun setupBottomNavigation() {
        // Bắt sự kiện chuyển tab bằng ViewBinding thông qua id "includeBottomNav"
        binding.includeBottomNav.navHome.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            overridePendingTransition(0, 0)
            finish() // Đóng Activity hiện tại để tránh xếp chồng màn hình
        }

        binding.includeBottomNav.navCart.setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
            overridePendingTransition(0, 0)
            finish()
        }

        binding.includeBottomNav.navStoreLocation.setOnClickListener {
            startActivity(Intent(this, StoreLocationMapActivity::class.java))
            overridePendingTransition(0, 0)
            finish()
        }

        // Tab Lịch sử (navOrderHistory) không cần click vì đang đứng ở màn hình này rồi
    }
}