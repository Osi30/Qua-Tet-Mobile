package com.semester7.quatet.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.RoundedCornersTransformation
import com.semester7.quatet.data.model.CartItemResponse
import com.semester7.quatet.databinding.ItemCartBinding
import java.text.NumberFormat
import java.util.*

class CartAdapter(
    private var items: List<CartItemResponse>,
    private val onIncrease: (CartItemResponse) -> Unit,
    private val onDecrease: (CartItemResponse) -> Unit,
    private val onRemove: (CartItemResponse) -> Unit
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    class CartViewHolder(val binding: ItemCartBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val binding = ItemCartBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CartViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        val item = items[position]
        val formatter = NumberFormat.getCurrencyInstance(
            Locale.Builder().setLanguage("vi").setRegion("VN").build()
        )

        with(holder.binding) {
            // Tên sản phẩm
            tvProductName.text = item.productName ?: "Sản phẩm"

            // Giá đơn vị
            tvPrice.text = formatter.format(item.price ?: 0.0)

            // Số lượng
            tvQuantity.text = (item.quantity ?: 0).toString()

            // SubTotal
            tvSubTotal.text = formatter.format(item.subTotal)

            // Ảnh sản phẩm
            if (!item.imageUrl.isNullOrEmpty()) {
                ivProductImage.load(item.imageUrl) {
                    crossfade(true)
                    transformations(RoundedCornersTransformation(8f))
                }
            }

            // Sự kiện nút tăng
            btnIncrease.setOnClickListener { onIncrease(item) }

            // Sự kiện nút giảm
            btnDecrease.setOnClickListener { onDecrease(item) }

            // Sự kiện nút xóa
            btnRemove.setOnClickListener { onRemove(item) }
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<CartItemResponse>) {
        items = newItems
        notifyDataSetChanged()
    }
}
