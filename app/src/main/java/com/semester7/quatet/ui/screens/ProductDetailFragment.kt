package com.semester7.quatet.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import coil.load
import coil.transform.RoundedCornersTransformation
import com.semester7.quatet.R
import com.semester7.quatet.data.local.SessionManager
import com.semester7.quatet.data.model.ProductDetailDTO
import com.semester7.quatet.databinding.FragmentProductDetailBinding
import com.semester7.quatet.ui.activities.CartActivity
import com.semester7.quatet.ui.activities.LoginActivity
import com.semester7.quatet.utils.NotificationHelper
import com.semester7.quatet.viewmodel.CartViewModel
import com.semester7.quatet.viewmodel.ProductDetailViewModel
import java.text.NumberFormat
import java.util.Locale

class ProductDetailFragment : Fragment(R.layout.fragment_product_detail) {
    private val viewModel: ProductDetailViewModel by viewModels()

    // Dùng activityViewModels() để chia sẻ chung 1 instance với ProductActivity
    private val cartViewModel: CartViewModel by activityViewModels()

    private var _binding: FragmentProductDetailBinding? = null
    private val binding get() = _binding!!

    // 1. Launcher xin quyền Notification (Bắt buộc từ Android 13+)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(requireContext(), "Bạn cần cấp quyền để hiển thị số lượng giỏ hàng trên Icon.", Toast.LENGTH_LONG).show()
        }
    }

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

        checkNotificationPermission()

        val productId = arguments?.getInt("PRODUCT_ID") ?: -1

        observeViewModel()
        observeCartViewModel()

        // Đảm bảo dữ liệu giỏ hàng luôn mới nhất khi vào màn hình
        if (SessionManager.isLoggedIn(requireContext())) {
            cartViewModel.fetchCart()
        }

        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        if (productId != -1) {
            viewModel.fetchProduct(productId)
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun observeViewModel() {
        viewModel.product.observe(viewLifecycleOwner) { product ->
            product?.let { displayProductDetails(it) }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.layoutContent.visibility = if (isLoading) View.GONE else View.VISIBLE
        }

        // Lắng nghe tín hiệu Mua Ngay thành công
        viewModel.buyNowSuccess.observe(viewLifecycleOwner) { isSuccess ->
            if (isSuccess) {
                // 1. Chuyển hướng sang màn hình Giỏ hàng
                startActivity(Intent(requireContext(), CartActivity::class.java))

                // 2. Báo cho CartViewModel tải lại giỏ hàng để cập nhật số lượng mới nhất
                cartViewModel.fetchCart()

                // 3. Reset lại trạng thái để tránh bị lỗi tự động nhảy trang khi bấm nút Back
                viewModel.resetBuyNowState()
            }
        }
    }

    private fun displayProductDetails(product: ProductDetailDTO) {
        binding.apply {
            tvProductName.text = product.productname
            tvDescription.text = product.description
            tvQuantity.text = getString(R.string.product_stock, product.totalQuantity)

            val formatter = NumberFormat.getCurrencyInstance(
                Locale.Builder().setLanguage("vi").setRegion("VN").build()
            )
            tvPrice.text = formatter.format(product.price)

            product.totalQuantity?.let {
                tvStatus.text = if (it > 0) "Còn hàng" else "Hết hàng"
                tvStatus.setTextColor(ContextCompat.getColor(requireContext(),
                    if (it > 0) R.color.green else R.color.red))

                btnBuyNow.isEnabled = it > 0
                btnAddToCart.isEnabled = it > 0
                imgAddToCart.isEnabled = it > 0 // Vô hiệu hóa icon mini nếu hết hàng
            }

            imgProduct.load(product.imageUrl) {
                crossfade(true)
                placeholder(R.drawable.ic_image_placeholder)
                error(R.drawable.ic_image_placeholder)
                transformations(RoundedCornersTransformation(16f))
            }

            // --- Sự kiện cho nút MUA NGAY ---
            btnBuyNow.setOnClickListener {
                if (!SessionManager.isLoggedIn(requireContext())) {
                    startActivity(Intent(requireContext(), LoginActivity::class.java))
                } else {
                    viewModel.buyNow(product.productid, 1)
                }
            }

            // --- Sự kiện cho nút THÊM VÀO GIỎ (To, ở Bottom Bar) ---
            btnAddToCart.setOnClickListener {
                if (!SessionManager.isLoggedIn(requireContext())) {
                    startActivity(Intent(requireContext(), LoginActivity::class.java))
                } else {
                    cartViewModel.addItem(product.productid, 1)
                }
            }

            // --- Sự kiện cho nút ICON GIỎ HÀNG MINI (Cạnh giá tiền) ---
            imgAddToCart.setOnClickListener {
                if (!SessionManager.isLoggedIn(requireContext())) {
                    startActivity(Intent(requireContext(), LoginActivity::class.java))
                } else {
                    cartViewModel.addItem(product.productid, 1)
                    Toast.makeText(requireContext(), "Đã ném vào giỏ hàng!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun observeCartViewModel() {
        cartViewModel.cart.observe(viewLifecycleOwner) { cart ->
            val count = cart?.itemCount ?: 0
            updateCartBadgeUI(count)
        }

        cartViewModel.shouldShowNotification.observe(viewLifecycleOwner) { message ->
            message?.let {
                val totalItems = cartViewModel.cart.value?.itemCount ?: 0
                if (totalItems > 0) {
                    NotificationHelper.showCartNotification(
                        requireContext(),
                        getString(R.string.app_name),
                        "Giỏ hàng hiện có $totalItems sản phẩm. $it",
                        totalItems
                    )
                }
                cartViewModel.clearMessages()
            }
        }

        cartViewModel.successMessage.observe(viewLifecycleOwner) { message ->
            if (!message.isNullOrEmpty()) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                cartViewModel.clearMessages()
            }
        }

        cartViewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            if (!error.isNullOrEmpty()) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
                cartViewModel.clearMessages()
            }
        }
    }

    private fun updateCartBadgeUI(count: Int) {
        Log.d("CART_DEBUG", "Số lượng sản phẩm hiện tại: $count")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}