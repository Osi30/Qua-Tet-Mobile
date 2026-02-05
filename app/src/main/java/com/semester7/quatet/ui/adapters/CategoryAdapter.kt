package com.semester7.quatet.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.semester7.quatet.data.model.CategoryDTO
import com.semester7.quatet.databinding.ItemCategoryFilterBinding

class CategoryAdapter(
    private var categories: List<CategoryDTO>,
    initialSelectedIds: List<Int>,
    private val onSelectionChanged: (List<Int>) -> Unit
) :
    RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    class CategoryViewHolder(val binding: ItemCategoryFilterBinding) :
        RecyclerView.ViewHolder(binding.root)

    // Lưu các ID đã chọn (không trùng lặp)
    private val selectedIds = initialSelectedIds.toMutableSet()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CategoryViewHolder {
        val binding = ItemCategoryFilterBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CategoryViewHolder(binding)
    }

    // Thêm event cập nhập selection ids
    override fun onBindViewHolder(
        holder: CategoryViewHolder,
        position: Int
    ) {
        val item = categories[position]
        holder.binding.cbCategory.text = item.categoryname

        // Tránh bị kích hoạt ngược khi RecyclerView tái sử dụng view
        holder.binding.cbCategory.setOnCheckedChangeListener(null)
        holder.binding.cbCategory.isChecked = selectedIds.contains(item.categoryid)

        holder.binding.cbCategory.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedIds.add(item.categoryid)
            } else {
                selectedIds.remove(item.categoryid)
            }
            // Thông báo danh sách ID hiện tại về Product Activity
            onSelectionChanged(selectedIds.toList())
        }
    }

    override fun getItemCount() = categories.size

    // Cập nhật dữ liệu
    fun updateData(newCategories: List<CategoryDTO>) {

        // So sánh danh sách cũ và mới
        val diffCallback = object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = categories.size
            override fun getNewListSize(): Int = newCategories.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                // So sánh ID của loại để biết có cùng 1 đối tượng không
                return categories[oldItemPosition].categoryid == newCategories[newItemPosition].categoryid
            }

            override fun areContentsTheSame(
                oldItemPosition: Int,
                newItemPosition: Int
            ): Boolean {
                // So sánh nội dung bên trong (tên loại)
                return categories[oldItemPosition] == newCategories[newItemPosition]
            }
        }

        val diffResult = DiffUtil.calculateDiff(diffCallback)

        // Cập nhật lại danh sách dữ liệu chính
        this.categories = newCategories

        // Thông báo cho Adapter các thay đổi cụ thể (thêm, xóa, sửa)
        diffResult.dispatchUpdatesTo(this)
    }

    fun setSelectedIds(ids: List<Int>) {
        selectedIds.clear()
        selectedIds.addAll(ids)

        // Thông báo cập nhập tất cả các item nhưng chỉ ở phần CheckBox
        notifyItemRangeChanged(0, categories.size, "RESET_CHECKBOX")
    }

    fun clearSelections() {
        // Xóa Ids
        selectedIds.clear()

        // Thông báo cập nhập tất cả các item nhưng chỉ ở phần CheckBox
        notifyItemRangeChanged(0, categories.size, "RESET_CHECKBOX")
    }

}