package com.github.learningplatform.data.remote

import com.github.learningplatform.core.network.RawTableData
import kotlinx.serialization.json.JsonObject
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * 积分商城接口（对齐后端 shopping 模块 ApiV1Shop*Controller）。
 *
 *  - 商品：`GET /api/v1/shop/product/list`、`/detail`
 *  - 分类：`GET /api/v1/shop/category/list`
 *  - 兑换：`POST /api/v1/shop/exchange`
 *  - 兑换记录：`GET /api/v1/shop/order/list`（只能看自己）
 *  - 收货地址：`GET /api/v1/shop/address/list`、`/detail`，`POST /add`、`/edit`、`/remove`
 */
interface ShopApi {

    /** 商品列表（可按分类、商品名模糊搜索，分页） */
    @GET("api/v1/shop/product/list")
    suspend fun getProductList(
        @Query("categoryId") categoryId: Long? = null,
        @Query("keyword") keyword: String? = null,
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 50
    ): Response<RawTableData>

    /** 商品详情（业务字段在 `data` 下；不存在返回 code=500） */
    @GET("api/v1/shop/product/detail")
    suspend fun getProductDetail(
        @Query("productId") productId: String
    ): Response<JsonObject>

    /** 启用中的商品分类（不分页） */
    @GET("api/v1/shop/category/list")
    suspend fun getCategoryList(): Response<RawTableData>

    /**
     * 兑换商品（表单）。
     * 返回 `{orderId, redemptionCode, productId, productName, pointsCost, balance}`。
     */
    @FormUrlEncoded
    @POST("api/v1/shop/exchange")
    suspend fun exchange(
        @Field("productId") productId: String
    ): Response<JsonObject>

    /** 我的兑换记录（按兑换时间倒序，只能看自己） */
    @GET("api/v1/shop/order/list")
    suspend fun getOrderList(
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 50
    ): Response<RawTableData>

    /** 我的收货地址列表（不分页，默认地址在前） */
    @GET("api/v1/shop/address/list")
    suspend fun getAddressList(): Response<RawTableData>

    /** 地址详情 */
    @GET("api/v1/shop/address/detail")
    suspend fun getAddressDetail(
        @Query("addressId") addressId: String
    ): Response<JsonObject>

    /** 新增地址；isDefault 传 "1" 为默认，第一条地址后端自动成为默认 */
    @FormUrlEncoded
    @POST("api/v1/shop/address/add")
    suspend fun addAddress(
        @Field("receiverName") receiverName: String,
        @Field("receiverPhone") receiverPhone: String,
        @Field("detailAddress") detailAddress: String,
        @Field("isDefault") isDefault: String
    ): Response<JsonObject>

    /** 修改地址；isDefault 不传保持原值，传 "0" 取消默认 */
    @FormUrlEncoded
    @POST("api/v1/shop/address/edit")
    suspend fun editAddress(
        @Field("addressId") addressId: String,
        @Field("receiverName") receiverName: String,
        @Field("receiverPhone") receiverPhone: String,
        @Field("detailAddress") detailAddress: String,
        @Field("isDefault") isDefault: String
    ): Response<JsonObject>

    /** 删除地址 */
    @FormUrlEncoded
    @POST("api/v1/shop/address/remove")
    suspend fun removeAddress(
        @Field("addressId") addressId: String
    ): Response<JsonObject>
}
