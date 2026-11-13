package com.github.learningplatform.ui.mall

import androidx.compose.ui.graphics.Color

/**
 * 积分商城「死数据」。
 *
 * 当前后端还没有商城奖品/兑换接口，先按参考图把奖品、档位以本地数据渲染；
 * 后续接入接口时，只需把 [demoMallPrizes] 换成服务端列表，UI 结构不用动。
 */

/** 积分档位筛选 */
data class MallPriceRange(
    val label: String,
    val min: Int,
    val max: Int
) {
    fun contains(points: Int): Boolean = points in min..max
}

/** 商城奖品（展示用） */
data class MallPrize(
    val id: String,
    val title: String,
    val categoryKey: String,
    val categoryLabel: String,
    /** 所需积分 */
    val points: Int,
    /** 参考原价；优惠券类可以为空 */
    val originalPrice: String?,
    /** 占位图（没有商品图时用 emoji） */
    val emoji: String,
    /** 角标：热门 / 新品 / 限量 / 超值 */
    val tag: String?,
    /** 商品图占位渐变色 */
    val visualStart: Color,
    val visualEnd: Color
)

val mallPriceRanges: List<MallPriceRange> = listOf(
    MallPriceRange("全部", 0, Int.MAX_VALUE),
    MallPriceRange("1000 以下", 0, 999),
    MallPriceRange("1000-2000", 1000, 2000),
    MallPriceRange("2000 以上", 2001, Int.MAX_VALUE)
)

/**
 * 示例奖品。
 *
 * 选品参考了原图里的「视频会员 + 生活好物 + 优惠券」，同时换成教育平台更贴合的
 * 学习文具、课程优惠和生活周边；积分档位覆盖 300~2500，方便演示三档筛选。
 */
val demoMallPrizes: List<MallPrize> = listOf(
    MallPrize(
        id = "tencent_video_week",
        title = "腾讯视频 VIP 周卡",
        categoryKey = "video",
        categoryLabel = "视频会员",
        points = 900,
        originalPrice = "¥9.90",
        emoji = "🎬",
        tag = "热门",
        visualStart = Color(0xFFE3ECFF),
        visualEnd = Color(0xFFF7FAFF)
    ),
    MallPrize(
        id = "iqiyi_video_week",
        title = "爱奇艺黄金 VIP 周卡",
        categoryKey = "video",
        categoryLabel = "视频会员",
        points = 1000,
        originalPrice = "¥10.00",
        emoji = "🎥",
        tag = "热兑",
        visualStart = Color(0xFFDFF5EA),
        visualEnd = Color(0xFFF5FCF8)
    ),
    MallPrize(
        id = "youku_video_week",
        title = "优酷视频 VIP 周卡",
        categoryKey = "video",
        categoryLabel = "视频会员",
        points = 1000,
        originalPrice = "¥10.00",
        emoji = "📺",
        tag = null,
        visualStart = Color(0xFFFFE8D9),
        visualEnd = Color(0xFFFFF9F4)
    ),
    MallPrize(
        id = "mango_tv_week",
        title = "芒果 TV 移动影视会员周卡",
        categoryKey = "video",
        categoryLabel = "视频会员",
        points = 1000,
        originalPrice = "¥10.00",
        emoji = "🥭",
        tag = "新品",
        visualStart = Color(0xFFFFF3D6),
        visualEnd = Color(0xFFFFFBF0)
    ),
    MallPrize(
        id = "ximalaya_week",
        title = "喜马拉雅 VIP 周卡",
        categoryKey = "video",
        categoryLabel = "视频会员",
        points = 700,
        originalPrice = "¥7.00",
        emoji = "🎧",
        tag = null,
        visualStart = Color(0xFFFFE3E3),
        visualEnd = Color(0xFFFFF7F7)
    ),
    MallPrize(
        id = "study_notebook",
        title = "精品学习笔记本",
        categoryKey = "stationery",
        categoryLabel = "学习文具",
        points = 500,
        originalPrice = "¥19.90",
        emoji = "📒",
        tag = "限量",
        visualStart = Color(0xFFFFF3D6),
        visualEnd = Color(0xFFFFFBF0)
    ),
    MallPrize(
        id = "gel_pen_set",
        title = "中性笔 3 支装",
        categoryKey = "stationery",
        categoryLabel = "学习文具",
        points = 300,
        originalPrice = "¥6.90",
        emoji = "🖊️",
        tag = null,
        visualStart = Color(0xFFFDE7E3),
        visualEnd = Color(0xFFFFF7F5)
    ),
    MallPrize(
        id = "bookmark",
        title = "精美书签",
        categoryKey = "stationery",
        categoryLabel = "学习文具",
        points = 200,
        originalPrice = "¥3.90",
        emoji = "🔖",
        tag = null,
        visualStart = Color(0xFFFFF3D6),
        visualEnd = Color(0xFFFFFBF0)
    ),
    MallPrize(
        id = "course_discount",
        title = "课程 9 折优惠券",
        categoryKey = "course",
        categoryLabel = "课程优惠",
        points = 800,
        originalPrice = null,
        emoji = "🎟️",
        tag = "超值",
        visualStart = Color(0xFFDCF2F5),
        visualEnd = Color(0xFFF4FBFC)
    ),
    MallPrize(
        id = "open_course_ticket",
        title = "名师公开课兑换券",
        categoryKey = "course",
        categoryLabel = "课程优惠",
        points = 1200,
        originalPrice = null,
        emoji = "🎫",
        tag = null,
        visualStart = Color(0xFFE3ECFD),
        visualEnd = Color(0xFFF6F9FF)
    ),
    MallPrize(
        id = "study_tote_bag",
        title = "学习帆布包",
        categoryKey = "life",
        categoryLabel = "生活好物",
        points = 1500,
        originalPrice = "¥29.90",
        emoji = "🎒",
        tag = "新品",
        visualStart = Color(0xFFFDE7E3),
        visualEnd = Color(0xFFFFF7F5)
    ),
    MallPrize(
        id = "study_headphone",
        title = "蓝牙学习耳机",
        categoryKey = "life",
        categoryLabel = "生活好物",
        points = 2500,
        originalPrice = "¥99.00",
        emoji = "🎧",
        tag = "限量",
        visualStart = Color(0xFFEEE7FF),
        visualEnd = Color(0xFFF9F6FF)
    )
)
