package com.github.learningplatform.ui.course

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.learningplatform.data.remote.dto.CourseDto
import com.github.learningplatform.data.repository.AppRepository
import com.github.learningplatform.data.repository.CategoryRepository
import com.github.learningplatform.data.repository.CourseRepository
import com.github.learningplatform.ui.isStaleRequest
import com.github.learningplatform.ui.readableMessage
import kotlinx.coroutines.CancellationException
import com.github.learningplatform.ui.PagedUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

const val CATEGORY_TYPE_COURSE = 2
const val CATEGORY_TYPE_ARTICLE = 1

data class HomeUiState(

    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val categories: List<Pair<Long?, String>> = emptyList(),
    val selectedCategoryId: Long? = null,
    val hotCourses: List<CourseDto> = emptyList(),
    val rankCourses: List<CourseDto> = emptyList(),
    val adSlots: List<Pair<String, String>> = emptyList(),
    val pageNum: Int = 1,
    val hasMore: Boolean = false,
    /** 分页竞态代次：reset 时 +1，响应回来时校验 */
    override val generation: Int = 0
) : PagedUiState {
    /**
     * 顶部轮播的精选课程。
     *
     * 取热度排行前 5。之所以不复用 hotCourses：那个列表会被上拉加载不断追加，
     * 轮播跟着变长会让用户滑到一半发现页数变了。
     * 单独一个固定 5 条的列表，页数稳定。
     */
    val featuredCourses: List<CourseDto>
        get() = rankCourses.take(5).ifEmpty { hotCourses.take(5) }
}

/**
 * 课程首页。
 *
 * 首页同时需要三份数据，来源不同、失败互不影响：
 *  - 分类树（4.1，匿名可访问）
 *  - 热门课程（6.1，按热门度排序）与热度排行（6.1，sort=2）
 *  - 广告位（13.1，仅取 slotCode 占位）
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val courseRepository: CourseRepository,
    private val appRepository: AppRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadCategories()
        loadAdSlots()
        loadCourses(reset = true)
    }

    fun refresh() {
        loadCategories()
        loadCourses(reset = true)
    }

    fun selectCategory(categoryId: Long?) {
        if (categoryId == _uiState.value.selectedCategoryId) return
        _uiState.value = _uiState.value.copy(selectedCategoryId = categoryId)
        loadCourses(reset = true)
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoadingMore || !state.hasMore || state.isLoading) return
        loadCourses(reset = false)
    }

    private fun loadCategories() {
        viewModelScope.launch {
            try {
                val tree = categoryRepository.getTree(CATEGORY_TYPE_COURSE)
                _uiState.value = _uiState.value.copy(
                    categories = tree.map { it.categoryId to it.categoryName }
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                // 分类树失败不阻塞首页主内容，静默降级为「仅全部」
            }
        }
    }

    private fun loadAdSlots() {
        viewModelScope.launch {
            try {
                val slots = appRepository.getAdSlots("startup")
                _uiState.value = _uiState.value.copy(
                    adSlots = slots.map { it.slotCode to it.slotName }
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                // 广告位失败不影响首页
            }
        }
    }

    private fun loadCourses(reset: Boolean) {
        val snapshot = _uiState.value
        val nextPage = if (reset) 1 else snapshot.pageNum + 1
        // reset 时开启新一轮：飞行中的旧请求回来后因代次不匹配会被丢弃
        val generation = if (reset) snapshot.generation + 1 else snapshot.generation

        viewModelScope.launch {
            _uiState.value = snapshot.copy(
                isLoading = reset,
                isLoadingMore = !reset,
                error = if (reset) null else snapshot.error,
                generation = generation
            )
            try {
                val page = courseRepository.getCourses(
                    pageNum = nextPage,
                    pageSize = 20,
                    categoryId = snapshot.selectedCategoryId,
                    sort = 1
                )
                val rank = courseRepository.getCourses(
                    pageNum = 1,
                    pageSize = 10,
                    categoryId = snapshot.selectedCategoryId,
                    sort = 2
                ).list

                // 分类在请求飞行途中被切换 → 这份结果属于上一轮
                if (_uiState.value.isStaleRequest(generation)) return@launch

                val accumulated = if (reset) page.list else snapshot.hotCourses + page.list
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    error = null,
                    hotCourses = accumulated,
                    rankCourses = rank,
                    pageNum = nextPage,
                    hasMore = accumulated.size < page.total
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                if (_uiState.value.isStaleRequest(generation)) return@launch
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    error = if (reset) e.readableMessage() else _uiState.value.error
                )
            }
        }
    }
}
