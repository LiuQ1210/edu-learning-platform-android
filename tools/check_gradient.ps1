# 采样已实现界面的背景渐变，与设计稿对照。
Add-Type -AssemblyName System.Drawing
$f = 'D:\Android\edu-learning-platform-android-main\docs\screenshots\01_登录页.png'
$img = [System.Drawing.Bitmap]::FromFile($f)
$w = $img.Width; $h = $img.Height
Write-Output "截图: $w x $h"

# 取最左 8% 宽度做竖向扫描（避开中间的输入框与按钮）
Write-Output ''
Write-Output '=== 登录页背景竖向扫描（最左 8% 宽）==='
$rows = @(0.02, 0.08, 0.15, 0.22, 0.30, 0.38, 0.46, 0.54, 0.62, 0.70, 0.78, 0.86, 0.94, 0.99)
foreach ($frac in $rows) {
  $y0 = [int]($h * $frac)
  $y1 = [Math]::Min($y0 + 6, $h)
  $r = 0L; $g = 0L; $b = 0L; $n = 0L
  for ($x = 2; $x -lt [int]($w * 0.08); $x += 2) {
    for ($y = $y0; $y -lt $y1; $y++) {
      $c = $img.GetPixel($x, $y); $r += $c.R; $g += $c.G; $b += $c.B; $n++
    }
  }
  $col = [System.Drawing.Color]::FromArgb([int]($r/$n), [int]($g/$n), [int]($b/$n))
  Write-Output ("  {0,5:N1}%   #{1:X2}{2:X2}{3:X2}" -f ($frac * 100), $col.R, $col.G, $col.B)
}

# 硬边检测：相邻行平均色的最大跳变
Write-Output ''
Write-Output '=== 硬边检测（相邻行差值，>6 视为可见跳变）==='
$prev = $null
$maxJump = 0
for ($y = 2; $y -lt $h - 2; $y += 2) {
  $r = 0L; $g = 0L; $b = 0L; $n = 0L
  for ($x = 2; $x -lt [int]($w * 0.08); $x += 2) {
    $c = $img.GetPixel($x, $y); $r += $c.R; $g += $c.G; $b += $c.B; $n++
  }
  $cur = @([int]($r/$n), [int]($g/$n), [int]($b/$n))
  if ($prev) {
    $d = [Math]::Abs($cur[0]-$prev[0]) + [Math]::Abs($cur[1]-$prev[1]) + [Math]::Abs($cur[2]-$prev[2])
    if ($d -gt $maxJump) { $maxJump = $d }
    if ($d -gt 18) { Write-Output "  y=$y 跳变 $d" }
  }
  $prev = $cur
}
Write-Output "  最大相邻行跳变: $maxJump （越小越平滑）"

# 品牌蓝是否落到位
Write-Output ''
Write-Output '=== 关键元素取色 ==='
function MostSat($x0, $y0, $x1, $y1, $label) {
  $best = $null; $bestSat = -1
  for ($x = $x0; $x -lt $x1; $x++) {
    for ($y = $y0; $y -lt $y1; $y++) {
      $c = $img.GetPixel($x, $y)
      $sat = [Math]::Max($c.R,[Math]::Max($c.G,$c.B)) - [Math]::Min($c.R,[Math]::Min($c.G,$c.B))
      if ($sat -gt $bestSat) { $bestSat = $sat; $best = $c }
    }
  }
  Write-Output ("  {0,-16} #{1:X2}{2:X2}{3:X2}" -f $label, $best.R, $best.G, $best.B)
}
MostSat 240 940 480 1030 '登录按钮'
MostSat 250 120 470 200  '品牌方块「学」'
MostSat 430 1105 520 1150 '「立即注册」文字'
$img.Dispose()
