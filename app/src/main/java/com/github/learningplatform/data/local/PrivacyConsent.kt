package com.github.learningplatform.data.local

import android.content.Context

/**
 * 广告 SDK 的隐私授权标记。
 *
 * 独立于账号 DataStore：退出登录会清空 DataStore，但隐私授权不该被重置。
 */
object PrivacyConsent {
    private const val PREFS_NAME = "taku_privacy"
    private const val KEY_AGREED = "agreed"

    fun isAgreed(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_AGREED, false)

    fun agree(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_AGREED, true)
            .apply()
    }
}
