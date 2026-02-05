package com.semester7.quatet.ui.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.semester7.quatet.databinding.ItemSortOptionBinding
import com.semester7.quatet.databinding.SheetSortProductBinding
import com.semester7.quatet.viewmodel.ProductViewModel

class ProductSortSheet : BottomSheetDialogFragment() {
    private lateinit var binding: SheetSortProductBinding
    private val viewModel: ProductViewModel by activityViewModels()

    // Khởi tạo binding (Kết nối với file XML)
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = SheetSortProductBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Khởi tạo các hàng sort
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup nội dung
        setupSortItem(binding.layoutSortDefault, "Mặc định", "default")
        setupSortItem(binding.layoutSortPriceAsc, "Giá thấp đến cao", "price_asc")
        setupSortItem(binding.layoutSortPriceDesc, "Giá cao đến thấp", "price_desc")
        setupSortItem(binding.layoutSortPopular, "A - Z", "name_asc")
        setupSortItem(binding.layoutSortNewest, "Z - A", "name_desc")
    }

    // Setup text, key và event
    private fun setupSortItem(layout: View, title: String, sortKey: String) {
        // Binding item_sort_option
        val itemBinding = ItemSortOptionBinding.bind(layout)
        itemBinding.tvSortTitle.text = title

        // Nếu đang là sortKey hiện tại thì hiện tích xanh
        if (viewModel.getCurrentSort() == sortKey) {
            itemBinding.imgSelected.visibility = View.VISIBLE
        }

        layout.setOnClickListener {
            viewModel.updateFilters(sort = sortKey)
            dismiss()
        }
    }
}