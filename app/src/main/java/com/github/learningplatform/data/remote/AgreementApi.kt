package com.github.learningplatform.data.remote

import kotlinx.serialization.json.JsonObject
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * 平台协议接口（对齐后端 ApiV1AgreementController）。
 *
 *  - `GET /api/v1/agreement/{type}` 按类型取单篇协议（含正文 HTML）
 *
 * 类型取值：user_agreement / privacy_policy / about_us；非法或已停用后端返回 code=500。
 * 列表接口 `/api/v1/agreement/list` 不含正文，设置页三项类型固定，客户端直接按类型取详情即可。
 */
interface AgreementApi {

    /** 协议详情（业务字段在 `data` 下，content 为 HTML 正文） */
    @GET("api/v1/agreement/{agreementType}")
    suspend fun getAgreement(
        @Path("agreementType") agreementType: String
    ): Response<JsonObject>
}
