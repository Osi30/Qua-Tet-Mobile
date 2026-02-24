package com.semester7.quatet.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.semester7.quatet.R

object NotificationHelper {
    private const val CHANNEL_ID = "cart_notification_channel"
    private const val CHANNEL_NAME = "Giỏ hàng"
    private const val CHANNEL_DESC = "Thông báo khi thêm sản phẩm vào giỏ hàng"

    // Hàm khởi tạo Channel (Gọi 1 lần duy nhất ở Activity đầu tiên hoặc Application)
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                // Tự động bật Badge (Dấu chấm đỏ trên Icon)
                setShowBadge(true)
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    // Hàm bắn thông báo
    fun showCartNotification(context: Context, title: String, message: String, count: Int) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            // Dòng quan trọng nhất để hiển thị số lượng trên App Icon Badge
            .setNumber(count)
            .setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL)

        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.notify(1001, builder.build())
    }
}