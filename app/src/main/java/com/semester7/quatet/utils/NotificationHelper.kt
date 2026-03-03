package com.semester7.quatet.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.semester7.quatet.R
import me.leolin.shortcutbadger.ShortcutBadger // Import thư viện mới

object NotificationHelper {
    private const val CHANNEL_ID = "cart_notification_channel"
    private const val CHANNEL_NAME = "Giỏ hàng"
    private const val CHANNEL_DESC = "Thông báo khi thêm sản phẩm vào giỏ hàng"

    // Hàm khởi tạo Channel (Gọi 1 lần duy nhất ở Activity đầu tiên)
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                setShowBadge(true)
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    // Hàm bắn thông báo và ép hiển thị con số
    fun showCartNotification(context: Context, title: String, message: String, count: Int) {

        // 1. SỨC MẠNH CỦA THƯ VIỆN: Ép Launcher vẽ con số lên Icon
        if (count > 0) {
            ShortcutBadger.applyCount(context, count)
        } else {
            ShortcutBadger.removeCount(context)
        }

        // 2. Vẫn tạo thông báo hệ thống bình thường để đảm bảo luồng Android chuẩn
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setNumber(count) // Vẫn giữ lại cho các máy chạy Android thuần
            .setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL)

        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Chỉ bắn thông báo nếu có sản phẩm, nếu = 0 thì không làm phiền người dùng
        if (count > 0) {
            notificationManager.notify(1001, builder.build())
        }
    }

    // Hàm dọn dẹp nhanh: Xóa sạch dấu chấm đỏ và thông báo trên thanh trạng thái
    fun clearBadge(context: Context) {
        ShortcutBadger.removeCount(context)
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(1001)
    }
}