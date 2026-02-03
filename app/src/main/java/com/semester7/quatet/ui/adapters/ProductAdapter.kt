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

class ProductAdapter(private var products: List<ProductDTO>) :
    RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    class ProductViewHolder(val binding: ItemProductBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = products[position]
        with(holder.binding) {
            tvProductName.text = product.productname

            // Định dạng giá tiền
            val formatter = NumberFormat
                .getCurrencyInstance(Locale.Builder()
                    .setLanguage("vi")
                    .setRegion("VN")
                    .build())
            tvProductPrice.text = formatter.format(product.price)

            // Nạp ảnh bằng Coil từ imageUrl mới cập nhật
            holder.binding.imgProduct.load(product.imageUrl) {
                crossfade(true)
                placeholder(R.drawable.ic_image_placeholder)
                error(R.drawable.ic_image_placeholder)
                transformations(RoundedCornersTransformation(16f))
            }

            // Xử lý sự kiện click nút Add to cart (btnAddToCart)
            btnAddToCart.setOnClickListener {
                // Code xử lý thêm vào giỏ hàng tại đây
            }
        }
    }

    override fun getItemCount() = products.size

    // Hàm cập nhật dữ liệu khi gọi API xong
    fun updateData(newProducts: List<ProductDTO>) {
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