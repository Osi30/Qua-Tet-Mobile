package com.semester7.quatet.ui.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.semester7.quatet.databinding.ActivityOrderHistoryBinding

class OrderHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOrderHistoryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrderHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        BottomTabNavigator.setup(this, BottomTabNavigator.Tab.ORDER_HISTORY)
    }
}
