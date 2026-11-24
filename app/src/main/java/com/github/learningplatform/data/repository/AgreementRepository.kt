package com.github.learningplatform.data.repository

import com.github.learningplatform.core.network.long
import com.github.learningplatform.core.network.okBody
import com.github.learningplatform.core.network.okTable
import com.github.learningplatform.core.network.payload
import com.github.learningplatform.core.network.str
import com.github.learningplatform.data.remote.AgreementApi
import com.github.learningplatform.data.remote.dto.AgreementDto
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 平台协议仓库：对接后端 v1 协议接口，供设置页「用户协议 / 隐私政策 / 关于我们」使用。
 *
 * [getAgreements] 取列表（不含正文），当前页面仍按类型逐个取详情；列表留给后续协议列表页。
 */
@Singleton
class AgreementRepository @Inject constructor(
    private val agreementApi: AgreementApi
) {

    /**
     * 协议列表（三种固定类型，按 order_num 升序，不含正文）。
     *
     * @throws com.github.learningplatform.core.network.ApiException 网络或业务异常
     */
    suspend fun getAgreements(): List<AgreementDto> =
        agreementApi.getAgreementList().okTable().rows.map {
            AgreementDto(
                agreementId = it.long("agreementId"),
                agreementType = it.str("agreementType"),
                typeLabel = it.str("typeLabel"),
                title = it.str("title"),
                updateTime = it.str("updateTime")
            )
        }

    /**
     * 按类型取协议详情。
     *
     * @param agreementType user_agreement / privacy_policy / about_us
     * @throws com.github.learningplatform.core.network.ApiException 类型非法 / 不存在 / 已停用 / 未登录
     */
    suspend fun getAgreement(agreementType: String): AgreementDto {
        val d = agreementApi.getAgreement(agreementType).okBody().payload()
        return AgreementDto(
            agreementId = d.long("agreementId"),
            agreementType = d.str("agreementType"),
            typeLabel = d.str("typeLabel"),
            title = d.str("title"),
            content = d.str("content"),
            updateTime = d.str("updateTime")
        )
    }
}
