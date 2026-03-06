package com.semester7.quatet.ui.adapters

import android.content.res.ColorStateList // Import thêm cái này để giữ bo góc
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.semester7.quatet.data.model.OrderDTO
import com.semester7.quatet.databinding.ItemOrderHistoryBinding
import java.text.NumberFormat
import java.util.Locale

class OrderHistoryAdapter(
    private var orderList: List<OrderDTO>
) : RecyclerView.Adapter<OrderHistoryAdapter.OrderViewHolder>() {

    inner class OrderViewHolder(val binding: ItemOrderHistoryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val binding = ItemOrderHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return OrderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = orderList[position]

        with(holder.binding) {
            tvOrderId.text = "Đơn hàng #${order.orderId ?: "N/A"}"

            // Xử lý chuỗi ngày tháng: Cắt bỏ phần chữ "T" và giờ phút dư thừa
            val dateStr = order.orderDateTime?.substringBefore("T") ?: "Không rõ"
            tvOrderDate.text = dateStr

            val format = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
            tvOrderTotal.text = format.format(order.totalPrice ?: 0.0)

            // Kiểm tra trạng thái và ĐỔI MÀU NHƯNG VẪN GIỮ BO GÓC
            val statusStr = order.status?.trim()?.uppercase()
            when (statusStr) {
                "PENDING" -> {
                    tvOrderStatus.text = "Chờ xử lý"
                    tvOrderStatus.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FF9800"))
                }
                "CONFIRMED" -> {
                    tvOrderStatus.text = "Đã xác nhận"
                    tvOrderStatus.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#4CAF50"))
                }
                "CANCELLED" -> {
                    tvOrderStatus.text = "Đã hủy"
                    tvOrderStatus.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F44336"))
                }
                else -> {
                    tvOrderStatus.text = statusStr ?: "Không xác định"
                    tvOrderStatus.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#9E9E9E"))
                }
            }
        }
    }

    override fun getItemCount(): Int = orderList.size

    fun updateData(newList: List<OrderDTO>) {
        orderList = newList
        notifyDataSetChanged()
    }
}