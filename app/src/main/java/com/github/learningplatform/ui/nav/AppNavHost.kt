package com.github.learningplatform.ui.nav

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.github.learningplatform.ui.article.ArticleDetailScreen
import com.github.learningplatform.ui.theme.BottomNavContainer
import com.github.learningplatform.ui.theme.BottomNavUnselected
import com.github.learningplatform.ui.article.ArticleEditScreen
import com.github.learningplatform.ui.article.MyArticlesScreen
import com.github.learningplatform.ui.article.TagArticlesScreen
import com.github.learningplatform.ui.auth.LoginScreen
import com.github.learningplatform.ui.auth.RegisterScreen
import com.github.learningplatform.ui.checkin.CheckinRecordScreen
import com.github.learningplatform.ui.checkin.CheckinTaskScreen
import com.github.learningplatform.ui.community.CommunityScreen
import com.github.learningplatform.ui.course.CategoryAllScreen
import com.github.learningplatform.ui.course.CourseDetailScreen
import com.github.learningplatform.ui.course.HomeScreen
import com.github.learningplatform.ui.course.SearchScreen
import com.github.learningplatform.ui.mine.BlogHomeScreen
import com.github.learningplatform.ui.mine.ChangePasswordScreen
import com.github.learningplatform.ui.mine.EditProfileScreen
import com.github.learningplatform.ui.mine.FavoritesScreen
import com.github.learningplatform.ui.mine.DownloadsScreen
import com.github.learningplatform.ui.mine.LikesScreen
import com.github.learningplatform.ui.mine.MyCoursesScreen
import com.github.learningplatform.ui.mine.ProfileScreen
import com.github.learningplatform.ui.mine.StudyStatsScreen
import com.github.learningplatform.ui.mine.StudyPathScreen
import com.github.learningplatform.ui.mine.SettingsScreen
import com.github.learningplatform.ui.mine.WatchHistoryScreen
import com.github.learningplatform.ui.note.NoteDetailScreen
import com.github.learningplatform.ui.note.NotesScreen
import com.github.learningplatform.ui.todo.TodosScreen
import com.github.learningplatform.ui.video.PlayerScreen
import com.github.learningplatform.ui.video.VideoScreen

/**
 * 应用根导航。
 *
 * 结构（单 NavHost + 主框架嵌套，避免子页面把底栏顶掉）：
 *   root NavHost
 *     |- login / register / checkin / checkin_record / player   （全屏，无底栏）
 *     `- main（MainScaffold：底栏 + 主 NavHost，含 4 个 Tab 及需保留底栏的子页面）
 *
 * 登录态门禁：启动时读 DataStore，已登录直接进 main，否则进 login。
 */
@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    appViewModel: AppViewModel = hiltViewModel()
) {
    val startState by appViewModel.startState.collectAsStateWithLifecycle()

    when (startState) {
        // 启动页：对齐设计稿的奶油渐变 + 柔光圆 + 品牌名
        AppStartState.LOADING -> SplashScreen()
        AppStartState.LOGGED_IN -> RootNavHost(navController, Routes.MAIN)
        AppStartState.LOGGED_OUT -> RootNavHost(navController, Routes.LOGIN)
    }
}

@Composable
private fun RootNavHost(navController: NavHostController, startDestination: String) {
    NavHost(navController = navController, startDestination = startDestination) {

        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate(Routes.REGISTER) }
            )
        }

        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }

        composable(Routes.MAIN) {
            MainScaffold(
                onOpenCheckin = { navController.navigate(Routes.CHECKIN) },
                onOpenPlayer = { courseId, lessonId ->
                    navController.navigate(Routes.player(courseId, lessonId))
                },
                // 退出登录必须回到根图的登录页，并清掉整条主框架回退栈
                onLoggedOut = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.MAIN) { inclusive = true }
                    }
                }
            )
        }

        // 签到：全屏（手势绘制 / 定位面板需要整屏空间）
        composable(Routes.CHECKIN) {
            CheckinTaskScreen(
                onBack = { navController.popBackStack() },
                onOpenRecords = { navController.navigate(Routes.CHECKIN_RECORD) }
            )
        }

        composable(Routes.CHECKIN_RECORD) {
            CheckinRecordScreen(onBack = { navController.popBackStack() })
        }

        // 播放器：全屏 + 横竖屏自适应
        composable(
            route = Routes.PLAYER,
            arguments = listOf(
                navArgument("courseId") { type = NavType.LongType },
                navArgument("lessonId") {
                    type = NavType.LongType
                    defaultValue = 0L
                }
            )
        ) { entry ->
            val courseId = entry.arguments?.getLong("courseId") ?: 0L
            val lessonId = entry.arguments?.getLong("lessonId")?.takeIf { it > 0L }
            PlayerScreen(
                courseId = courseId,
                lessonId = lessonId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

/**
 * 主框架：底部导航 + 内层 NavHost。
 *
 * 底栏的显示/隐藏由内层当前 route 决定：只有 [Routes.barRoutes] 才显示，
 * 子页面（课程详情、文章详情、我的课程……）自动隐藏，但仍然共享内层回退栈。
 */
@Composable
fun MainScaffold(
    onOpenCheckin: () -> Unit,
    onOpenPlayer: (Long, Long?) -> Unit,
    /**
     * 退出登录：必须由根 NavHost 处理。
     * 内层 navController 的图里没有登录页，用它 navigate(LOGIN) 会直接抛异常。
     */
    onLoggedOut: () -> Unit
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val currentRoute = currentDestination?.route
    val showBar = currentRoute in Routes.barRoutes

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = showBar,
                enter = slideInVertically { it },
                exit = slideOutVertically { it }
            ) {
                NavigationBar(
                    // 显式给底色：NavigationBar 默认取 surfaceContainer（带紫调的基线色）。
                    // 这里纯白底，选中态由每个 Tab 自己的 ink / wash 决定
                    containerColor = BottomNavContainer,
                    contentColor = BottomNavUnselected
                ) {
                    BottomNavItem.entries.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            // 每个 Tab 一个色对：选中用该 Tab 的墨色 + 同色系淡垫。
                            // 不共用主色蓝 —— 那会让四个 Tab 选中时长得一样，也压不住奶油底。
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = item.ink,
                                selectedTextColor = item.ink,
                                indicatorColor = item.wash,
                                unselectedIconColor = BottomNavUnselected,
                                unselectedTextColor = BottomNavUnselected
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(innerPadding)
        ) {
            // ---------------- 4 个 Tab ----------------
            composable(Routes.HOME) {
                HomeScreen(
                    onOpenSearch = { navController.navigate(Routes.search()) },
                    onOpenAllCategories = { navController.navigate(Routes.CATEGORY_ALL) },
                    onOpenCourse = { navController.navigate(Routes.courseDetail(it)) },
                )
            }

            composable(Routes.VIDEO) {
                VideoScreen(
                    onOpenCourse = { navController.navigate(Routes.courseDetail(it)) },
                    onOpenPlayer = onOpenPlayer
                )
            }

            composable(Routes.COMMUNITY) {
                CommunityScreen(
                    onOpenArticle = { navController.navigate(Routes.articleDetail(it)) },
                    onOpenPublish = { navController.navigate(Routes.ARTICLE_PUBLISH) },
                    // 社区搜索默认落到「文章」页（type=1）
                    onOpenSearch = { navController.navigate(Routes.search(type = 1)) }
                )
            }

            composable(Routes.PROFILE) {
                ProfileScreen(
                    onOpenCheckin = onOpenCheckin,
                    onOpenEditProfile = { navController.navigate(Routes.EDIT_PROFILE) },
                    onOpenMyCourses = { navController.navigate(Routes.MY_COURSES) },
                    onOpenStudyStats = { navController.navigate(Routes.STUDY_STATS) },
                    onOpenMyArticles = { navController.navigate(Routes.MY_ARTICLES) },
                    onOpenHistory = { navController.navigate(Routes.WATCH_HISTORY) },
                    onOpenFavorites = { navController.navigate(Routes.MY_FAVORITES) },
                    onOpenLikes = { navController.navigate(Routes.MY_LIKES) },
                    onOpenDownloads = { navController.navigate(Routes.MY_DOWNLOADS) },
                    onOpenNotes = { navController.navigate(Routes.NOTES) },
                    onOpenTodos = { navController.navigate(Routes.TODOS) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    onLoggedOut = onLoggedOut
                )
            }

            // ---------------- 课程 ----------------
            composable(
                route = Routes.COURSE_DETAIL,
                arguments = listOf(navArgument("courseId") { type = NavType.LongType })
            ) { entry ->
                CourseDetailScreen(
                    courseId = entry.arguments?.getLong("courseId") ?: 0L,
                    onBack = { navController.popBackStack() },
                    onOpenPlayer = onOpenPlayer
                )
            }

            composable(
                route = Routes.SEARCH,
                arguments = listOf(
                    navArgument("categoryId") {
                        type = NavType.LongType
                        defaultValue = 0L
                    },
                    navArgument("keyword") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                    navArgument("type") {
                        type = NavType.IntType
                        defaultValue = 2
                    }
                )
            ) { entry ->
                SearchScreen(
                    initialCategoryId = entry.arguments?.getLong("categoryId")?.takeIf { it > 0L },
                    initialKeyword = entry.arguments?.getString("keyword").orEmpty(),
                    initialType = entry.arguments?.getInt("type") ?: 2,
                    onBack = { navController.popBackStack() },
                    onOpenCourse = { navController.navigate(Routes.courseDetail(it)) },
                    onOpenArticle = { navController.navigate(Routes.articleDetail(it)) }
                )
            }

            composable(Routes.CATEGORY_ALL) {
                CategoryAllScreen(
                    onBack = { navController.popBackStack() },
                    // 选分类后进入带 categoryId 的搜索页（搜索页按分类列内容）
                    onOpenCategory = { navController.navigate(Routes.search(categoryId = it)) }
                )
            }

            // ---------------- 社区 ----------------
            composable(
                route = Routes.ARTICLE_DETAIL,
                arguments = listOf(navArgument("articleId") { type = NavType.LongType })
            ) { entry ->
                ArticleDetailScreen(
                    articleId = entry.arguments?.getLong("articleId") ?: 0L,
                    onBack = { navController.popBackStack() },
                    onOpenAuthor = { navController.navigate(Routes.blogHome(it)) },
                    onEdit = { navController.navigate(Routes.articleEdit(it)) },
                    onOpenTag = { navController.navigate(Routes.tagArticles(it)) }
                )
            }

            composable(Routes.ARTICLE_PUBLISH) {
                ArticleEditScreen(
                    articleId = 0L,
                    onBack = { navController.popBackStack() },
                    onPublished = { navController.popBackStack() }
                )
            }

            composable(
                route = Routes.ARTICLE_EDIT,
                arguments = listOf(navArgument("articleId") { type = NavType.LongType })
            ) { entry ->
                ArticleEditScreen(
                    articleId = entry.arguments?.getLong("articleId") ?: 0L,
                    onBack = { navController.popBackStack() },
                    onPublished = { navController.popBackStack() }
                )
            }

            composable(Routes.MY_ARTICLES) {
                MyArticlesScreen(
                    onBack = { navController.popBackStack() },
                    onOpenArticle = { navController.navigate(Routes.articleDetail(it)) },
                    onEdit = { navController.navigate(Routes.articleEdit(it)) },
                    onPublish = { navController.navigate(Routes.ARTICLE_PUBLISH) }
                )
            }

            composable(
                route = Routes.TAG_ARTICLES,
                arguments = listOf(navArgument("tagId") { type = NavType.LongType })
            ) { entry ->
                TagArticlesScreen(
                    tagId = entry.arguments?.getLong("tagId") ?: 0L,
                    onBack = { navController.popBackStack() },
                    onOpenArticle = { navController.navigate(Routes.articleDetail(it)) }
                )
            }

            composable(
                route = Routes.BLOG_HOME,
                arguments = listOf(navArgument("userId") { type = NavType.LongType })
            ) { entry ->
                BlogHomeScreen(
                    userId = entry.arguments?.getLong("userId") ?: 0L,
                    onBack = { navController.popBackStack() },
                    onOpenArticle = { navController.navigate(Routes.articleDetail(it)) }
                )
            }

            // ---------------- 我的 ----------------
            composable(Routes.EDIT_PROFILE) {
                EditProfileScreen(
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() }
                )
            }

            composable(Routes.MY_COURSES) {
                MyCoursesScreen(
                    onBack = { navController.popBackStack() },
                    onOpenCourse = { navController.navigate(Routes.courseDetail(it)) }
                )
            }

            composable(Routes.STUDY_STATS) {
                StudyStatsScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.STUDY_PATH) {
                StudyPathScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.MY_FAVORITES) {
                FavoritesScreen(
                    onBack = { navController.popBackStack() },
                    onOpenTarget = { type, id ->
                        if (type == 1) navController.navigate(Routes.articleDetail(id))
                        else navController.navigate(Routes.courseDetail(id))
                    }
                )
            }

            composable(Routes.MY_LIKES) {
                LikesScreen(
                    onBack = { navController.popBackStack() },
                    onOpenTarget = { type, id ->
                        if (type == 1) navController.navigate(Routes.articleDetail(id))
                        else navController.navigate(Routes.courseDetail(id))
                    }
                )
            }

            composable(Routes.MY_DOWNLOADS) {
                DownloadsScreen(
                    onBack = { navController.popBackStack() },
                    // 下载记录的 resourceType 与点赞的 targetType 是两套语义，
                    // 但跳转目标一致：文章详情 / 课程详情
                    onOpenTarget = { type, id ->
                        if (type == 1) navController.navigate(Routes.articleDetail(id))
                        else navController.navigate(Routes.courseDetail(id))
                    }
                )
            }

            composable(Routes.WATCH_HISTORY) {
                WatchHistoryScreen(
                    onBack = { navController.popBackStack() },
                    onOpenTarget = { type, id ->
                        if (type == 1) navController.navigate(Routes.articleDetail(id))
                        else navController.navigate(Routes.courseDetail(id))
                    }
                )
            }

            composable(Routes.NOTES) {
                NotesScreen(
                    onBack = { navController.popBackStack() },
                    onOpenNote = { navController.navigate(Routes.noteDetail(it)) }
                )
            }

            composable(
                route = Routes.NOTE_DETAIL,
                arguments = listOf(navArgument("noteId") { type = NavType.LongType })
            ) { entry ->
                NoteDetailScreen(
                    noteId = entry.arguments?.getLong("noteId") ?: 0L,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.TODOS) {
                TodosScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenChangePassword = { navController.navigate(Routes.CHANGE_PASSWORD) },
                    onLoggedOut = onLoggedOut
                )
            }

            composable(Routes.CHANGE_PASSWORD) {
                ChangePasswordScreen(
                    onBack = { navController.popBackStack() },
                    onDone = { navController.popBackStack() }
                )
            }
        }
    }
}
