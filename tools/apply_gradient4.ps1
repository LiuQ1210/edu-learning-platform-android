# 把剩余页面的纯色底统一换成渐变 Brush。逐个文件精确替换 + 补 import。
$ErrorActionPreference = 'Stop'
$root = 'D:\Android\edu-learning-platform-android-main\app\src\main\java\com\github\learningplatform\ui'

function Read-Lf([string]$p) { return ([System.IO.File]::ReadAllText($p, [System.Text.Encoding]::UTF8)) -replace "`r`n", "`n" }
function Write-Lf([string]$p, [string]$t) { [System.IO.File]::WriteAllText($p, $t, (New-Object System.Text.UTF8Encoding($false))) }

$solid = 'background(MaterialTheme.colorScheme.background)'

# strong=false 给长列表页；strong=true 给内容少的页面
$targets = @(
  @{ f = "$root\mine\ProfileScreen.kt";            strong = 'true'  },
  @{ f = "$root\mine\SettingsScreen.kt";           strong = 'true'  },
  @{ f = "$root\mine\EditProfileScreen.kt";        strong = 'true'  },
  @{ f = "$root\article\ArticleEditScreen.kt";     strong = 'true'  },
  @{ f = "$root\course\CourseDetailScreen.kt";     strong = 'true'  },
  @{ f = "$root\course\CategoryAllScreen.kt";      strong = 'true'  },
  @{ f = "$root\course\SearchScreen.kt";           strong = 'true'  },
  @{ f = "$root\checkin\CheckinTaskScreen.kt";     strong = 'true'  },
  @{ f = "$root\checkin\CheckinRecordScreen.kt";   strong = 'true'  },
  @{ f = "$root\todo\TodosScreen.kt";              strong = 'true'  },
  @{ f = "$root\note\NotesScreen.kt";              strong = 'true'  }
)

foreach ($tg in $targets) {
  $p = $tg['f']
  if (-not (Test-Path $p)) { Write-Output ("  跳过（不存在）: " + $p); continue }
  $name = Split-Path $p -Leaf
  $t = Read-Lf $p
  $n = 0
  while ($t.Contains($solid)) { $t = $t.Replace($solid, "background(pageGradientBrush(strong = $($tg['strong'])))"); $n++ }

  # CategoryTabs 等共用组件里的纯色也可能被误换，这里只处理本文件，无需额外判断

  if ($n -gt 0 -and -not $t.Contains('import com.github.learningplatform.ui.theme.pageGradientBrush')) {
    $lines = $t.Split("`n")
    $out = New-Object System.Collections.Generic.List[string]
    $done = $false
    foreach ($l in $lines) {
      $out.Add($l)
      if (-not $done -and $l -like 'import com.github.learningplatform.ui.theme.*') {
        $out.Add('import com.github.learningplatform.ui.theme.pageGradientBrush')
        $done = $true
      }
    }
    if ($done) { $t = $out -join "`n" } else { Write-Output ("    IMPORT ANCHOR MISS: " + $name) }
  }
  Write-Lf $p $t
  Write-Output ("  {0,-32} 换 {1} 处" -f $name, $n)
}

Write-Output ''
Write-Output '=== 复查 ==='
Get-ChildItem -Recurse -File -Path $root -Filter *.kt | ForEach-Object {
  $t = Read-Lf $_.FullName
  $c = ([regex]::Matches($t, [regex]::Escape($solid))).Count
  if ($c -gt 0) { Write-Output ("  仍有纯色整屏底 {0} 处: {1}" -f $c, $_.Name) }
}
Write-Output '  复查结束'
