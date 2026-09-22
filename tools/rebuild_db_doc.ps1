# 从原始 v3 库设计重建「完整版」。
$ErrorActionPreference = 'Stop'

$dbSrc  = 'C:\Users\23743\.dsh\attachments\v1\files\9e\9ee384fb50f5d2d63c3647d81868d989e453455c45aa2ea406f0e1cae3a75d8b\完整数据库设计_全模块_v3.md'
$outDir = 'D:\Android\edu-learning-platform-android-main\docs'
New-Item -ItemType Directory -Force -Path $outDir | Out-Null

$db = [System.IO.File]::ReadAllText($dbSrc, [System.Text.Encoding]::UTF8)

function Sub([string]$t, [string]$old, [string]$new) {
  if (-not $t.Contains($old)) { Write-Output ('  MISS: ' + $old.Substring(0,[Math]::Min(52,$old.Length)).Replace("`n",' ')); return $t }
  return $t.Replace($old, $new)
}

Write-Output '=== 数据库设计文档_完整版 ==='

# 标题与变更说明
$db = Sub $db '# 智能广告基座（学习平台）— 完整数据库设计 v3.0' '# 智能广告基座（学习平台）— 完整数据库设计'
$db = Sub $db '> **v3.0 变更说明**：统一评论/收藏/点赞/分类为多态表；课程视频采用三层结构 + 云厂商 VOD；新增 refreshToken 双 Token、搜索记录、评论举报、课程评分、用户课程关联、连续签到天数；评论支持 root_id + parent_id 三级楼中楼；分类支持多级树形。' @'
> **统一评论/收藏/点赞/分类为多态表**；课程视频采用三层结构 + 云厂商 VOD；双 Token（accessToken + refreshToken）、搜索记录、评论举报、课程评分、用户课程关联、连续签到天数；评论支持 root_id + parent_id 三级楼中楼；分类支持多级树形。
>
> **本版变化**：`user` 支持手机号/邮箱双通道注册（`username` 可空、`email` 唯一）；新增 `file_record` 统一文件登记表；补齐 `resource` 配套资源表与 `article_report` 文章举报表。
'@

# 表清单
$db = Sub $db '| 社区文章 | article / article_tag / article_tag_relation | 文章、标签、标签关联 | 3 |' '| 社区文章 | article / article_tag / article_tag_relation / article_report | 文章、标签、标签关联、文章举报 | 4 |'
$db = Sub $db '| 个人工具 | note / todo | 笔记（关联文章/视频）、待办（提醒/截止/优先级） | 2 |' @'
| 个人工具 | note / todo | 笔记（关联文章/视频）、待办（提醒/截止/优先级） | 2 |
| 文件 | file_record | 文件上传登记表（头像/封面/配图/签到照片/资源） | 1 |
| 资源下载 | resource | 课程配套资源表 | 1 |
'@
$db = Sub $db '| **合计** | | | **31 张表** |' '| **合计** | | | **34 张表** |'

# ER 图补充
$db = Sub $db '  ├──< download_record (N)           └──< user_refresh_token (N)' @'
  ├──< download_record (N)           ├──< user_refresh_token (N)
  ├──< article_report (N)            └──< file_record (N) [上传人]
  └──< comment_report (N)
'@
$db = Sub $db 'course (1) ──< user_course_rel (N)' @'
course (1) ──< user_course_rel (N)
course (1) ──< resource (N)
'@
$db = Sub $db 'comment (1) ──< comment_report (N)' @'
comment (1) ──< comment_report (N)

resource (1) ──< download_record (N)
file_record (1) ── 被 user.avatar / article.cover_url / course.cover_url / sign_record.photo_url / resource.file_url 引用
'@

# user 表：username 可空 + email 唯一 + URL 扩容
$db = Sub $db "  ``username``          VARCHAR(50)  NOT NULL                COMMENT '登录账号'," "  ``username``          VARCHAR(50)  DEFAULT NULL            COMMENT '登录账号（手机号/邮箱注册时由服务端生成 user_{user_id} 后回填）',"
$db = Sub $db "  ``avatar``            VARCHAR(255) DEFAULT NULL            COMMENT '头像URL'," "  ``avatar``            VARCHAR(512) DEFAULT NULL            COMMENT '头像URL',"
$db = Sub $db "  UNIQUE KEY ``uk_phone`` (``phone``)," "  UNIQUE KEY ``uk_phone`` (``phone``),`r`n  UNIQUE KEY ``uk_email`` (``email``),"
$db = Sub $db '| username | VARCHAR(50) | 登录账号，唯一 |' '| username | VARCHAR(50) | 登录账号，唯一。注册时由服务端生成 `user_{user_id}`，用户不可自定义 |'
$db = Sub $db '| avatar | VARCHAR(255) | 头像URL |' '| avatar | VARCHAR(512) | 头像URL |'
$db = Sub $db '| phone | VARCHAR(20) | 手机号，唯一，微信登录后需绑定 |' '| phone | VARCHAR(20) | 手机号，唯一。邮箱注册时必须写 NULL |'
$db = Sub $db '| email | VARCHAR(100) | 邮箱 |' '| email | VARCHAR(100) | 邮箱，唯一 |'

# user 表脚注补落库规则
$db = Sub $db '> **账号锁定机制**：连续5次密码登录失败' @'
> **账号标识落库规则**：`accountType=1`（手机号）写 `phone` 列、`email` 写 `NULL`；`accountType=2`（邮箱）写 `email` 列、`phone` 写 `NULL`。**未使用的一列必须写 `NULL`，不能写空串** —— MySQL 的 UNIQUE 索引允许多个 `NULL`，但只允许一个 `''`。邮箱地址禁止写入 `phone` 列（`VARCHAR(20)` 装不下，会截断或报错）。
>
> **登录查询**：`username` / `phone` / `email` 三选一命中，均需带上 `deleted = 0 AND status = 1`。
>
> **账号锁定机制**：连续5次密码登录失败
'@

# verification_code 双通道说明
$db = Sub $db '> 校验逻辑：查 target + scene 下最新一条未使用且未过期的记录，比对 code；通过后标记 used=1。同一目标同一场景 60 秒内限发一次。' @'
> **校验逻辑**：查 target + scene 下最新一条未使用且未过期的记录，比对 code；通过后标记 used=1。同一目标同一场景 60 秒内限发一次。
>
> **双通道**：`target_type=1` 走短信网关，`target_type=2` 走邮件服务；发送结果记入 `send_status` / `fail_reason`（如 SMTP 被拒原因）。校验逻辑两条通道一致，无需分表。
'@

# 第三方头像扩容
$db = Sub $db "  ``avatar``      VARCHAR(255) DEFAULT NULL            COMMENT '第三方头像（授权时获取）'," "  ``avatar``      VARCHAR(512) DEFAULT NULL            COMMENT '第三方头像（授权时获取）',"

# 分类图标扩容
$db = Sub $db "  ``icon_url``      VARCHAR(255)  DEFAULT NULL            COMMENT '分类图标URL'," "  ``icon_url``      VARCHAR(512)  DEFAULT NULL            COMMENT '分类图标URL',"
$db = Sub $db '| icon_url | VARCHAR(255) | 分类图标 |' '| icon_url | VARCHAR(512) | 分类图标 |'

# course 表 URL 扩容 + 索引 + 作者说明
$db = Sub $db "  ``cover_url``       VARCHAR(255)  NOT NULL                COMMENT '封面图URL'," "  ``cover_url``       VARCHAR(512)  NOT NULL                COMMENT '封面图URL',"
$db = Sub $db "  KEY ``idx_view_count`` (``view_count``)," "  KEY ``idx_status_view`` (``status``, ``view_count``),`r`n  KEY ``idx_view_count`` (``view_count``),"
$db = Sub $db '| cover_url | VARCHAR(255) | 封面图 |' '| cover_url | VARCHAR(512) | 封面图 |'
$db = Sub $db '> **VOD 说明**：管理员上传视频到云厂商 VOD' @'
> **作者字段**：课程无独立作者列，讲师取 `instructor_name`，内容项的 `authorId` 取 `create_by`。
>
> **VOD 说明**：管理员上传视频到云厂商 VOD
'@

# course_lesson 播放凭证路径修正 + 权限判定
$db = Sub $db '> 播放凭证接口 `/api/v1/courses/{courseId}/lessons/{lessonId}/play-info` 校验用户权限（user_course_rel 或 is_free=1），调用VOD SDK生成带时效签名URL + 动态水印（用户ID/手机号防录屏）。' '> 播放凭证接口 `/api/v1/courses/{courseId}/play-info` 校验用户权限（`user_course_rel` 存在 或 `course.is_free=1` 或 `course_lesson.is_free=1`），调用VOD SDK生成带时效签名URL + 动态水印（用户ID/手机号防录屏）。'

# article 表 URL 扩容 + 索引 + 字段表
$db = Sub $db "  ``cover_url``       VARCHAR(255)  DEFAULT NULL            COMMENT '封面图URL'," "  ``cover_url``       VARCHAR(512)  DEFAULT NULL            COMMENT '封面图URL',"
$db = Sub $db "  ``author_avatar``   VARCHAR(255)  DEFAULT NULL            COMMENT '作者头像（冗余）'," "  ``author_avatar``   VARCHAR(512)  DEFAULT NULL            COMMENT '作者头像（冗余）',"
$db = Sub $db "  KEY ``idx_status_publish`` (``status``, ``publish_time``),`r`n  KEY ``idx_top_essence`` (``is_top``, ``is_essence``)," "  KEY ``idx_status_publish`` (``status``, ``publish_time``),`r`n  KEY ``idx_status_view`` (``status``, ``view_count``),`r`n  KEY ``idx_top_essence`` (``is_top``, ``is_essence``),"
$db = Sub $db '> 文章状态机：0草稿 → 1待审核 → 2已发布 / 3驳回 → 4下架。作者可撤回已发布文章（2→0草稿）。社区文章用户可发布（UGC），视频仅后台管理员上传（PGC）。' @'
> 文章状态机：0草稿 → 1待审核 → 2已发布 / 3驳回 → 4下架。作者可撤回已发布文章（2→0草稿）。社区文章用户可发布（UGC），视频仅后台管理员上传（PGC）。
>
> 内容项列表的 `summary` 取 `summary`，为空时后端可截取 `content` 前 N 字；`authorName` 直接取冗余列 `author_name`，不必 JOIN `user`。
'@

# comment 头像扩容
$db = Sub $db "  ``user_avatar``     VARCHAR(255)  DEFAULT NULL            COMMENT '用户头像（冗余）'," "  ``user_avatar``     VARCHAR(512)  DEFAULT NULL            COMMENT '用户头像（冗余）',"

# comment_report 补处理字段
$db = Sub $db @'
  ``status``      TINYINT       NOT NULL DEFAULT 0      COMMENT '处理状态：0-待处理 1-已处理 2-已驳回',
  ``create_time`` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '举报时间',
'@ @'
  ``status``      TINYINT       NOT NULL DEFAULT 0      COMMENT '处理状态：0-待处理 1-已处理 2-已驳回',
  ``handle_remark`` VARCHAR(300) DEFAULT NULL           COMMENT '处理备注',
  ``handle_time`` DATETIME      DEFAULT NULL            COMMENT '处理时间',
  ``handle_by``   BIGINT        DEFAULT NULL            COMMENT '处理管理员ID',
  ``create_time`` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '举报时间',
'@

# favorite 列表查询说明
$db = Sub $db '> **toggle 收藏接口**：`POST /api/v1/favorites/toggle`，内部走 `INSERT ... ON DUPLICATE KEY UPDATE delete_time = IF(delete_time=0, UNIX_TIMESTAMP(), 0)`，返回最新收藏状态。唯一索引包含 delete_time，完美解决多次收藏/取消的约束报错。' @'
> **toggle 收藏接口**：`POST /api/v1/favorites/toggle`，内部走 `INSERT ... ON DUPLICATE KEY UPDATE delete_time = IF(delete_time=0, UNIX_TIMESTAMP(), 0)`，返回最新收藏状态。唯一索引包含 delete_time，解决多次收藏/取消的约束报错。
>
> 列表查询需过滤 `delete_time = 0` 才是在收藏状态。
'@

# sign_record photo_url 扩容 + 唯一键说明
$db = Sub $db "  ``photo_url``       VARCHAR(255)  DEFAULT ''              COMMENT '拍照上传图片URL（拍照签到）'," "  ``photo_url``       VARCHAR(512)  DEFAULT ''              COMMENT '拍照上传图片URL（拍照签到）',"
$db = Sub $db '> 同一任务同一用户唯一一条签到记录。签到成功后更新 user.continue_sign_day 和 user.last_sign_date。' @'
> 同一任务同一用户唯一一条签到记录（`uk_user_task`），重复签到返回 6004。签到成功后更新 `user.continue_sign_day` 和 `user.last_sign_date`。
>
> 拍照签到的 `photo_url` 来自文件上传接口（`scene=checkin_photo`）返回的 `url`。
'@

# note source_cover 扩容 + 全文索引 + 业务校验
$db = Sub $db "  ``source_cover``    VARCHAR(255)  DEFAULT NULL            COMMENT '来源封面（冗余）'," "  ``source_cover``    VARCHAR(512)  DEFAULT NULL            COMMENT '来源封面（冗余）',"
$db = Sub $db "  KEY ``idx_source`` (``source_type``, ``source_id``)`r`n) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户学习笔记表';" "  KEY ``idx_source`` (``source_type``, ``source_id``),`r`n  FULLTEXT KEY ``ft_title_content`` (``title``, ``content``) WITH PARSER ngram`r`n) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户学习笔记表';"
$db = Sub $db '> 文章详情页和视频播放页均可记笔记。视频笔记自动记录当前播放进度到 video_timestamp，便于回看时定位。' @'
> 文章详情页和视频播放页均可记笔记。视频笔记自动记录当前播放进度到 video_timestamp，便于回看时定位。
>
> **`source_id` 必须命中真实内容**：笔记必须挂在某篇文章或某门课程下，服务端需校验 `source_id` 对应记录存在（否则返回 400）。不支持「无来源的独立笔记」。关键词搜索走 `ft_title_content` 全文索引。
'@

# view_history target_cover 扩容
$db = Sub $db "  ``target_cover``    VARCHAR(255) DEFAULT NULL        COMMENT '资源封面（冗余）'," "  ``target_cover``    VARCHAR(512) DEFAULT NULL        COMMENT '资源封面（冗余）',"
$db = Sub $db '> 文章浏览和视频观看统一用此表。视频播放进度心跳上报走 MQ 异步批量更新此表（高频写入不直接落库）。' '> 文章浏览和视频观看统一用此表。视频播放进度心跳上报走 MQ 异步批量更新此表（高频写入不直接落库），`watch_duration` 累计的是每次上报的**增量**而非总位置。'

# download_record 索引
$db = Sub $db "  KEY ``idx_user_time`` (``user_id``, ``create_time``)`r`n) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='下载记录表';" "  KEY ``idx_user_time`` (``user_id``, ``create_time``),`r`n  KEY ``idx_user_type_time`` (``user_id``, ``resource_type``, ``create_time``)`r`n) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='下载记录表';"
$db = Sub $db "  ``resource_id``  BIGINT        NOT NULL                COMMENT '资源ID'," "  ``resource_id``  BIGINT        NOT NULL                COMMENT '资源ID（关联 resource.resource_id）',"

# admin_user 头像扩容
$db = Sub $db "  ``avatar``        VARCHAR(255) DEFAULT NULL            COMMENT '头像'," "  ``avatar``        VARCHAR(512) DEFAULT NULL            COMMENT '头像',"

[System.IO.File]::WriteAllText("$outDir\数据库设计文档_完整版.md", $db, (New-Object System.Text.UTF8Encoding($false)))
Write-Output ('  写出，行数: ' + ($db -split "`n").Count)
