package com.github.learningplatform.data.repository

import com.github.learningplatform.core.network.RawTableData
import com.github.learningplatform.core.network.bool
import com.github.learningplatform.core.network.int
import com.github.learningplatform.core.network.long
import com.github.learningplatform.core.network.okBody
import com.github.learningplatform.core.network.okTable
import com.github.learningplatform.core.network.payload
import com.github.learningplatform.core.network.str
import com.github.learningplatform.data.remote.CheckinApi
import com.github.learningplatform.data.remote.PointsApi
import com.github.learningplatform.data.remote.dto.AdFreeRewardDto
import com.github.learningplatform.data.remote.dto.CheckinCalendarDto
import com.github.learningplatform.data.remote.dto.CheckinRecordDto
import com.github.learningplatform.data.remote.dto.CheckinRulesDto
import com.github.learningplatform.data.remote.dto.CheckinTierDto
import com.github.learningplatform.data.remote.dto.DailySignResultDto
import com.github.learningplatform.data.remote.dto.DailySignStatusDto
import com.github.learningplatform.data.remote.dto.ExchangeResultDto
import com.github.learningplatform.data.remote.dto.PageData
import com.github.learningplatform.data.remote.dto.PointsAccountDto
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 每日签到 / 积分仓库（全部对接后端）。
 *
 * 签到：`/api/v1/checkin/{status,sign,calendar,records,rules}`
 * 积分：`/api/v1/points/{account,exchange/tiers,exchange}`
 *
 * 所有「今天」「是否已签」「免广告还剩多久」都由服务端算好返回，
 * 客户端不做本地时间推算（手机时间可改）。
 */
@Singleton
class CheckinRepository @Inject constructor(
    private val checkinApi: CheckinApi,
    private val pointsApi: PointsApi
) {

    suspend fun getStatus(): DailySignStatusDto {
        val d = checkinApi.getStatus().okBody().payload()
        return DailySignStatusDto(
            signedToday = d.bool("signedToday"),
            continuousDays = d.int("continuousDays"),
            points = d.int("balance"),
            totalSigns = d.int("totalDays"),
            todayPoints = d.int("todayPoints")
        )
    }

    suspend fun sign(): DailySignResultDto {
        val d = checkinApi.sign().okBody().payload()
        return DailySignResultDto(
            success = true,
            message = "签到成功，+" + d.int("points") + " 积分",
            earnedPoints = d.int("points"),
            continuousDays = d.int("continuousDays"),
            points = d.int("balance")
        )
    }

    /** 某月签到日历（year/month 不传 = 服务端当月） */
    suspend fun getCalendar(year: Int? = null, month: Int? = null): CheckinCalendarDto {
        val d = checkinApi.getCalendar(year, month).okBody().payload()
        return CheckinCalendarDto(
            year = d.int("year"),
            month = d.int("month"),
            days = ((d["days"] as? JsonArray) ?: JsonArray(emptyList()))
                .mapNotNull { (it as? kotlinx.serialization.json.JsonPrimitive)?.content?.toIntOrNull() },
            today = d.str("today"),
            signedToday = d.bool("signedToday")
        )
    }

    /** 我的签到记录（分页） */
    suspend fun getRecords(pageNum: Int = 1, pageSize: Int = 20): PageData<CheckinRecordDto> {
        val table: RawTableData = checkinApi.getRecords(pageNum, pageSize).okTable()
        val list = table.rows.map {
            CheckinRecordDto(
                checkinId = it.long("checkinId"),
                signDate = it.str("signDate"),
                points = it.int("points"),
                continuousDays = it.int("continuousDays"),
                createTime = it.str("createTime")
            )
        }
        return PageData(total = table.total, list = list)
    }

    /** 签到积分规则（档位表 + 循环/重置说明） */
    suspend fun getRules(): CheckinRulesDto {
        val d = checkinApi.getRules().okBody().payload()
        return CheckinRulesDto(
            tiers = d.objectList("tiers").map {
                CheckinTierDto(day = it.int("day"), points = it.int("points"))
            },
            loopDays = d.int("loopDays", 7),
            dailyLimit = d.int("dailyLimit", 1),
            dailyTip = d.str("dailyTip"),
            resetNote = d.str("resetNote"),
            loopNote = d.str("loopNote")
        )
    }

    /** 积分账户：余额 + 免广告权益 */
    suspend fun getAccount(): PointsAccountDto {
        val d = pointsApi.getAccount().okBody().payload()
        return PointsAccountDto(
            balance = d.int("balance"),
            adFree = d.bool("adFree"),
            adFreeExpireTime = d.str("adFreeExpireTime").ifBlank { null },
            adFreeRemainMinutes = d.int("adFreeRemainMinutes")
        )
    }

    /** 兑换档位（服务端定义，客户端不再写死） */
    suspend fun getExchangeTiers(): List<AdFreeRewardDto> {
        val d = pointsApi.getExchangeTiers().okBody().payload()
        return d.objectList("tiers").map {
            AdFreeRewardDto(
                // rewardId 存后端档位号，兑换时原样回传
                rewardId = it.int("tier").toString(),
                label = it.str("label"),
                costPoints = it.int("cost"),
                durationMinutes = it.int("minutes")
            )
        }
    }

    /** 兑换免广告（服务端扣积分 + 权益叠加） */
    suspend fun exchange(tier: Int): ExchangeResultDto {
        val d = pointsApi.exchange(tier).okBody().payload()
        val remain = d.int("adFreeRemainMinutes")
        return ExchangeResultDto(
            success = true,
            message = "兑换成功，免广告剩余 " + remain + " 分钟",
            pointsLeft = d.int("balance"),
            // 仅用于本地展示兜底；真实状态请用 getAccount() 的 adFreeRemainMinutes
            adFreeUntil = System.currentTimeMillis() + remain * 60_000L
        )
    }

    /** 兼容旧调用：rewardId 即后端档位号 */
    suspend fun exchange(reward: AdFreeRewardDto): ExchangeResultDto =
        exchange(reward.rewardId.toIntOrNull() ?: 1)
}

/** 取 JSON 数组里的对象元素（跳过 null / 非对象） */
private fun JsonObject.objectList(key: String): List<JsonObject> =
    ((this[key] as? JsonArray) ?: JsonArray(emptyList()))
        .mapNotNull { it as? JsonObject }
