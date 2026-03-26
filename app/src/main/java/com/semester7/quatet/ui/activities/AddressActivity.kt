package com.semester7.quatet.ui.activities

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.semester7.quatet.data.model.AccountAddressDTO
import com.semester7.quatet.data.model.AddressRequest
import com.semester7.quatet.databinding.ActivityAddressBinding
import com.semester7.quatet.databinding.DialogAddressFormBinding
import com.semester7.quatet.ui.adapters.AddressAdapter
import com.semester7.quatet.viewmodel.AddressViewModel

class AddressActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_PICK_ADDRESS_MODE = "extra_pick_address_mode"
        const val RESULT_NAME = "result_name"
        const val RESULT_PHONE = "result_phone"
        const val RESULT_EMAIL = "result_email"
        const val RESULT_ADDRESS = "result_address"
    }

    private lateinit var binding: ActivityAddressBinding
    private lateinit var adapter: AddressAdapter
    private val viewModel: AddressViewModel by viewModels()

    private val isPickMode: Boolean by lazy {
        intent.getBooleanExtra(EXTRA_PICK_ADDRESS_MODE, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddressBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupListeners()
        observeViewModel()
        setupModeUi()

        viewModel.fetchAddresses()
    }

    private fun setupModeUi() {
        if (!isPickMode) return
        binding.tvPickHint.visibility = View.VISIBLE
        binding.tvTitle.text = "Chọn địa chỉ giao hàng"
        binding.tvSubtitle.text = "Chọn nhanh để điền thông tin vào đơn hàng"
    }

    private fun setupRecyclerView() {
        binding.rvAddresses.layoutManager = LinearLayoutManager(this)
        adapter = AddressAdapter(
            items = emptyList(),
            isPickMode = isPickMode,
            onEdit = { showAddressDialog(it) },
            onDelete = { showDeleteConfirm(it) },
            onSetDefault = { viewModel.setDefaultAddress(it.accountAddressId) },
            onSelect = { returnSelectedAddress(it) }
        )
        binding.rvAddresses.adapter = adapter
    }

    private fun returnSelectedAddress(address: AccountAddressDTO) {
        setResult(
            RESULT_OK,
            Intent().apply {
                putExtra(RESULT_NAME, address.customername ?: "")
                putExtra(RESULT_PHONE, address.customerphone ?: "")
                putExtra(RESULT_EMAIL, address.customeremail ?: "")
                putExtra(RESULT_ADDRESS, address.addressLine)
            }
        )
        finish()
    }

    private fun setupListeners() {
        binding.ivBack.setOnClickListener { finish() }
        binding.btnAddAddress.setOnClickListener { showAddressDialog(null) }
    }

    private fun observeViewModel() {
        viewModel.addresses.observe(this) { list ->
            val addresses = list ?: emptyList()
            adapter.updateData(addresses)

            val isEmpty = addresses.isEmpty()
            binding.layoutEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
            binding.rvAddresses.visibility = if (isEmpty) View.GONE else View.VISIBLE
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnAddAddress.isEnabled = !isLoading
        }

        viewModel.errorMessage.observe(this) { message ->
            if (!message.isNullOrEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                viewModel.clearMessages()
            }
        }

        viewModel.successMessage.observe(this) { message ->
            if (!message.isNullOrEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                viewModel.clearMessages()
            }
        }
    }

    private fun showDeleteConfirm(address: AccountAddressDTO) {
        AlertDialog.Builder(this)
            .setTitle("Xóa địa chỉ")
            .setMessage("Bạn có chắc chắn muốn xóa địa chỉ '${address.label}'?")
            .setPositiveButton("Xóa") { _, _ ->
                viewModel.deleteAddress(address.accountAddressId)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun showAddressDialog(address: AccountAddressDTO?) {
        val dialogBinding = DialogAddressFormBinding.inflate(LayoutInflater.from(this))

        if (address != null) {
            dialogBinding.edtLabel.setText(address.label)
            dialogBinding.edtCustomerName.setText(address.customername ?: "")
            dialogBinding.edtCustomerPhone.setText(address.customerphone ?: "")
            dialogBinding.edtCustomerEmail.setText(address.customeremail ?: "")
            dialogBinding.edtAddressLine.setText(address.addressLine)
            dialogBinding.switchDefault.isChecked = address.isDefault
            dialogBinding.switchActive.isChecked = address.isActive
        }

        AlertDialog.Builder(this)
            .setTitle(if (address == null) "Thêm địa chỉ mới" else "Cập nhật địa chỉ")
            .setView(dialogBinding.root)
            .setNegativeButton("Hủy", null)
            .setPositiveButton(if (address == null) "Lưu" else "Cập nhật", null)
            .create()
            .also { dialog ->
                dialog.setOnShowListener {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val label = dialogBinding.edtLabel.text.toString().trim()
                        val customerName = dialogBinding.edtCustomerName.text.toString().trim().ifEmpty { null }
                        val customerPhone =
                            dialogBinding.edtCustomerPhone.text.toString().trim().ifEmpty { null }
                        val customerEmail =
                            dialogBinding.edtCustomerEmail.text.toString().trim().ifEmpty { null }
                        val addressLine = dialogBinding.edtAddressLine.text.toString().trim()
                        val isDefault = dialogBinding.switchDefault.isChecked
                        val isActive = dialogBinding.switchActive.isChecked

                        if (label.isEmpty()) {
                            dialogBinding.edtLabel.error = "Vui lòng nhập nhãn địa chỉ"
                            return@setOnClickListener
                        }

                        if (addressLine.isEmpty()) {
                            dialogBinding.edtAddressLine.error = "Vui lòng nhập địa chỉ"
                            return@setOnClickListener
                        }

                        submitAddress(
                            address = address,
                            label = label,
                            customerName = customerName,
                            customerPhone = customerPhone,
                            customerEmail = customerEmail,
                            addressLine = addressLine,
                            isDefault = isDefault,
                            isActive = isActive
                        )
                        dialog.dismiss()
                    }
                }
                dialog.show()
            }
    }

    private fun submitAddress(
        address: AccountAddressDTO?,
        label: String,
        customerName: String?,
        customerPhone: String?,
        customerEmail: String?,
        addressLine: String,
        isDefault: Boolean,
        isActive: Boolean
    ) {
        val request = AddressRequest(
            label = label,
            customername = customerName,
            customerphone = customerPhone,
            customeremail = customerEmail,
            customerName = customerName,
            customerPhone = customerPhone,
            customerEmail = customerEmail,
            addressLine = addressLine,
            latitude = null,
            longitude = null,
            isDefault = isDefault,
            isActive = isActive
        )

        if (address == null) {
            viewModel.createAddress(request)
        } else {
            viewModel.updateAddress(address.accountAddressId, request)
        }
    }
}
