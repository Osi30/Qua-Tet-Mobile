package com.semester7.quatet.data.local

import android.content.Context
import android.content.SharedPreferences

// Quản lý phiên đăng nhập bằng SharedPreferences
object SessionManager {

    private const val PREF_NAME = "QuaTetSession"
    private const val KEY_TOKEN = "token"
    private const val KEY_ACCOUNT_ID = "accountId"
    private const val KEY_USERNAME = "username"
    private const val KEY_EMAIL = "email"
    private const val KEY_ROLE = "role"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    // Lưu thông tin sau khi login/register thành công
    fun saveSession(
        context: Context,
        token: String,
        accountId: Int,
        username: String,
        email: String?,
        role: String?
    ) {
        getPrefs(context).edit().apply {
            putString(KEY_TOKEN, token)
            putInt(KEY_ACCOUNT_ID, accountId)
            putString(KEY_USERNAME, username)
            putString(KEY_EMAIL, email)
            putString(KEY_ROLE, role)
            apply()
        }
    }

    // Kiểm tra đã đăng nhập chưa
    fun isLoggedIn(context: Context): Boolean {
        return getPrefs(context).getString(KEY_TOKEN, null) != null
    }

    // Lấy token để gắn vào Header Authorization
    fun getToken(context: Context): String? {
        return getPrefs(context).getString(KEY_TOKEN, null)
    }

    fun getAccountId(context: Context): Int {
        return getPrefs(context).getInt(KEY_ACCOUNT_ID, -1)
    }

    fun getUsername(context: Context): String? {
        return getPrefs(context).getString(KEY_USERNAME, null)
    }

    fun getEmail(context: Context): String? {
        return getPrefs(context).getString(KEY_EMAIL, null)
    }

    fun getRole(context: Context): String? {
        return getPrefs(context).getString(KEY_ROLE, null)
    }

    // Đăng xuất: Xóa toàn bộ session
    fun clearSession(context: Context) {
        getPrefs(context).edit().clear().apply()
    }
}
