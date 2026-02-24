package com.semester7.quatet.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.semester7.quatet.R
import com.semester7.quatet.databinding.ActivityCartBinding
import com.semester7.quatet.ui.adapters.CartAdapter
import com.semester7.quatet.utils.NotificationHelper
import com.semester7.quatet.viewmodel.CartViewModel
import java.text.NumberFormat
import java.util.*

class CartActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCartBinding
    private lateinit var adapter: CartAdapter
    private val viewModel: CartViewModel by viewModels()

    private var currentTotalPrice: Double = 0.0

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

        viewModel.fetchCart()
    }

    private fun setupRecyclerView() {
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
                    showDeleteConfirmDialog(item.cartDetailId, item.productName ?: "sản phẩm")
                }
            },
            onRemove = { item ->
                showDeleteConfirmDialog(item.cartDetailId, item.productName ?: "sản phẩm")
            }
        )
        binding.rvCartItems.adapter = adapter
    }

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
        binding.ivBack.setOnClickListener { finish() }

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
        viewModel.cart.observe(this) { cart ->
            if (cart == null || cart.items.isEmpty()) {
                binding.rvCartItems.visibility = View.GONE
                binding.layoutEmpty.visibility = View.VISIBLE
                binding.layoutFooter.visibility = View.GONE
                binding.tvClearCart.visibility = View.GONE
                currentTotalPrice = 0.0

                // ĐỒNG BỘ BADGE: Xóa thông báo khi giỏ hàng rỗng
                NotificationHelper.showCartNotification(
                    this,
                    getString(R.string.app_name),
                    "Giỏ hàng của bạn đang trống.",
                    0 // Badge sẽ biến mất khi số lượng là 0
                )
            } else {
                binding.rvCartItems.visibility = View.VISIBLE
                binding.layoutEmpty.visibility = View.GONE
                binding.layoutFooter.visibility = View.VISIBLE
                binding.tvClearCart.visibility = View.VISIBLE

                adapter.updateData(cart.items)

                val total = cart.totalPrice ?: 0.0
                currentTotalPrice = total

                binding.tvTotalPrice.text = formatter.format(total)
                binding.tvItemCount.text = "${cart.itemCount ?: 0} sản phẩm"

                // ĐỒNG BỘ BADGE: Cập nhật số lượng mới nhất lên App Icon
                NotificationHelper.showCartNotification(
                    this,
                    getString(R.string.app_name),
                    "Bạn đang có ${cart.itemCount} sản phẩm trong giỏ hàng.",
                    cart.itemCount
                )
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.errorMessage.observe(this) { error ->
            if (!error.isNullOrEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
                viewModel.clearMessages()
            }
        }

        viewModel.successMessage.observe(this) { message ->
            if (!message.isNullOrEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                viewModel.clearMessages()
            }
        }
    }
}