package com.github.learningplatform.data.repository

import com.github.learningplatform.core.network.long
import com.github.learningplatform.core.network.okBody
import com.github.learningplatform.core.network.payload
import com.github.learningplatform.core.network.str
import com.github.learningplatform.data.remote.AgreementApi
import com.github.learningplatform.data.remote.dto.AgreementDto
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 平台协议仓库：对接后端 v1 协议详情接口，供设置页「用户协议 / 隐私政策 / 关于我们」使用。
 */
@Singleton
class AgreementRepository @Inject constructor(
    private val agreementApi: AgreementApi
) {

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
