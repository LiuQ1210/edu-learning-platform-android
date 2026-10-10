package com.github.learningplatform.data.repository

import com.github.learningplatform.core.network.safeApiCall
import com.github.learningplatform.core.network.safeApiCallForUnit
import com.github.learningplatform.data.demo.DemoData
import com.github.learningplatform.data.demo.preview
import com.github.learningplatform.data.demo.previewUnit
import com.github.learningplatform.data.remote.ArticleApi
import com.github.learningplatform.data.remote.dto.ArticleDetailDto
import com.github.learningplatform.data.remote.dto.ArticleDto
import com.github.learningplatform.data.remote.dto.ArticleTagDto
import com.github.learningplatform.data.remote.dto.CreateArticleRequest
import com.github.learningplatform.data.remote.dto.CreateArticleResponse
import com.github.learningplatform.data.remote.dto.PageData
import com.github.learningplatform.data.remote.dto.ReportRequest
import com.github.learningplatform.data.remote.dto.UpdateArticleRequest
import javax.inject.Inject
import javax.inject.Singleton

/** 社区文章仓库（接口文档 v3.0 第七章） */
@Singleton
class ArticleRepository @Inject constructor(
    private val articleApi: ArticleApi
) {

    /** @param sort 1最新 2最热 3精华 */
    suspend fun getArticles(
        pageNum: Int = 1,
        pageSize: Int = 20,
        categoryId: Long? = null,
        tagId: Long? = null,
        sort: Int? = null
    ): PageData<ArticleDto> {
        preview { return DemoData.articlePage(pageNum, pageSize, categoryId) }
        return safeApiCall {
            articleApi.getArticles(pageNum, pageSize, categoryId, tagId, sort)
        }
    }

    suspend fun getArticleDetail(articleId: Long): ArticleDetailDto {
        preview { return DemoData.articleDetail(articleId) }
        return safeApiCall { articleApi.getArticleDetail(articleId) }
    }

    /** @param publish 0-存草稿 1-提交发布 */
    suspend fun createArticle(request: CreateArticleRequest): CreateArticleResponse {
        preview { return CreateArticleResponse(articleId = 5999L, status = request.publish) }
        return safeApiCall { articleApi.createArticle(request) }
    }

    suspend fun updateArticle(articleId: Long, request: UpdateArticleRequest) {
        previewUnit { return }
        safeApiCallForUnit { articleApi.updateArticle(articleId, request) }
    }

    suspend fun deleteArticle(articleId: Long) {
        previewUnit { return }
        safeApiCallForUnit { articleApi.deleteArticle(articleId) }
    }

    /** 我的发布。status: 0草稿 1待审核 2已发布 3驳回 4下架 */
    suspend fun getMyArticles(pageNum: Int = 1, pageSize: Int = 20, status: Int? = null): PageData<ArticleDto> {
        preview { return DemoData.myArticles(status) }
        return safeApiCall { articleApi.getMyArticles(pageNum, pageSize, status) }
    }

    suspend fun withdrawArticle(articleId: Long) {
        previewUnit { return }
        safeApiCallForUnit { articleApi.withdrawArticle(articleId) }
    }

    suspend fun getTags(): List<ArticleTagDto> {
        preview { return DemoData.articleTags }
        return safeApiCall { articleApi.getTags() }
    }

    suspend fun reportArticle(articleId: Long, request: ReportRequest) {
        previewUnit { return }
        safeApiCallForUnit { articleApi.reportArticle(articleId, request) }
    }
}
