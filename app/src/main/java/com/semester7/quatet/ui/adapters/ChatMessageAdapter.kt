package com.semester7.quatet.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.semester7.quatet.data.model.ChatMessageDTO
import com.semester7.quatet.databinding.ItemChatMessageMeBinding
import com.semester7.quatet.databinding.ItemChatMessageStaffBinding

class ChatMessageAdapter(
    private val currentUserId: Int
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_ME = 1
        private const val TYPE_OTHER = 2
    }

    private val items = mutableListOf<ChatMessageDTO>()

    fun updateData(newItems: List<ChatMessageDTO>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (items[position].senderId == currentUserId) TYPE_ME else TYPE_OTHER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_ME) {
            val binding = ItemChatMessageMeBinding.inflate(inflater, parent, false)
            MeViewHolder(binding)
        } else {
            val binding = ItemChatMessageStaffBinding.inflate(inflater, parent, false)
            OtherViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        when (holder) {
            is MeViewHolder -> holder.bind(item)
            is OtherViewHolder -> holder.bind(item)
        }
    }

    override fun getItemCount(): Int = items.size

    class MeViewHolder(private val binding: ItemChatMessageMeBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ChatMessageDTO) {
            binding.tvContent.text = item.content
            binding.tvTime.text = formatTime(item.createdAt)
        }
    }

    class OtherViewHolder(private val binding: ItemChatMessageStaffBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ChatMessageDTO) {
            binding.tvContent.text = item.content
            binding.tvTime.text = formatTime(item.createdAt)
        }
    }
}

private fun formatTime(value: String?): String {
    if (value.isNullOrBlank()) return ""
    return try {
        val raw = value.replace("T", " ")
        if (raw.length >= 16) raw.substring(0, 16) else raw
    } catch (_: Exception) {
        value
    }
}

