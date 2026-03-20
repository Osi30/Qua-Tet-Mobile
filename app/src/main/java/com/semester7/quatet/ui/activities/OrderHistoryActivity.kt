package com.semester7.quatet.ui.activities

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
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
        BottomTabNavigator.setup(this, BottomTabNavigator.Tab.ORDER_HISTORY)
        observeViewModel()

        viewModel.fetchMyOrders()
    }

    override fun onResume() {
        super.onResume()
        BottomTabNavigator.refreshChatBadge(this)
    }

    private fun setupRecyclerView() {
        adapter = OrderHistoryAdapter(emptyList())
        binding.rvOrderHistory.layoutManager = LinearLayoutManager(this)
        binding.rvOrderHistory.adapter = adapter
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.orders.observe(this) { orderList ->
            if (orderList.isNullOrEmpty()) {
                binding.rvOrderHistory.visibility = View.GONE
                binding.tvEmptyState.visibility = View.VISIBLE
            } else {
                binding.rvOrderHistory.visibility = View.VISIBLE
                binding.tvEmptyState.visibility = View.GONE
                adapter.updateData(orderList)
            }
        }

        viewModel.errorMessage.observe(this) { errorMsg ->
            if (!errorMsg.isNullOrEmpty()) {
                Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }
    }
}
