package com.github.learningplatform.data.remote

import com.github.learningplatform.core.network.RawTableData
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * 类别接口（对齐后端 ApiV1CategoryController）。
 *
 * 课程与社区文章共用 learning_category，用 `categoryType` 区分：
 *  - `course`  课程类别
 *  - `article` 社区文章类别
 *
 * 返回 `{code, rows:[{categoryId, categoryName}], total}`（不分页，按显示顺序升序）。
 * 客户端拿到 categoryId 后再传给 `/api/v1/course/list` 或 `/api/v1/post/list`。
 */
interface CategoryApi {

    /** @param categoryType course / article（必传，后端会校验） */
    @GET("api/v1/category/list")
    suspend fun getCategories(
        @Query("categoryType") categoryType: String
    ): Response<RawTableData>
}
