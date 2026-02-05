package com.semester7.quatet.ui.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioGroup
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.slider.RangeSlider
import com.semester7.quatet.R
import com.semester7.quatet.databinding.ProductSheetFilterBinding
import com.semester7.quatet.ui.adapters.CategoryAdapter
import com.semester7.quatet.viewmodel.ProductViewModel

class ProductFilterSheet : BottomSheetDialogFragment() {
    private lateinit var binding: ProductSheetFilterBinding
    private val viewModel: ProductViewModel by activityViewModels()
    private lateinit var categoryAdapter: CategoryAdapter

    // Khởi tạo binding (Kết nối với file XML)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = ProductSheetFilterBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Khởi tạo, gắn adapter, events
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupCategoryRecyclerView()
        setupRangeSlider(view)
        observeCategories()

        viewModel.fetchCategories()

        binding.btnClose.setOnClickListener { dismiss() }
        binding.tvReset.setOnClickListener { resetFilters() }
        binding.btnApply.setOnClickListener { applyFilters() }
    }

    // Gán adapter
    private fun setupCategoryRecyclerView() {
        categoryAdapter = CategoryAdapter(
            emptyList(),
            viewModel.currentSelectedCategoryIds
        ) { selectedIds ->
            viewModel.currentSelectedCategoryIds = selectedIds
        }
        binding.rvCategories.adapter = categoryAdapter
    }

    // Khi có thay đổi -> kíc hoạt event
    private fun observeCategories() {
        viewModel.categories.observe(viewLifecycleOwner) { list ->
            if (list != null) {
                categoryAdapter.updateData(list)
                categoryAdapter.setSelectedIds(viewModel.currentSelectedCategoryIds)
            }
        }
    }

    // Event khóa hoặc mở range slider
    private fun setupRangeSlider(view: View) {
        val rgPrice = view.findViewById<RadioGroup>(R.id.rgPrice)
        val layoutCustomPrice = view.findViewById<LinearLayout>(R.id.layoutCustomPrice)
        val rangeSlider = view.findViewById<RangeSlider>(R.id.rangeSlider)
        val edtMin = view.findViewById<EditText>(R.id.edtMinPrice)
        val edtMax = view.findViewById<EditText>(R.id.edtMaxPrice)

        rgPrice.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rbCustomPrice) {
                // Khi chọn tùy chỉnh -> Mở khóa
                layoutCustomPrice.alpha = 1.0f
                rangeSlider.isEnabled = true
                edtMin.isEnabled = true
                edtMax.isEnabled = true
            } else {
                // Khi chọn các mức giá cố định -> Khóa lại
                layoutCustomPrice.alpha = 0.5f
                rangeSlider.isEnabled = false
                edtMin.isEnabled = false
                edtMax.isEnabled = false
            }
        }

        // Thêm: Cập nhật EditText khi kéo Slider
        rangeSlider.addOnChangeListener { slider, _, _ ->
            val values = slider.values
            edtMin.setText(values[0].toInt().toString())
            edtMax.setText(values[1].toInt().toString())
        }
    }

    // Cập nhâp filter
    private fun applyFilters() {
        var minPrice: Double? = null
        var maxPrice: Double? = null

        // 1. Lấy giá tiền dựa trên RadioGroup
        when (binding.rgPrice.checkedRadioButtonId) {
            R.id.rbPrice1 -> {
                minPrice = 0.0; maxPrice = 300000.0
            }

            R.id.rbPrice2 -> {
                minPrice = 300000.0; maxPrice = 500000.0
            }

            R.id.rbPrice3 -> {
                minPrice = 500000.0; maxPrice = 1000000.0
            }

            R.id.rbCustomPrice -> {
                minPrice = binding.edtMinPrice.text.toString().toDoubleOrNull()
                maxPrice = binding.edtMaxPrice.text.toString().toDoubleOrNull()
            }
        }

        // 2. Update
        viewModel.updateFilters(
            minPrice = minPrice,
            maxPrice = maxPrice
        )
        dismiss()
    }

    // Reset filter
    private fun resetFilters() {
        // A. Reset RadioGroup về mặc định
        binding.rgPrice.check(R.id.rgPrice)

        // B. Reset RangeSlider (giả sử từ 0 đến 100tr)
        binding.rangeSlider.setValues(0f, 100000000f)

        // C. Reset danh sách Category IDs
        viewModel.currentSelectedCategoryIds = emptyList()

        // D. Reset trạng thái CheckBox trong Adapter
        categoryAdapter.clearSelections()
    }
}