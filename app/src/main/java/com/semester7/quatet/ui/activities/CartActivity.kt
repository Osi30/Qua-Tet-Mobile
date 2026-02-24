package com.semester7.quatet.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.semester7.quatet.databinding.ActivityCartBinding
import com.semester7.quatet.ui.adapters.CartAdapter
import com.semester7.quatet.viewmodel.CartViewModel
import java.text.NumberFormat
import java.util.*

class CartActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCartBinding
    private lateinit var adapter: CartAdapter
    private val viewModel: CartViewModel by viewModels()

    // Biến lưu trữ tổng tiền để truyền sang màn hình Checkout
    private var currentTotalPrice: Double = 0.0

    private val formatter = NumberFormat.getCurrencyInstance(
        Locale.Builder().setLanguage("vi").setRegion("VN").build()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Khởi tạo ViewBinding
        binding = ActivityCartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupListeners()
        observeViewModel()

        // Lấy giỏ hàng từ server
        viewModel.fetchCart()
    }

    private fun setupRecyclerView() {
        // QUAN TRỌNG: Bắt buộc phải có LayoutManager để RecyclerView biết cách xếp item (cuộn dọc)
        binding.rvCartItems.layoutManager = LinearLayoutManager(this)

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
                    showDeleteConfirmDialog(item.cartDetailId, item.productName ?: "sản phẩm")
                }
            },
            onRemove = { item ->
                showDeleteConfirmDialog(item.cartDetailId, item.productName ?: "sản phẩm")
            }
        )
        binding.rvCartItems.adapter = adapter
    }

    // Tách hàm Dialog ra cho code Clean và tái sử dụng
    private fun showDeleteConfirmDialog(cartDetailId: Int, productName: String) {
        AlertDialog.Builder(this)
            .setTitle("Xóa sản phẩm")
            .setMessage("Bạn có muốn xóa $productName khỏi giỏ hàng?")
            .setPositiveButton("Xóa") { _, _ ->
                viewModel.removeItem(cartDetailId)
            }
            .setNegativeButton("Hủy", null)
            .show()
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

        // Nút thanh toán chuyển sang CheckoutActivity
        binding.btnCheckout.setOnClickListener {
            if (currentTotalPrice > 0) {
                val intent = Intent(this, CheckoutActivity::class.java)
                intent.putExtra("EXTRA_TOTAL_PRICE", currentTotalPrice)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Giỏ hàng rỗng, không thể thanh toán!", Toast.LENGTH_SHORT).show()
            }
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
                currentTotalPrice = 0.0
            } else {
                // Có sản phẩm
                binding.rvCartItems.visibility = View.VISIBLE
                binding.layoutEmpty.visibility = View.GONE
                binding.layoutFooter.visibility = View.VISIBLE
                binding.tvClearCart.visibility = View.VISIBLE

                adapter.updateData(cart.items)

                // Cập nhật tổng tiền lên UI và lưu vào biến để chuẩn bị đẩy qua Checkout
                val total = cart.totalPrice ?: 0.0
                currentTotalPrice = total

                binding.tvTotalPrice.text = formatter.format(total)
                binding.tvItemCount.text = "${cart.itemCount ?: 0} sản phẩm"
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