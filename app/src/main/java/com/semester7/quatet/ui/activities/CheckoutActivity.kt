package com.semester7.quatet.ui.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.semester7.quatet.databinding.ActivityCheckoutBinding
import com.semester7.quatet.viewmodel.CheckoutViewModel
import java.text.NumberFormat
import java.util.Locale

class CheckoutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCheckoutBinding
    private val viewModel: CheckoutViewModel by viewModels()

    private val manageAddressLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                val data = result.data!!
                binding.edtCustomerName.setText(data.getStringExtra(AddressActivity.RESULT_NAME).orEmpty())
                binding.edtCustomerPhone.setText(data.getStringExtra(AddressActivity.RESULT_PHONE).orEmpty())
                binding.edtCustomerEmail.setText(data.getStringExtra(AddressActivity.RESULT_EMAIL).orEmpty())
                binding.edtCustomerAddress.setText(data.getStringExtra(AddressActivity.RESULT_ADDRESS).orEmpty())
            } else {
                viewModel.loadDefaultAddress()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCheckoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val totalPrice = intent.getDoubleExtra("EXTRA_TOTAL_PRICE", 0.0)
        displayOrderSummary(totalPrice)

        setupListeners()
        observeViewModel()

        viewModel.loadDefaultAddress()
    }

    private fun displayOrderSummary(totalPrice: Double) {
        val format = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
        val formattedPrice = format.format(totalPrice)

        binding.tvCheckoutSubtotal.text = formattedPrice
        binding.tvCheckoutTotal.text = formattedPrice
    }

    private fun setupListeners() {
        binding.btnManageAddress.setOnClickListener {
            manageAddressLauncher.launch(
                Intent(this, AddressActivity::class.java).apply {
                    putExtra(AddressActivity.EXTRA_PICK_ADDRESS_MODE, true)
                }
            )
        }

        binding.btnPlaceOrder.setOnClickListener {
            val name = binding.edtCustomerName.text.toString().trim()
            val phone = binding.edtCustomerPhone.text.toString().trim()
            val email = binding.edtCustomerEmail.text.toString().trim()
            val address = binding.edtCustomerAddress.text.toString().trim()
            val note = binding.edtNote.text.toString().trim()

            if (name.isEmpty() || phone.isEmpty() || email.isEmpty() || address.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin bắt buộc (*)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Log.d("CHECKOUT_UI", "Bat dau goi ViewModel xu ly Checkout...")
            viewModel.processCheckout(name, phone, email, address, note)
        }
    }

    private fun observeViewModel() {
        viewModel.defaultAddress.observe(this) { address ->
            if (address != null) {
                binding.edtCustomerName.setText(address.customername ?: "")
                binding.edtCustomerPhone.setText(address.customerphone ?: "")
                binding.edtCustomerEmail.setText(address.customeremail ?: "")
                binding.edtCustomerAddress.setText(address.addressLine)
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBarCheckout.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnPlaceOrder.isEnabled = !isLoading
        }

        viewModel.errorMessage.observe(this) { errorMsg ->
            if (!errorMsg.isNullOrEmpty()) {
                Log.e("CHECKOUT_UI", "Loi tu ViewModel: $errorMsg")
                Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
                viewModel.clearMessages()
            }
        }

        viewModel.paymentUrl.observe(this) { url ->
            if (!url.isNullOrEmpty()) {
                Log.d("CHECKOUT_UI", "Nhan duoc URL VNPay: $url")

                val currentOrderId = viewModel.createdOrderId.value ?: -1

                val intent = Intent(this, PaymentWebViewActivity::class.java)
                intent.putExtra("EXTRA_PAYMENT_URL", url)
                intent.putExtra("EXTRA_ORDER_ID", currentOrderId)
                startActivity(intent)

                finish()
            }
        }
    }
}
