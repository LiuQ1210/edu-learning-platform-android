package com.github.learningplatform.data.remote.dto

import kotlinx.serialization.Serializable

// ---------------------- 9. 签到模块 ----------------------

/** 9.1 今日待签到任务 */
@Serializable
data class CheckinTaskDto(
    val taskId: Long,
    val taskName: String = "",
    /** 1正常 2位置 3手势 4拍照 */
    val checkinType: Int = 1,
    val description: String = "",
    val targetLongitude: Double? = null,
    val targetLatitude: Double? = null,
    /** 允许半径（米） */
    val allowRadius: Int = 100,
    val gesturePattern: String? = null,
    val startTime: String = "",
    val endTime: String = "",
    /** 0-未签到 1-已签到 */
    val status: Int = 0
) {
    val type: CheckinType get() = CheckinType.from(checkinType)
    val signed: Boolean get() = status == 1
}

enum class CheckinType(val code: Int, val label: String) {
    NORMAL(1, "正常签到"),
    LOCATION(2, "位置签到"),
    GESTURE(3, "手势签到"),
    PHOTO(4, "拍照签到");

    companion object {
        fun from(code: Int): CheckinType = entries.firstOrNull { it.code == code } ?: NORMAL
    }
}

/** 9.2 提交签到 */
@Serializable
data class CheckinSubmitRequest(
    val taskId: Long,
    val longitude: Double? = null,
    val latitude: Double? = null,
    val gesturePattern: String? = null,
    val photoUrl: String? = null
)

@Serializable
data class CheckinResultDto(
    /** 1-成功 */
    val checkinStatus: Int = 1,
    val message: String = "",
    val continueSignDay: Int = 0,
    val checkinTime: String = ""
) {
    val success: Boolean get() = checkinStatus == 1
}

/** 9.3 签到记录项 */
@Serializable
data class CheckinRecordDto(
    val recordId: Long,
    val taskId: Long = 0,
    val taskName: String = "",
    val checkinType: Int = 1,
    val signTime: String = "",
    val status: Int = 1,
    val failReason: String = ""
) {
    val type: CheckinType get() = CheckinType.from(checkinType)
    val success: Boolean get() = status == 1
}