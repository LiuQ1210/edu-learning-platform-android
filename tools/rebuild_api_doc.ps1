# 从原始 v3 文档重建「完整版」两份文档。
# 思路：读原始 md -> 应用修订对 -> 写出完整版。避免手抄 1800 行。
$ErrorActionPreference = 'Stop'

$apiSrc = 'C:\Users\23743\.dsh\attachments\v1\files\6a\6a0d74deaff932ffdbe0b8a0891bdb12e0f7843d731cb009969976c439c0a80c\接口设计文档_规范版_v3.md'
$dbSrc  = 'C:\Users\23743\.dsh\attachments\v1\files\9e\9ee384fb50f5d2d63c3647d81868d989e453455c45aa2ea406f0e1cae3a75d8b\完整数据库设计_全模块_v3.md'
$outDir = 'D:\Android\edu-learning-platform-android-main\docs'
New-Item -ItemType Directory -Force -Path $outDir | Out-Null

function Apply([string]$text, [array]$pairs) {
  foreach ($p in $pairs) {
    $old = $p[0]; $new = $p[1]
    if (-not $text.Contains($old)) { Write-Output ("  MISS: " + $old.Substring(0, [Math]::Min(46, $old.Length)).Replace("`n", " ")); continue }
    $text = $text.Replace($old, $new)
  }
  return $text
}

# ============================================================ 接口文档
Write-Output '=== 接口设计文档_完整版 ==='
$api = [System.IO.File]::ReadAllText($apiSrc, [System.Text.Encoding]::UTF8)
$apiPairs = @(
  @('# 智能广告基座（学习平台）— 接口设计文档 v3.0', '# 智能广告基座（学习平台）— 接口设计文档'),
  @('> **v3.0 变更**：双Token（accessToken+refreshToken）、评论游标分页、幂等Token、课程三层结构+VOD播放凭证、搜索热词、统一多态评论/收藏/点赞/分类、分类多级树形、评论举报、课程独立评分、连续签到天数、评论三级楼中楼（root_id+parent_id）。',
    '> **核心机制**：双Token（accessToken+refreshToken）、评论游标分页、幂等Token、课程三层结构+VOD播放凭证、搜索热词、统一多态评论/收藏/点赞/分类、分类多级树形、评论举报、课程独立评分、连续签到天数、评论三级楼中楼（root_id+parent_id）、文件统一上传、手机号/邮箱双通道注册。'),
  @('| 下载广告 | 9001-9099 | 下载/广告 |', "| 下载广告 | 9001-9099 | 下载/广告 |`r`n| 文件上传 | 9101-9199 | 文件类型/大小/存储异常 |"),
  @('| 429 | 请求过于频繁 | 验证码/下载限流 |', '| 429 | 请求过于频繁 | 验证码/下载/上传限流 |')
)
$api = Apply $api $apiPairs

# 1.6 / 1.7 错误码明细：插在 1.5 表之后
$errDetail = @'
| 429 | 请求过于频繁 | 验证码/下载/上传限流 |
| 500 | 系统异常 | 服务端错误 |

### 1.6 认证错误码明细

| code | 含义 |
|---|---|
| 1001 | 验证码错误 |
| 1002 | 验证码过期 |
| 1003 | refreshToken 无效/已过期/已作废 |
| 1004 | 密码错误 |
| 1005 | 账号不存在 |
| 1007 | 账号已锁定 |
| 1008 | 手机号已被注册 |
| 1009 | 邮箱已被注册 |
| 1010 | 需绑定手机号 |
| 1011 | 平台参数不合法 |
| 1012 | 第三方账号已绑定 |

### 1.7 业务错误码明细

| code | 含义 |
|---|---|
| 2001 | 已加入该课程 |
| 3001 | 课程不存在 |
| 3002 | 课程需付费 |
| 3003 | 无播放权限（未加入课程且非免费试看） |
| 3004 | 视频转码中 |
| 3005 | 视频转码失败 |
| 4001 | 文章不存在 |
| 5001 | 已举报过该内容 |
| 6001 | 不在签到时间 |
| 6002 | 超出签到范围 |
| 6003 | 手势不匹配 |
| 6004 | 已签到 |
| 6005 | 任务不存在 |
| 7001-7099 | 笔记/待办业务错误 |
| 8001 | 父分类不存在 |
| 8002 | 存在子分类不允许删除 |
| 9001 | 无下载权限 |
| 9002 | 资源不存在 |
| 9101 | 文件类型不允许 |
| 9102 | 文件超过大小上限 |
| 9103 | 文件内容为空 |
'@
$api = $api.Replace('| 429 | 请求过于频繁 | 验证码/下载/上传限流 |
| 500 | 系统异常 | 服务端错误 |', $errDetail)

# 2.2 注册改双通道
$api = Apply $api @(
  @('- **说明**: 手机号+密码+验证码注册，注册成功签发双Token', '- **说明**: 手机号/邮箱 + 密码 + 验证码注册，注册成功签发双Token'),
  @(@'
| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| phone | string | 是 | 手机号，全局唯一 |
| password | string | 是 | 密码（6-20位） |
| code | string | 是 | 短信验证码 |
| nickname | string | 否 | 昵称，不传则自动生成 |
| email | string | 否 | 邮箱 |
'@, @'
| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| account | string | 是 | 手机号或邮箱，全局唯一 |
| accountType | int | 否 | 1-手机号（默认）2-邮箱 |
| password | string | 是 | 密码（6-20位，需同时包含字母和数字） |
| code | string | 是 | 对应通道的 6 位验证码 |
| nickname | string | 否 | 昵称，不传则自动生成 |
| email | string | 否 | 邮箱。`accountType=2` 时可不传（等于 `account`） |
'@),
  @('**错误码**: 1001验证码错误 / 1002验证码过期 / 1008手机号已绑定',
    "**错误码**: 1001验证码错误 / 1002验证码过期 / 1008手机号已被注册 / 1009邮箱已被注册 / 400参数错误`r`n`r`n> **落库规则**：``accountType=1`` 时 ``account`` 写入 ``user.phone``，``email`` 列写 ``NULL``；``accountType=2`` 时 ``account`` 写入 ``user.email``，``phone`` 列写 ``NULL``。未使用的一列必须写 ``NULL`` 而非空串（唯一索引允许多个 ``NULL``，只允许一个 ``''``）。邮箱地址禁止写入 ``phone`` 列（``VARCHAR(20)`` 装不下，会截断或报错）。`r`n>`r`n> **username 处理**：注册请求不含 ``username``，服务端生成 ``user_{user_id}`` 后回填，用户不可自定义。登录标识为手机号或邮箱。"),
  @('| username | string | 是 | 用户名/手机号 |', '| username | string | 是 | 用户名 / 手机号 / 邮箱（三选一命中） |'),
  @('**错误码**: 1009第三方账号已绑定', '**错误码**: 1012第三方账号已绑定')
)

# 4.2 / 5.1 补 ContentItemDto
$api = Apply $api @(
  @(@'
**Query参数**: categoryId / type / pageNum / pageSize / sort（选填，1最新 2最热）

**响应 data**: 分页列表
'@, @'
**Query参数**:

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| categoryId | long | 是 | 分类ID |
| type | int | 是 | 1-文章 2-视频课程 |
| pageNum | int | 否 | 页码，默认1 |
| pageSize | int | 否 | 每页条数，默认20，最大100 |
| sort | int | 否 | 1-最新（create_time 倒序）2-最热（view_count 倒序） |

**响应 data**: 分页列表，列表项见 18.1 `ContentItemDto`
'@),
  @(@'
| pageNum | int | 是 | 页码 |
| pageSize | int | 是 | 每页条数 |

**响应 data**: 分页列表
'@, @'
| pageNum | int | 否 | 页码，默认1 |
| pageSize | int | 否 | 每页条数，默认20，最大100 |

**响应 data**: 分页列表，列表项见 18.1 `ContentItemDto`，其中 `type` 与请求参数一致

> 相关性优先，其次按 `publishTime` 倒序。搜索词写入 `t_search_record`，同时 `ZINCRBY search:hot:words`。
'@)
)

# 7.1 补 status
$api = $api.Replace(
  '**响应 data**: 分页列表，每项含 articleId/title/summary/coverUrl/authorId/authorName/authorAvatar/categoryName/viewCount/likeCount/commentCount/publishTime',
  "**响应 data**: 分页列表，每项含 articleId/title/summary/coverUrl/authorId/authorName/authorAvatar/categoryName/viewCount/likeCount/commentCount/status/publishTime`r`n`r`n> ``status`` 取值 0草稿 1待审核 2已发布 3驳回 4下架。公开列表只返回 ``status=2``。")

# 6.3 权限判定
$api = $api.Replace(
  '**错误码**: 3003无播放权限（未加入课程且非免费试看）/ 3004视频转码中 / 3005视频转码失败',
  "**权限判定**: 整门课程免费（``course.is_free=1``）**或**该小节免费试看（``course_lesson.is_free=1``）**或**用户已加入课程（``user_course_rel`` 存在）。`r`n`r`n**错误码**: 3003无播放权限 / 3004视频转码中 / 3005视频转码失败")

# 6.4 watchDuration 说明
$api = $api.Replace(
  '| watchDuration | int | 是 | 本次有效观看时长（秒） |',
  '| watchDuration | int | 是 | **本次**有效观看时长（秒），即距上次上报的增量，不是总时长 |')
$api = $api.Replace(
  '**响应 data**: `{}`' + "`r`n" + '`r`n' + '---' + "`r`n" + '`r`n' + '### 6.5 提交课程评分',
  '**响应 data**: `{}`' + "`r`n" + '`r`n' + '> 建议上报间隔 15-30 秒，退出播放页时补报一次。' + "`r`n" + '`r`n' + '---' + "`r`n" + '`r`n' + '### 6.5 提交课程评分')

# 7.3 categoryId 必填说明 + coverUrl 来源
$api = $api.Replace('| coverUrl | string | 否 | 封面图 |', '| coverUrl | string | 否 | 封面图，传上传接口（scene=article_cover）返回的 url |')
$api = $api.Replace('| categoryId | long | 是 | 分类ID |' + "`r`n" + '| tagIds | array | 否 | 标签ID数组 |',
                    '| categoryId | long | 是 | 分类ID（草稿也必填） |' + "`r`n" + '| tagIds | array | 否 | 标签ID数组（最多5个） |')

# 8.12 文章举报补唯一性
$api = $api.Replace(
  '- **说明**: 同一用户同一文章仅可举报一次' + "`r`n",
  '')
$api = $api.Replace(@'
**请求参数（Body）：** reason / description

**响应 data**: `{}`
'@, @'
**请求参数（Body）：** reason（枚举同 8.6）/ description

**响应 data**: `{}`

**错误码**: 5001已举报过该文章
'@)

# 9.2 拍照签到 photoUrl 来源
$api = $api.Replace('| photoUrl | string | 否 | 现场照片URL（拍照签到必传） |',
                    '| photoUrl | string | 否 | 现场照片URL（拍照签到必传，来自上传接口 scene=checkin_photo） |')
$api = $api.Replace('| gesturePattern | string | 否 | 用户绘制手势图案（手势签到必传） |',
                    '| gesturePattern | string | 否 | 用户绘制手势图案（手势签到必传，如 "0,1,2,5,8"） |')

# 10.2 sourceId 必填校验
$api = $api.Replace('| sourceId | long | 是 | 来源内容ID |', '| sourceId | long | 是 | 来源内容ID，**必须命中真实内容** |')
$api = $api.Replace('**响应 data**: `{ "noteId": 1001 }`' + "`r`n" + '`r`n' + '---' + "`r`n" + '`r`n' + '### 10.3 编辑笔记',
                    '**响应 data**: `{ "noteId": 1001 }`' + "`r`n" + '`r`n' + '**错误码**: 400来源内容不存在' + "`r`n" + '`r`n' + '> 不支持「无来源的独立笔记」：`sourceId` 需校验对应 article/course 存在。笔记入口只出现在文章详情页与视频播放页。' + "`r`n" + '`r`n' + '---' + "`r`n" + '`r`n' + '### 10.3 编辑笔记')

# 12.1 下载权限判定
$api = $api.Replace('**错误码**: 9001无下载权限 / 429下载限流 / 9002资源不存在',
                    '**权限判定**: `resource.is_public=1` 或用户已加入 `resource.course_id` 对应课程。' + "`r`n" + '`r`n' + '**错误码**: 9001无下载权限 / 429下载限流 / 9002资源不存在')

# 2.10 修改密码后作废 token
$api = $api.Replace('- **说明**: 已登录用户修改密码，需验证旧密码',
                    '- **说明**: 已登录用户修改密码，需验证旧密码。修改成功后作废该用户全部 refreshToken')

[System.IO.File]::WriteAllText("$outDir\接口设计文档_完整版.md", $api, (New-Object System.Text.UTF8Encoding($false)))
Write-Output ("  写出，行数: " + ($api -split "`n").Count)
