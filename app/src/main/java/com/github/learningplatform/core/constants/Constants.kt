package com.github.learningplatform.core.constants

import com.github.learningplatform.BuildConfig

/**
 * 全局常量 —— 对齐《接口设计文档 v3.0》。
 *
 * BASE_URL 不能声明为 const val：BuildConfig.BASE_URL 是 Java static final。
 */
object Constants {

    /** v3.0 URL 前缀为 /api/v1（不再是 /api/v1/app） */
    val BASE_URL: String = BuildConfig.BASE_URL

    const val CONNECT_TIMEOUT = 30L
    const val READ_TIMEOUT = 30L
    const val WRITE_TIMEOUT = 30L

    /** v3.0 分页：pageNum 默认1，pageSize 默认20 最大100 */
    const val DEFAULT_PAGE_NUM = 1
    const val DEFAULT_PAGE_SIZE = 20
    const val MAX_PAGE_SIZE = 100

    const val DATASTORE_NAME = "learn_platform_prefs"

    const val CODE_SUCCESS = 0

    // ---- 通用（HTTP 语义，见文档 1.5）----
    const val CODE_BAD_REQUEST = 400
    const val CODE_UNAUTHORIZED = 401
    const val CODE_FORBIDDEN = 403
    const val CODE_NOT_FOUND = 404
    const val CODE_TOO_MANY_REQUESTS = 429
    const val CODE_SERVER_ERROR = 500

    // ---- 认证 1001-1099 ----
    const val CODE_CAPTCHA_ERROR = 1001
    const val CODE_CAPTCHA_EXPIRED = 1002
    const val CODE_REFRESH_TOKEN_INVALID = 1003
    const val CODE_PASSWORD_ERROR = 1004
    const val CODE_ACCOUNT_NOT_EXIST = 1005
    const val CODE_ACCOUNT_LOCKED = 1007
    const val CODE_PHONE_BOUND = 1008

    /** v3.1 增补 2.2：邮箱已被注册（与 1008 区分，便于客户端把错误定位到对应输入框） */
    const val CODE_EMAIL_BOUND = 1009

    // 1010 需绑定手机号 / 1011 平台参数不合法 / 1012 第三方账号已绑定
    // 随第三方（微信）登录一并移除，不再接入。

    // ---- 文件上传（v3.1 增补 15.1）----
    // 暂借 7001-7003；若后端已把 7001+ 编给笔记/待办，请整段挪到 9101-9103
    const val CODE_FILE_TYPE_NOT_ALLOWED = 7001
    const val CODE_FILE_TOO_LARGE = 7002
    const val CODE_FILE_EMPTY = 7003

    // ---- 用户中心 2001-2099 ----
    const val CODE_ALREADY_JOINED_COURSE = 2001

    // ---- 课程视频 3001-3099 ----
    const val CODE_COURSE_NOT_FOUND = 3001
    const val CODE_COURSE_NEED_PAY = 3002
    const val CODE_NO_PLAY_PERMISSION = 3003
    const val CODE_TRANSCODING = 3004
    const val CODE_TRANSCODE_FAILED = 3005

    // ---- 社区文章 4001-4099 ----
    const val CODE_ARTICLE_NOT_FOUND = 4001

    // ---- 互动 5001-5099 ----
    const val CODE_COMMENT_REPORTED = 5001

    // ---- 签到 6001-6099 ----
    const val CODE_NOT_IN_CHECKIN_TIME = 6001
    const val CODE_OUT_OF_RANGE = 6002
    const val CODE_GESTURE_MISMATCH = 6003
    const val CODE_ALREADY_CHECKED_IN = 6004
    const val CODE_CHECKIN_TASK_NOT_FOUND = 6005

    // ---- 搜索分类 8001-8099 ----
    const val CODE_PARENT_CATEGORY_NOT_FOUND = 8001
    const val CODE_CATEGORY_HAS_CHILDREN = 8002

    // ---- 下载广告 9001-9099 ----
    const val CODE_NO_DOWNLOAD_PERMISSION = 9001
    const val CODE_RESOURCE_NOT_FOUND = 9002

    // ---- 请求头 ----
    const val HEADER_AUTHORIZATION = "Authorization"
    const val HEADER_IDEMPOTENT = "X-Idempotent-Token"
    const val HEADER_TRACE_ID = "X-Trace-Id"
    const val BEARER_PREFIX = "Bearer "

    // ---- 多态目标类型（统一互动模块）----
    const val TARGET_ARTICLE = 1
    const val TARGET_COURSE = 2
    const val TARGET_COMMENT = 3
}