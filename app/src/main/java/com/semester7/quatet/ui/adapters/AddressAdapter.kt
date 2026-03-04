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
            tvAddress.text = item.addressLine

            val customerInfo = buildString {
                if (!item.customername.isNullOrBlank()) append(item.customername)
                if (!item.customerphone.isNullOrBlank()) {
                    if (isNotEmpty()) append(" - ")
                    append(item.customerphone)
                }
                if (!item.customeremail.isNullOrBlank()) {
                    if (isNotEmpty()) append("\n")
                    append(item.customeremail)
                }
            }

            tvCustomerInfo.text = if (customerInfo.isBlank()) {
                "Thông tin người nhận chưa cập nhật"
            } else {
                customerInfo
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
