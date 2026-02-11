package com.semester7.quatet.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.semester7.quatet.databinding.ItemProductBinding
import com.semester7.quatet.data.model.ProductDTO
import java.text.NumberFormat
import java.util.*
import coil.load
import coil.transform.RoundedCornersTransformation
import com.semester7.quatet.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProductAdapter(
    private var products: List<ProductDTO>,
    private val onItemClick: (Int) -> Unit
) :
    RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    // Ref to View
    class ProductViewHolder(val binding: ItemProductBinding) : RecyclerView.ViewHolder(binding.root)

    // các override fun: Layout Manager thực thi
    // Tạo View Holder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProductViewHolder(binding)
    }

    // Nạp dữ liệu cho View Holder
    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = products[position]
        with(holder.binding) {
            tvProductName.text = product.productname

            // Định dạng giá tiền
            val formatter = NumberFormat
                .getCurrencyInstance(
                    Locale.Builder()
                        .setLanguage("vi")
                        .setRegion("VN")
                        .build()
                )
            tvProductPrice.text = formatter.format(product.price)

            // Nạp ảnh bằng Coil từ imageUrl mới cập nhật
            holder.binding.imgProduct.load(product.imageUrl) {
                crossfade(true)
                placeholder(R.drawable.ic_image_placeholder)
                error(R.drawable.ic_image_placeholder)
                transformations(RoundedCornersTransformation(16f))
            }

            // Event
            cvProduct.setOnClickListener {
                // Trả ID về cho Activity xử lý
                onItemClick(product.productid)
            }

            // Add to cart (btnAddToCart) — kiểm tra login
            btnAddToCart.setOnClickListener {
                val context = holder.itemView.context
                if (!com.semester7.quatet.data.local.SessionManager.isLoggedIn(context)) {
                    context.startActivity(
                        android.content.Intent(context, com.semester7.quatet.ui.activities.LoginActivity::class.java)
                    )
                } else {
                    // Gọi API thêm giỏ hàng
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                        try {
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                com.semester7.quatet.data.repository.CartRepository().addItem(product.productid, 1)
                            }
                            android.widget.Toast.makeText(context, "Đã thêm vào giỏ hàng", android.widget.Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(context, "Lỗi: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    override fun getItemCount() = products.size

    // Cập nhật dữ liệu
    fun updateData(newProducts: List<ProductDTO>) {

        // So sánh danh sách cũ và mới
        val diffCallback = object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = products.size
            override fun getNewListSize(): Int = newProducts.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                // So sánh ID của sản phẩm để biết có cùng 1 đối tượng không
                return products[oldItemPosition].productid == newProducts[newItemPosition].productid
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                // So sánh nội dung bên trong (giá, tên, ảnh...)
                return products[oldItemPosition] == newProducts[newItemPosition]
            }
        }

        val diffResult = DiffUtil.calculateDiff(diffCallback)

        // Cập nhật lại list dữ liệu chính
        products = newProducts

        // Yêu cầu DiffUtil tự gọi các hàm notify phù hợp
        diffResult.dispatchUpdatesTo(this)
    }
}