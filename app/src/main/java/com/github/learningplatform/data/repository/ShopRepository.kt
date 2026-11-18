package com.github.learningplatform.data.repository

import com.github.learningplatform.core.network.int
import com.github.learningplatform.core.network.long
import com.github.learningplatform.core.network.okBody
import com.github.learningplatform.core.network.okTable
import com.github.learningplatform.core.network.payload
import com.github.learningplatform.core.network.str
import com.github.learningplatform.data.remote.ShopApi
import com.github.learningplatform.data.remote.dto.ExchangeResultDto
import com.github.learningplatform.data.remote.dto.PageData
import com.github.learningplatform.data.remote.dto.ShopAddressDto
import com.github.learningplatform.data.remote.dto.ShopCategoryDto
import com.github.learningplatform.data.remote.dto.ShopOrderDto
import com.github.learningplatform.data.remote.dto.ShopProductDto
import kotlinx.serialization.json.JsonObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 积分商城仓库：商品 / 分类 / 兑换 / 兑换记录 / 收货地址，全部对接后端 shopping 模块。
 */
@Singleton
class ShopRepository @Inject constructor(
    private val shopApi: ShopApi
) {

    /** 商品列表（可按分类、关键词搜索，分页） */
    suspend fun getProducts(
        keyword: String? = null,
        categoryId: Long? = null,
        pageNum: Int = 1,
        pageSize: Int = 50
    ): PageData<ShopProductDto> {
        val table = shopApi.getProductList(categoryId, keyword, pageNum, pageSize).okTable()
        return PageData(total = table.total, list = table.rows.map { it.toProduct() })
    }

    /** 商品详情；不存在 / 已下架由后端返回 code=500，走异常 */
    suspend fun getProductDetail(productId: Long): ShopProductDto =
        shopApi.getProductDetail(productId.toString()).okBody().payload().toProduct()

    /** 启用中的商品分类（不分页） */
    suspend fun getCategories(): List<ShopCategoryDto> {
        val table = shopApi.getCategoryList().okTable()
        return table.rows.map {
            ShopCategoryDto(
                categoryId = it.long("categoryId"),
                categoryName = it.str("categoryName"),
                orderNum = it.int("orderNum")
            )
        }
    }

    /** 兑换商品；返回兑换码、消耗积分与兑换后余额 */
    suspend fun exchange(productId: Long): ExchangeResultDto {
        val body = shopApi.exchange(productId.toString()).okBody().payload()
        return ExchangeResultDto(
            orderId = body.long("orderId"),
            redemptionCode = body.str("redemptionCode"),
            productId = body.long("productId"),
            productName = body.str("productName"),
            pointsCost = body.int("pointsCost"),
            balance = body.int("balance")
        )
    }

    /** 我的兑换记录（后端只返回当前登录用户） */
    suspend fun getOrders(
        pageNum: Int = 1,
        pageSize: Int = 50
    ): PageData<ShopOrderDto> {
        val table = shopApi.getOrderList(pageNum, pageSize).okTable()
        return PageData(total = table.total, list = table.rows.map { it.toOrder() })
    }

    /** 我的收货地址列表（默认地址在前） */
    suspend fun getAddresses(): List<ShopAddressDto> {
        val table = shopApi.getAddressList().okTable()
        return table.rows.map { it.toAddress() }
    }

    /**
     * 保存地址：`addressId` 为空走新增，非空走修改。
     *
     * @param detailAddress 地区 + 详细地址拼接后的完整地址（后端只有单字段）
     * @param isDefault 是否设为默认地址
     */
    suspend fun saveAddress(
        addressId: Long?,
        receiverName: String,
        receiverPhone: String,
        detailAddress: String,
        isDefault: Boolean
    ) {
        val defaultFlag = if (isDefault) "1" else "0"
        if (addressId == null) {
            shopApi.addAddress(receiverName, receiverPhone, detailAddress, defaultFlag).okBody()
        } else {
            shopApi.editAddress(
                addressId.toString(),
                receiverName,
                receiverPhone,
                detailAddress,
                defaultFlag
            ).okBody()
        }
    }

    /** 删除地址 */
    suspend fun removeAddress(addressId: Long) {
        shopApi.removeAddress(addressId.toString()).okBody()
    }

    private fun JsonObject.toProduct(): ShopProductDto = ShopProductDto(
        productId = long("productId"),
        categoryId = long("categoryId"),
        productName = str("productName"),
        coverUrl = str("coverUrl"),
        pointsPrice = int("pointsPrice"),
        stock = int("stock"),
        soldCount = int("soldCount"),
        description = str("description"),
        categoryName = str("categoryName")
    )

    private fun JsonObject.toOrder(): ShopOrderDto = ShopOrderDto(
        orderId = long("orderId"),
        redemptionCode = str("redemptionCode"),
        userId = long("userId"),
        userName = str("userName"),
        productId = long("productId"),
        productName = str("productName"),
        coverUrl = str("coverUrl"),
        pointsCost = int("pointsCost"),
        createTime = str("createTime")
    )

    private fun JsonObject.toAddress(): ShopAddressDto = ShopAddressDto(
        addressId = long("addressId"),
        receiverName = str("receiverName"),
        receiverPhone = str("receiverPhone"),
        detailAddress = str("detailAddress"),
        isDefault = str("isDefault") == "1",
        createTime = str("createTime")
    )
}
