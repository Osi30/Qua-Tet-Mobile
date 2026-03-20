package com.semester7.quatet.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.semester7.quatet.data.model.ChatMessageDTO
import com.semester7.quatet.databinding.ItemChatDateHeaderBinding
import com.semester7.quatet.databinding.ItemChatMessageMeBinding
import com.semester7.quatet.databinding.ItemChatMessageStaffBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class ChatMessageAdapter(
    private val currentUserId: Int
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_ME = 1
        private const val TYPE_OTHER = 2
        private const val TYPE_DATE = 3
    }

    private val items = mutableListOf<ChatMessageDTO>()
    private val displayItems = mutableListOf<ChatDisplayItem>()

    fun updateData(newItems: List<ChatMessageDTO>) {
        items.clear()
        items.addAll(newItems.sortedBy { it.id })
        rebuildDisplayItems()
        notifyDataSetChanged()
    }

    fun getLastAdapterPosition(): Int = displayItems.lastIndex

    override fun getItemViewType(position: Int): Int {
        return when (val item = displayItems[position]) {
            is ChatDisplayItem.DateHeader -> TYPE_DATE
            is ChatDisplayItem.Message -> if (item.message.senderId == currentUserId) TYPE_ME else TYPE_OTHER
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_ME -> {
                val binding = ItemChatMessageMeBinding.inflate(inflater, parent, false)
                MeViewHolder(binding)
            }

            TYPE_OTHER -> {
                val binding = ItemChatMessageStaffBinding.inflate(inflater, parent, false)
                OtherViewHolder(binding)
            }

            else -> {
                val binding = ItemChatDateHeaderBinding.inflate(inflater, parent, false)
                DateHeaderViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = displayItems[position]
        when (holder) {
            is MeViewHolder -> if (item is ChatDisplayItem.Message) holder.bind(item.message)
            is OtherViewHolder -> if (item is ChatDisplayItem.Message) holder.bind(item.message)
            is DateHeaderViewHolder -> if (item is ChatDisplayItem.DateHeader) holder.bind(item.label)
        }
    }

    override fun getItemCount(): Int = displayItems.size

    private fun rebuildDisplayItems() {
        displayItems.clear()
        var previousDayKey: String? = null
        items.forEach { message ->
            val dayKey = formatDayKey(message.createdAt)
            if (dayKey != previousDayKey) {
                displayItems.add(ChatDisplayItem.DateHeader(formatDayLabel(message.createdAt)))
                previousDayKey = dayKey
            }
            displayItems.add(ChatDisplayItem.Message(message))
        }
    }

    class MeViewHolder(private val binding: ItemChatMessageMeBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ChatMessageDTO) {
            binding.tvContent.text = item.content
            binding.tvTime.text = formatMessageTime(item.createdAt)
        }
    }

    class OtherViewHolder(private val binding: ItemChatMessageStaffBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ChatMessageDTO) {
            binding.tvContent.text = item.content
            binding.tvTime.text = formatMessageTime(item.createdAt)
        }
    }

    class DateHeaderViewHolder(private val binding: ItemChatDateHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(label: String) {
            binding.tvDateLabel.text = label
        }
    }
}

private sealed class ChatDisplayItem {
    data class Message(val message: ChatMessageDTO) : ChatDisplayItem()
    data class DateHeader(val label: String) : ChatDisplayItem()
}

private fun formatMessageTime(value: String?): String {
    val millis = parseIsoMillis(value) ?: return ""
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    return formatter.format(Date(millis))
}

private fun formatDayLabel(value: String?): String {
    val millis = parseIsoMillis(value) ?: return "Khong ro ngay"
    val targetDayKey = dayKeyFromMillis(millis)
    val todayDayKey = dayKeyFromMillis(System.currentTimeMillis())

    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_YEAR, -1)
    val yesterdayDayKey = dayKeyFromMillis(calendar.timeInMillis)

    return when (targetDayKey) {
        todayDayKey -> "Hom nay"
        yesterdayDayKey -> "Hom qua"
        else -> SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(millis))
    }
}

private fun formatDayKey(value: String?): String {
    val millis = parseIsoMillis(value) ?: return "unknown"
    return dayKeyFromMillis(millis)
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

