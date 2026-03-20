package com.semester7.quatet.ui.activities

import android.app.Activity
import android.content.Intent
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.view.View
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.semester7.quatet.R
import com.semester7.quatet.data.local.ChatUnreadManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object BottomTabNavigator {
    private const val CHAT_BADGE_POLL_INTERVAL_MS = 15_000L

    enum class Tab {
        STORE_LOCATION,
        CART,
        HOME,
        ORDER_HISTORY,
        USER
    }

    fun setup(activity: Activity, current: Tab) {
        activity.findViewById<LinearLayout>(R.id.navStoreLocation)?.setOnClickListener {
            open(activity, StoreLocationMapActivity::class.java)
        }
        activity.findViewById<LinearLayout>(R.id.navCart)?.setOnClickListener {
            open(activity, CartActivity::class.java)
        }
        activity.findViewById<FrameLayout>(R.id.navHome)?.setOnClickListener {
            open(activity, ProductActivity::class.java)
        }
        activity.findViewById<LinearLayout>(R.id.navOrderHistory)?.setOnClickListener {
            open(activity, OrderHistoryActivity::class.java)
        }
        activity.findViewById<LinearLayout>(R.id.navUser)?.setOnClickListener {
            open(activity, SettingsActivity::class.java)
        }

        setActiveState(activity, current)
        renderChatBadge(activity, ChatUnreadManager.hasUnread(activity))
        refreshChatBadge(activity)
        startChatBadgePolling(activity)
    }

    private fun setActiveState(activity: Activity, current: Tab) {
        val activeColor = ContextCompat.getColor(activity, android.R.color.black)
        val inactiveColor = ContextCompat.getColor(activity, android.R.color.darker_gray)
        val whiteColor = ContextCompat.getColor(activity, android.R.color.white)

        val ids = listOf(
            R.id.tvNavStoreLocation to (current == Tab.STORE_LOCATION),
            R.id.tvNavCart to (current == Tab.CART),
            R.id.tvNavHome to (current == Tab.HOME),
            R.id.tvNavOrderHistory to (current == Tab.ORDER_HISTORY),
            R.id.tvNavUser to (current == Tab.USER)
        )
        ids.forEach { (id, isActive) ->
            activity.findViewById<TextView>(id)?.setTextColor(if (isActive) activeColor else inactiveColor)
        }

        val iconIds = listOf(
            R.id.ivNavStoreLocation to (current == Tab.STORE_LOCATION),
            R.id.ivNavCart to (current == Tab.CART),
            R.id.ivNavOrderHistory to (current == Tab.ORDER_HISTORY),
            R.id.ivNavUser to (current == Tab.USER)
        )
        iconIds.forEach { (id, isActive) ->
            activity.findViewById<ImageView>(id)?.setColorFilter(if (isActive) activeColor else inactiveColor)
        }

        val homeBg = activity.findViewById<android.view.View>(R.id.viewNavHomeBg)
        val homeIcon = activity.findViewById<ImageView>(R.id.ivNavHome)
        if (current == Tab.HOME) {
            homeBg?.setBackgroundResource(R.drawable.bg_nav_home_active)
            homeIcon?.setColorFilter(whiteColor)
        } else {
            homeBg?.setBackgroundResource(R.drawable.bg_nav_home_inactive)
            homeIcon?.setColorFilter(activeColor)
        }
    }

    private fun open(activity: Activity, target: Class<out Activity>) {
        if (activity::class.java == target) return
        val intent = Intent(activity, target).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION
            )
        }
        activity.startActivity(intent)
        activity.overridePendingTransition(0, 0)
    }

    fun refreshChatBadge(activity: Activity) {
        renderChatBadge(activity, ChatUnreadManager.hasUnread(activity))
        val lifecycleActivity = activity as? ComponentActivity ?: return
        lifecycleActivity.lifecycleScope.launch {
            val unread = withContext(Dispatchers.IO) {
                ChatUnreadManager.refreshFromServer(activity.applicationContext)
            }
            renderChatBadge(activity, unread)
        }
    }

    fun setChatUnread(activity: Activity, hasUnread: Boolean) {
        ChatUnreadManager.setUnread(activity.applicationContext, hasUnread)
        renderChatBadge(activity, hasUnread)
    }

    fun renderChatBadge(activity: Activity, hasUnread: Boolean) {
        activity.findViewById<View>(R.id.viewNavUserBadge)?.visibility =
            if (hasUnread) View.VISIBLE else View.GONE
    }

    private fun startChatBadgePolling(activity: Activity) {
        val lifecycleActivity = activity as? ComponentActivity ?: return
        lifecycleActivity.lifecycleScope.launch {
            lifecycleActivity.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    val unread = withContext(Dispatchers.IO) {
                        ChatUnreadManager.refreshFromServer(activity.applicationContext)
                    }
                    renderChatBadge(activity, unread)
                    delay(CHAT_BADGE_POLL_INTERVAL_MS)
                }
            }
        }
    }
}
