package com.semester7.quatet.ui.screens

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import coil.load
import coil.transform.RoundedCornersTransformation
import com.semester7.quatet.R
import com.semester7.quatet.data.model.ProductDetailDTO
import com.semester7.quatet.databinding.ActivityProductBinding
import com.semester7.quatet.databinding.FragmentProductDetailBinding
import com.semester7.quatet.viewmodel.ProductDetailViewModel
import java.text.NumberFormat
import java.util.Locale

class ProductDetailFragment : Fragment(R.layout.fragment_product_detail) {
    private val viewModel: ProductDetailViewModel by viewModels()
    private var _binding: FragmentProductDetailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProductDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Lấy ID sản phẩm từ Arguments (được truyền khi chuyển màn hình)
        val productId = arguments?.getInt("PRODUCT_ID") ?: -1

        // 2. Thiết lập quan sát dữ liệu (Observer)
        observeViewModel()

        // 3. Nút quay lại
        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // 4. Gọi API
        if (productId != -1) {
            viewModel.fetchProduct(productId)
        }
    }

    private fun observeViewModel() {
        viewModel.product.observe(viewLifecycleOwner) { product ->
            product?.let { displayProductDetails(it) }
        }

        // Hiển thị hoặc ẩn Progress Bar
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.layoutContent.visibility = if (isLoading) View.GONE else View.VISIBLE
        }
    }

    private fun displayProductDetails(product: ProductDetailDTO) {
        Log.d("API_DEBUG", "Id Binding Mới: ${product.productid}")

        binding.apply {
            tvProductName.text = product.productname
            tvDescription.text = product.description
            tvQuantity.text = getString(R.string.product_stock, product.totalQuantity)

            // Định dạng tiền tệ VNĐ
            val formatter = NumberFormat
                .getCurrencyInstance(
                    Locale.Builder()
                        .setLanguage("vi")
                        .setRegion("VN")
                        .build()
                )
            tvPrice.text = formatter.format(product.price)

            // Xử lý trạng thái (status)
            product.totalQuantity?.let { tvStatus.text = if (it > 0) "Còn hàng" else "Hết hàng" }
            product.totalQuantity?.let {
                tvStatus.setTextColor(
                    if (it > 0) resources.getColor(R.color.green, null)
                    else resources.getColor(R.color.red, null)
                )
            }

            // Nạp ảnh bằng Coil
            imgProduct.load(product.imageUrl) {
                crossfade(true)
                placeholder(R.drawable.ic_image_placeholder)
                error(R.drawable.ic_image_placeholder)
                transformations(RoundedCornersTransformation(16f))
            }

            // Disable nút nếu hết hàng
            product.totalQuantity?.let { btnBuyNow.isEnabled = it > 0 }
            product.totalQuantity?.let { btnAddToCart.isEnabled = it > 0 }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()

        // Tránh rò rỉ bộ nhớ
        _binding = null
    }
}