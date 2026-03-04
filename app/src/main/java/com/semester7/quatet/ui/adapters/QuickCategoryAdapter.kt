package com.semester7.quatet.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.semester7.quatet.R
import com.semester7.quatet.databinding.ItemQuickCategoryBinding

data class QuickCategoryItem(
    val id: Int?,
    val label: String
)

class QuickCategoryAdapter(
    private var items: List<QuickCategoryItem>,
    private val onSelected: (Int?) -> Unit
) : RecyclerView.Adapter<QuickCategoryAdapter.QuickCategoryViewHolder>() {

    private var selectedCategoryId: Int? = null

    class QuickCategoryViewHolder(val binding: ItemQuickCategoryBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuickCategoryViewHolder {
        val binding = ItemQuickCategoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return QuickCategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: QuickCategoryViewHolder, position: Int) {
        val item = items[position]
        val isSelected = item.id == selectedCategoryId

        with(holder.binding.tvQuickCategory) {
            text = item.label
            background = ContextCompat.getDrawable(
                context,
                if (isSelected) R.drawable.bg_quick_category_active
                else R.drawable.bg_quick_category_inactive
            )
            setTextColor(
                ContextCompat.getColor(
                    context,
                    if (isSelected) android.R.color.white else R.color.gray_text
                )
            )

            setOnClickListener {
                val previousIndex = items.indexOfFirst { it.id == selectedCategoryId }
                selectedCategoryId = item.id
                if (previousIndex >= 0) notifyItemChanged(previousIndex)
                notifyItemChanged(position)
                onSelected(item.id)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<QuickCategoryItem>, selectedId: Int?) {
        items = newItems
        selectedCategoryId = selectedId
        notifyDataSetChanged()
    }
}
