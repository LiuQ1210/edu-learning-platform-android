package com.github.learningplatform.data.remote.dto

/** 商城商品（后端 ShopProduct 裁剪字段） */
data class ShopProductDto(
    val productId: Long = 0L,
    val categoryId: Long = 0L,
    val productName: String = "",
    val coverUrl: String = "",
    val pointsPrice: Int = 0,
    val stock: Int = 0,
    val soldCount: Int = 0,
    val description: String = "",
    val categoryName: String = ""
)

/** 商城商品分类（后端 ShopCategory） */
data class ShopCategoryDto(
    val categoryId: Long = 0L,
    val categoryName: String = "",
    val orderNum: Int = 0
)

/** 我的兑换记录（后端 ShopOrder 裁剪字段） */
data class ShopOrderDto(
    val orderId: Long = 0L,
    val redemptionCode: String = "",
    val userId: Long = 0L,
    val userName: String = "",
    val productId: Long = 0L,
    val productName: String = "",
    val coverUrl: String = "",
    val pointsCost: Int = 0,
    val createTime: String = ""
)

/** 收货地址（后端 ShopAddress） */
data class ShopAddressDto(
    val addressId: Long = 0L,
    val receiverName: String = "",
    val receiverPhone: String = "",
    val detailAddress: String = "",
    val isDefault: Boolean = false,
    val createTime: String = ""
)

/** 兑换成功返回（后端 exchange 平铺字段） */
data class ExchangeResultDto(
    val orderId: Long = 0L,
    val redemptionCode: String = "",
    val productId: Long = 0L,
    val productName: String = "",
    val pointsCost: Int = 0,
    val balance: Int = 0
)
