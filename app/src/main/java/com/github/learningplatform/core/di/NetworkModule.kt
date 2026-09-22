package com.github.learningplatform.core.di

import com.github.learningplatform.core.network.RetrofitClient
import com.github.learningplatform.data.remote.AppApi
import com.github.learningplatform.data.remote.ArticleApi
import com.github.learningplatform.data.remote.AuthApi
import com.github.learningplatform.data.remote.CategoryApi
import com.github.learningplatform.data.remote.CheckinApi
import com.github.learningplatform.data.remote.CourseApi
import com.github.learningplatform.data.remote.FileApi
import com.github.learningplatform.data.remote.InteractionApi
import com.github.learningplatform.data.remote.NoteApi
import com.github.learningplatform.data.remote.ResourceApi
import com.github.learningplatform.data.remote.SearchApi
import com.github.learningplatform.data.remote.TodoApi
import com.github.learningplatform.data.remote.UserApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** 按接口文档 v3.0 的模块划分提供 API */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides @Singleton
    fun provideAuthApi(client: RetrofitClient): AuthApi = client.create()

    @Provides @Singleton
    fun provideUserApi(client: RetrofitClient): UserApi = client.create()

    @Provides @Singleton
    fun provideCategoryApi(client: RetrofitClient): CategoryApi = client.create()

    @Provides @Singleton
    fun provideSearchApi(client: RetrofitClient): SearchApi = client.create()

    @Provides @Singleton
    fun provideCourseApi(client: RetrofitClient): CourseApi = client.create()

    @Provides @Singleton
    fun provideArticleApi(client: RetrofitClient): ArticleApi = client.create()

    @Provides @Singleton
    fun provideInteractionApi(client: RetrofitClient): InteractionApi = client.create()

    @Provides @Singleton
    fun provideCheckinApi(client: RetrofitClient): CheckinApi = client.create()

    @Provides @Singleton
    fun provideNoteApi(client: RetrofitClient): NoteApi = client.create()

    @Provides @Singleton
    fun provideTodoApi(client: RetrofitClient): TodoApi = client.create()

    @Provides @Singleton
    fun provideAppApi(client: RetrofitClient): AppApi = client.create()

    /** 资源下载（12.1）*/
    @Provides @Singleton
    fun provideResourceApi(client: RetrofitClient): ResourceApi = client.create()

    /** 文件上传（v3.1 增补 15.1 / 15.2）*/
    @Provides @Singleton
    fun provideFileApi(client: RetrofitClient): FileApi = client.create()
}