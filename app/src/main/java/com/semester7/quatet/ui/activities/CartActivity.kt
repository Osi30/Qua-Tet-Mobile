package com.semester7.quatet.ui.activities

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.semester7.quatet.databinding.ActivityCartBinding
import com.semester7.quatet.ui.adapters.CartAdapter
import com.semester7.quatet.viewmodel.CartViewModel
import java.text.NumberFormat
import java.util.*

class CartActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCartBinding
    private lateinit var adapter: CartAdapter
    private val viewModel: CartViewModel by viewModels()

    private val formatter = NumberFormat.getCurrencyInstance(
        Locale.Builder().setLanguage("vi").setRegion("VN").build()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupListeners()
        observeViewModel()

        // Lấy giỏ hàng từ server
        viewModel.fetchCart()
    }

    private fun setupRecyclerView() {
        adapter = CartAdapter(
            items = emptyList(),
            onIncrease = { item ->
                val newQty = (item.quantity ?: 0) + 1
                viewModel.updateItem(item.cartDetailId, newQty)
            },
            onDecrease = { item ->
                val currentQty = item.quantity ?: 0
                if (currentQty > 1) {
                    viewModel.updateItem(item.cartDetailId, currentQty - 1)
                } else {
                    // Nếu số lượng = 1, hỏi xóa
                    AlertDialog.Builder(this)
                        .setTitle("Xóa sản phẩm")
                        .setMessage("Bạn có muốn xóa ${item.productName} khỏi giỏ hàng?")
                        .setPositiveButton("Xóa") { _, _ ->
                            viewModel.removeItem(item.cartDetailId)
                        }
                        .setNegativeButton("Hủy", null)
                        .show()
                }
            },
            onRemove = { item ->
                AlertDialog.Builder(this)
                    .setTitle("Xóa sản phẩm")
                    .setMessage("Bạn có muốn xóa ${item.productName} khỏi giỏ hàng?")
                    .setPositiveButton("Xóa") { _, _ ->
                        viewModel.removeItem(item.cartDetailId)
                    }
                    .setNegativeButton("Hủy", null)
                    .show()
            }
        )
        binding.rvCartItems.adapter = adapter
    }

    private fun setupListeners() {
        // Nút quay lại
        binding.ivBack.setOnClickListener { finish() }

        // Nút xóa hết giỏ hàng
        binding.tvClearCart.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Xóa giỏ hàng")
                .setMessage("Bạn có muốn xóa toàn bộ giỏ hàng không?")
                .setPositiveButton("Xóa hết") { _, _ ->
                    viewModel.clearCart()
                }
                .setNegativeButton("Hủy", null)
                .show()
        }

        // Nút thanh toán
        binding.btnCheckout.setOnClickListener {
            Toast.makeText(this, "Tính năng thanh toán đang được phát triển", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModel() {
        // Giỏ hàng
        viewModel.cart.observe(this) { cart ->
            if (cart == null || cart.items.isEmpty()) {
                // Giỏ hàng trống
                binding.rvCartItems.visibility = View.GONE
                binding.layoutEmpty.visibility = View.VISIBLE
                binding.layoutFooter.visibility = View.GONE
                binding.tvClearCart.visibility = View.GONE
            } else {
                // Có sản phẩm
                binding.rvCartItems.visibility = View.VISIBLE
                binding.layoutEmpty.visibility = View.GONE
                binding.layoutFooter.visibility = View.VISIBLE
                binding.tvClearCart.visibility = View.VISIBLE

                adapter.updateData(cart.items)

                // Cập nhật tổng tiền
                binding.tvTotalPrice.text = formatter.format(cart.totalPrice)
                binding.tvItemCount.text = "${cart.itemCount} sản phẩm"
            }
        }

        // Loading
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // Lỗi
        viewModel.errorMessage.observe(this) { error ->
            if (!error.isNullOrEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
                viewModel.clearMessages()
            }
        }

        // Thành công
        viewModel.successMessage.observe(this) { message ->
            if (!message.isNullOrEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                viewModel.clearMessages()
            }
        }
    }
}
