<#
智能体接口冒烟测试（直连后端，不启动本地服务）

用法：
  .\agent-smoke-test.ps1
  .\agent-smoke-test.ps1 -BaseUrl http://192.168.100.3:1000 -Phone 13800001234 -Password Test123456
  .\agent-smoke-test.ps1 -SkipHeavy        # 跳过视频总结 / PPT / 执行型智能体

覆盖：
  登录 → 会话列表 → 课程问答 → 流式提交 → SQL 问答 → 执行型智能体(含步骤) →
  步骤接口 → 长期记忆增删查 → 聊天图片上传 → 视频总结 → PPT 生成 → 任务取消

说明：
  - 只依赖 Windows 自带的 PowerShell 与 curl.exe，不需要本地启动后端。
  - 每个用例打印 PASS / FAIL / INFO，脚本结束给出汇总与退出码（有 FAIL 时为 1）。
#>
param(
    [string]$BaseUrl = "http://192.168.100.3:1000",
    [string]$Phone = "13800001234",
    [string]$Password = "Test123456",
    [int]$VideoId = 208,
    [switch]$SkipHeavy
)

$ErrorActionPreference = "Stop"
$BaseUrl = $BaseUrl.TrimEnd('/')
$Token = $null
$Results = @()

function Write-Result([string]$Name, [bool]$Ok, [string]$Detail = "") {
    $tag = if ($Ok) { "PASS" } else { "FAIL" }
    $line = "[$tag] $Name"
    if ($Detail) { $line += " — $Detail" }
    Write-Host $line
    $script:Results += [pscustomobject]@{ Name = $Name; Ok = $Ok; Detail = $Detail }
}

function Write-Info([string]$Name, [string]$Detail) {
    Write-Host "[INFO] $Name — $Detail"
}

function Invoke-Json {
    param(
        [string]$Method,
        [string]$Path,
        [object]$Body,
        [switch]$NoAuth
    )
    $headers = @{ "Accept" = "application/json" }
    if (-not $NoAuth) {
        if (-not $Token) { throw "未登录，缺少 Token" }
        $headers["Authorization"] = "Bearer $Token"
    }
    $reqParams = @{
        Uri = "$BaseUrl$Path"
        Method = $Method
        Headers = $headers
        UseBasicParsing = $true
        TimeoutSec = 30
    }
    if ($null -ne $Body) {
        $reqParams["Body"] = [Text.Encoding]::UTF8.GetBytes(($Body | ConvertTo-Json -Depth 8 -Compress))
        $reqParams["ContentType"] = "application/json; charset=utf-8"
    }
    $resp = Invoke-WebRequest @reqParams
    return [Text.Encoding]::UTF8.GetString($resp.RawContentStream.ToArray()) | ConvertFrom-Json
}

function Wait-Task {
    param([long]$TaskId, [int]$TimeoutSeconds = 90)
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        $r = Invoke-Json "GET" "/api/v1/agent/tasks/$TaskId"
        if ($r.task.status -in @("SUCCESS", "FAILED", "CANCELED")) { return $r }
        Start-Sleep -Seconds 2
    }
    throw "任务 $TaskId 在 $TimeoutSeconds 秒内未结束"
}

function Ensure-Login {
    try {
        $r = Invoke-Json "POST" "/api/v1/login" @{ phone = $Phone; password = $Password } -NoAuth
    } catch {
        $r = $null
    }
    if (-not $r -or $r.code -ne 0 -or -not $r.token) {
        # 账号不存在时尝试注册（后端开启注册才有效）
        try {
            $reg = Invoke-Json "POST" "/api/v1/register" @{ phone = $Phone; password = $Password; confirmPassword = $Password } -NoAuth
            Write-Info "注册测试账号" ("code=" + $reg.code)
        } catch {
            Write-Info "注册测试账号" $_.Exception.Message
        }
        $r = Invoke-Json "POST" "/api/v1/login" @{ phone = $Phone; password = $Password } -NoAuth
    }
    if ($r.code -ne 0 -or -not $r.token) { throw "登录失败：$($r.msg)" }
    $script:Token = $r.token
    Write-Result "登录" $true "userId=$($r.userId)"
}

Write-Host "=== 智能体接口冒烟测试：$BaseUrl ==="

# 1. 登录
Ensure-Login

# 2. 会话列表
try {
    $s = Invoke-Json "GET" "/api/v1/agent/sessions?pageNum=1&pageSize=5"
    Write-Result "会话列表" ($s.code -eq 0) "total=$($s.total)"
} catch { Write-Result "会话列表" $false $_.Exception.Message }

# 3. 课程问答
$chatSessionId = $null
try {
    $c = Invoke-Json "POST" "/api/v1/agent/chat" @{ question = "这个平台的 Java 课程主要讲什么？" }
    $chatSessionId = $c.sessionId
    Write-Result "课程问答" (($c.code -eq 0) -and ($c.answer.Length -gt 0)) "answer=$($c.answer.Length)字 refs=$(@($c.refs).Count) sessionId=$($c.sessionId)"
} catch { Write-Result "课程问答" $false $_.Exception.Message }

# 4. 历史消息
if ($chatSessionId) {
    try {
        $m = Invoke-Json "GET" "/api/v1/agent/sessions/$chatSessionId/messages"
        Write-Result "会话历史" (($m.code -eq 0) -and (@($m.rows).Count -ge 2)) "messages=$(@($m.rows).Count)"
    } catch { Write-Result "会话历史" $false $_.Exception.Message }
}

# 5. 流式提交（增量事件走 WebSocket，这里校验提交返回 requestId）
try {
    $st = Invoke-Json "POST" "/api/v1/agent/chat/stream" @{ question = "用一句话说明 TCP 和 UDP 的区别" }
    Write-Result "流式提交" (($st.code -eq 0) -and ($st.requestId.Length -gt 0)) "requestId=$($st.requestId)"
} catch { Write-Result "流式提交" $false $_.Exception.Message }

# 6. SQL 问答
try {
    $q = Invoke-Json "POST" "/api/v1/agent/sql/ask" @{ question = "平台一共有多少门课程？" }
    Write-Result "SQL 问答" (($q.code -eq 0) -and ($q.answer.Length -gt 0)) "rowCount=$($q.rowCount) error=$($q.error)"
} catch { Write-Result "SQL 问答" $false $_.Exception.Message }

# 7. 执行型智能体（工具调用 + 步骤）
if (-not $SkipHeavy) {
    try {
        $a = Invoke-Json "POST" "/api/v1/agent/act" @{ goal = "查询我的学习统计，告诉我学习时长和已完成课程数"; maxSteps = 4 }
        $done = Wait-Task $a.taskId 90
        $stepsOk = @($done.steps).Count -eq 0
        if (-not $stepsOk) {
            $d = Invoke-Json "GET" "/api/v1/agent/tasks/$($a.taskId)/steps"
            $stepsOk = @($d.steps).Count -ge 1
            $thought = @($d.steps | Where-Object { $_.stepType -eq "thought" })[0]
            if ($thought -and $thought.argsJson) {
                Write-Info "思考步骤正文" $thought.argsJson
            } elseif ($thought) {
                Write-Info "思考步骤正文" '旧版本未落 content（部署新后端后应出现 {"content":"..."}）'
            }
        }
        Write-Result "执行型智能体" (($done.task.status -eq "SUCCESS") -and $stepsOk) "taskId=$($a.taskId) status=$($done.task.status) steps=$(@($done.steps).Count)"
    } catch { Write-Result "执行型智能体" $false $_.Exception.Message }

    # 8. 任务取消
    try {
        $c1 = Invoke-Json "POST" "/api/v1/agent/act" @{ goal = "把计算机网络课程的视频做一份详细总结并生成四页PPT"; maxSteps = 8 }
        Start-Sleep -Seconds 2
        $c2 = Invoke-Json "POST" "/api/v1/agent/tasks/$($c1.taskId)/cancel"
        Start-Sleep -Seconds 2
        $c3 = Invoke-Json "GET" "/api/v1/agent/act/$($c1.taskId)"
        Write-Result "任务取消" ($c3.task.status -eq "CANCELED") "taskId=$($c1.taskId) status=$($c3.task.status) currentStep=$($c3.currentStep)"
    } catch { Write-Result "任务取消" $false $_.Exception.Message }
}

# 9. 长期记忆 增 → 查 → 删
$memoryId = $null
try {
    $m1 = Invoke-Json "POST" "/api/v1/agent/memories" @{ content = "冒烟测试记忆：" + (Get-Date -Format "yyyyMMddHHmmss"); source = "smoke" }
    $memoryId = $m1.memoryId
    $m2 = Invoke-Json "GET" "/api/v1/agent/memories"
    $found = @($m2.rows | Where-Object { $_.memoryId -eq $memoryId }).Count -eq 1
    Write-Result "长期记忆" (($m1.code -eq 0) -and $found) "memoryId=$memoryId total=$($m2.total)"
    $del = Invoke-Json "DELETE" "/api/v1/agent/memories/$memoryId"
    if ($del.code -eq 0) { Write-Info "长期记忆删除" "memoryId=$memoryId" }
} catch { Write-Result "长期记忆" $false $_.Exception.Message }

# 10. 聊天图片上传
try {
    $png = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg=="
    $tmp = Join-Path $env:TEMP "agent-smoke-dot.png"
    [IO.File]::WriteAllBytes($tmp, [Convert]::FromBase64String($png))
    $raw = & curl.exe -s -X POST "$BaseUrl/api/v1/upload/image" -H "Authorization: Bearer $Token" -F "file=@$tmp;type=image/png"
    $u = $raw | ConvertFrom-Json
    Write-Result "聊天图片上传" (($u.code -eq 0) -and ($u.url.Length -gt 0)) "url=$($u.url)"
} catch { Write-Result "聊天图片上传" $false $_.Exception.Message }

# 11. 视频总结
if (-not $SkipHeavy) {
    try {
        $v = Invoke-Json "POST" "/api/v1/agent/video/summary" @{ videoId = $VideoId }
        $vd = Wait-Task $v.taskId 120
        $out = $vd.task.outputJson | ConvertFrom-Json
        Write-Result "视频总结" (($vd.task.status -eq "SUCCESS") -and ($out.summary.Length -gt 0)) "videoId=$VideoId subtitle=$($out.subtitleUrl)"
    } catch { Write-Result "视频总结" $false $_.Exception.Message }

    # 12. PPT 生成
    try {
        $p = Invoke-Json "POST" "/api/v1/agent/ppt/generate" @{ topic = "冒烟测试：计算机网络五层模型"; pages = 4 }
        $pd = Wait-Task $p.taskId 90
        $po = $pd.task.outputJson | ConvertFrom-Json
        Write-Result "PPT 生成" (($pd.task.status -eq "SUCCESS") -and ($po.url.Length -gt 0)) "pages=$($po.pageCount) url=$($po.url)"
    } catch { Write-Result "PPT 生成" $false $_.Exception.Message }
}

$failed = @($Results | Where-Object { -not $_.Ok }).Count
Write-Host ""
Write-Host ("汇总：{0} 项，通过 {1}，失败 {2}" -f $Results.Count, ($Results.Count - $failed), $failed)
if ($failed -gt 0) { exit 1 }
