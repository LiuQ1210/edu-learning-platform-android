package com.github.learningplatform.data.local

/**
 * 兑换奖品用的收货信息。
 *
 * 当前后端没有地址接口，先按账号保存在本机 DataStore；
 * 后续接入服务端时把读写换成接口即可，页面结构不用动。
 */
data class ShippingAddress(
    val receiver: String = "",
    val phone: String = "",
    val region: String = "",
    val detail: String = ""
) {
    val isComplete: Boolean
        get() = receiver.isNotBlank() &&
            phone.isNotBlank() &&
            region.isNotBlank() &&
            detail.isNotBlank()

    /** 地区 + 详细地址，用于单行展示 */
    val fullAddress: String
        get() = listOf(region, detail)
            .filter { it.isNotBlank() }
            .joinToString(" ")
}
