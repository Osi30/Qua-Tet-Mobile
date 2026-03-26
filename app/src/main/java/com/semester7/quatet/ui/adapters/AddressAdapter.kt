package com.semester7.quatet.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.semester7.quatet.data.model.AccountAddressDTO
import com.semester7.quatet.databinding.ItemAddressBinding

class AddressAdapter(
    private var items: List<AccountAddressDTO>,
    private val isPickMode: Boolean,
    private val onEdit: (AccountAddressDTO) -> Unit,
    private val onDelete: (AccountAddressDTO) -> Unit,
    private val onSetDefault: (AccountAddressDTO) -> Unit,
    private val onSelect: (AccountAddressDTO) -> Unit
) : RecyclerView.Adapter<AddressAdapter.AddressViewHolder>() {

    class AddressViewHolder(val binding: ItemAddressBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddressViewHolder {
        val binding = ItemAddressBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AddressViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AddressViewHolder, position: Int) {
        val item = items[position]
        with(holder.binding) {
            tvLabel.text = item.label

            val addressText = item.addressLine.ifBlank { "Chưa cập nhật" }
            val nameText = item.customername?.takeIf { it.isNotBlank() } ?: "Chưa cập nhật"
            val phoneText = item.customerphone?.takeIf { it.isNotBlank() } ?: "Chưa cập nhật"
            val emailText = item.customeremail?.takeIf { it.isNotBlank() } ?: "Chưa cập nhật"

            tvAddress.text = "Địa chỉ: $addressText"
            tvCustomerInfo.text = buildString {
                append("Tên: $nameText")
                append("\n")
                append("SĐT: $phoneText")
                append("\n")
                append("Email: $emailText")
            }

            tvDefault.visibility = if (item.isDefault) View.VISIBLE else View.GONE
            layoutManageActions.visibility = View.VISIBLE
            btnChooseAddress.visibility = if (isPickMode) View.VISIBLE else View.GONE
            btnSetDefault.visibility = if (item.isDefault) View.GONE else View.VISIBLE

            btnEdit.setOnClickListener { onEdit(item) }
            btnDelete.setOnClickListener { onDelete(item) }
            btnSetDefault.setOnClickListener { onSetDefault(item) }
            btnChooseAddress.setOnClickListener { onSelect(item) }
            holder.itemView.setOnClickListener {
                if (isPickMode) onSelect(item)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<AccountAddressDTO>) {
        items = newItems
        notifyDataSetChanged()
    }
}
