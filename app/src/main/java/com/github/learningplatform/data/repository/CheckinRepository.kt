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
import com.github.learningplatform.data.remote.dto.AdRewardResultDto
import com.github.learningplatform.data.remote.dto.CheckinCalendarDto
import com.github.learningplatform.data.remote.dto.CheckinRecordDto
import com.github.learningplatform.data.remote.dto.CheckinRulesDto
import com.github.learningplatform.data.remote.dto.CheckinTierDto
import com.github.learningplatform.data.remote.dto.DailySignResultDto
import com.github.learningplatform.data.remote.dto.DailySignStatusDto
import com.github.learningplatform.data.remote.dto.PageData
import com.github.learningplatform.data.remote.dto.PointsAccountDto
import com.github.learningplatform.data.remote.dto.PointsLogDto
import com.github.learningplatform.domain.model.TaskCenterRules
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 每日签到 / 积分仓库（全部对接后端）。
 *
 * 签到：`/api/v1/checkin/{status,sign,calendar,records,rules}`
 * 积分：`/api/v1/points/account`；积分统一走积分商城兑换奖品，免广告兑换已下线。
 *
 * 所有「今天」「是否已签」都由服务端算好返回，客户端不做本地时间推算（手机时间可改）。
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

    /**
     * 积分账户：余额（`GET /api/v1/points/account`）。
     *
     * 任务中心总积分目前仍取 `/checkin/status` 的 `balance`，这里保留账户接口备用。
     */
    suspend fun getAccount(): PointsAccountDto {
        val d = pointsApi.getAccount().okBody().payload()
        return PointsAccountDto(balance = d.int("balance"))
    }

    /**
     * 积分明细（`GET /api/v1/points/logs`，分页）。
     *
     * 用户 ID 由 Token 决定，这里不传；`type` 为空表示全部来源。
     * 后端已按时间倒序返回，前端直接追加，不要再二次排序（同秒多条靠 logId 稳定）。
     */
    suspend fun getPointsLogs(
        pageNum: Int = 1,
        pageSize: Int = 20,
        type: String? = null
    ): PageData<PointsLogDto> {
        val table: RawTableData = pointsApi.getLogs(pageNum, pageSize, type).okTable()
        val list = table.rows.map {
            PointsLogDto(
                logId = it.long("logId"),
                changeAmount = it.int("changeAmount"),
                balance = it.int("balance"),
                type = it.str("type"),
                sourceLabel = it.str("sourceLabel"),
                remark = it.str("remark"),
                refId = it.long("refId"),
                createTime = it.str("createTime")
            )
        }
        return PageData(total = table.total, list = list)
    }

    /**
     * 看完广告后上报后端领取积分（`POST /api/v1/points/ad/reward`）。
     * 同一广告当天只记一次、每天最多 [TaskCenterRules.AD_DAILY_LIMIT] 条；
     * 是否入账由服务端判定，返回 [AdRewardResultDto.awardedPoints] 为 0 即未加分。
     */
    suspend fun reportAdReward(adId: String, seconds: Int): AdRewardResultDto {
        val d = pointsApi.reportAdReward(adId, seconds).okBody().payload()
        return AdRewardResultDto(
            awardedPoints = d.int("awardedPoints"),
            todayAdCount = d.int("todayAdCount"),
            todayAdLimit = d.int("todayAdLimit", TaskCenterRules.AD_DAILY_LIMIT)
        )
    }
}

/** 取 JSON 数组里的对象元素（跳过 null / 非对象） */
private fun JsonObject.objectList(key: String): List<JsonObject> =
    ((this[key] as? JsonArray) ?: JsonArray(emptyList()))
        .mapNotNull { it as? JsonObject }
