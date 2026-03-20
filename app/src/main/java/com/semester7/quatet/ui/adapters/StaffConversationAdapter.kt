package com.semester7.quatet.ui.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.semester7.quatet.data.model.StaffConversationItem
import com.semester7.quatet.databinding.ItemStaffConversationBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class StaffConversationAdapter(
    private val onItemClick: (StaffConversationItem) -> Unit
) : RecyclerView.Adapter<StaffConversationAdapter.ConversationViewHolder>() {

    private val items = mutableListOf<StaffConversationItem>()
    private var selectedConversationId: Int? = null

    fun updateData(newItems: List<StaffConversationItem>, selectedId: Int?) {
        items.clear()
        items.addAll(newItems)
        selectedConversationId = selectedId
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemStaffConversationBinding.inflate(inflater, parent, false)
        return ConversationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) {
        holder.bind(items[position], items[position].id == selectedConversationId)
    }

    override fun getItemCount(): Int = items.size

    inner class ConversationViewHolder(
        private val binding: ItemStaffConversationBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: StaffConversationItem, isSelected: Boolean) {
            val unreadTitleColor = Color.parseColor("#2B2B2B")
            val normalTitleColor = Color.parseColor("#2B2B2B")
            val unreadSubColor = Color.parseColor("#2B2B2B")
            val normalSubColor = Color.parseColor("#8A7A66")
            val unreadTimeColor = Color.parseColor("#8B0000")
            val normalTimeColor = Color.parseColor("#8A7A66")

            binding.tvAvatar.text = (item.userId ?: item.id).toString().takeLast(2)
            binding.tvTitle.text = "User #${item.userId ?: "-"}"
            val preview = item.lastMessage?.trim().orEmpty()
            binding.tvSubtitle.text = if (preview.isBlank()) "Chua co tin nhan" else preview
            binding.tvTime.text = formatConversationTime(item.lastMessageAt ?: item.createdAt)
            binding.viewUnreadBadge.visibility = if (item.hasUnread) View.VISIBLE else View.GONE
            binding.tvTitle.setTextColor(if (item.hasUnread) unreadTitleColor else normalTitleColor)
            binding.tvSubtitle.setTextColor(if (item.hasUnread) unreadSubColor else normalSubColor)
            binding.tvTime.setTextColor(if (item.hasUnread) unreadTimeColor else normalTimeColor)
            binding.root.alpha = if (isSelected) 1f else 0.98f
            binding.root.setOnClickListener { onItemClick(item) }
        }
    }
}

private fun formatConversationTime(value: String?): String {
    val millis = parseIsoMillis(value) ?: return ""
    val dayKey = dayKeyFromMillis(millis)
    val todayKey = dayKeyFromMillis(System.currentTimeMillis())
    return if (dayKey == todayKey) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))
    } else {
        SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(millis))
    }
}

private fun dayKeyFromMillis(millis: Long): String {
    val formatter = SimpleDateFormat("yyyyMMdd", Locale.US)
    return formatter.format(Date(millis))
}

private fun parseIsoMillis(value: String?): Long? {
    if (value.isNullOrBlank()) return null
    return try {
        val normalized = normalizeIso(value)
        val formatWithMillisUtc = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        formatWithMillisUtc.parse(normalized)?.time
    } catch (_: Exception) {
        try {
            val formatNoMillisUtc = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            formatNoMillisUtc.parse(value)?.time
        } catch (_: Exception) {
            null
        }
    }
}

private fun normalizeIso(value: String): String {
    if (!value.contains(".")) return value

    val datePart = value.substringBefore(".")
    val fractionAndZone = value.substringAfter(".")
    val zoneStart = fractionAndZone.indexOfFirst { it == 'Z' || it == '+' || it == '-' }
    if (zoneStart < 0) return value

    val fraction = fractionAndZone.substring(0, zoneStart)
    val zone = fractionAndZone.substring(zoneStart)
    val normalizedFraction = fraction.take(3).padEnd(3, '0')
    return "$datePart.$normalizedFraction$zone"
}

