# 从设计稿采样配色。用 System.Drawing，不依赖 PIL。
Add-Type -AssemblyName System.Drawing

$src = 'D:\Android\edu-learning-platform-android-main\docs\screenshots\_design\App启动页.jpg'
$img = [System.Drawing.Bitmap]::FromFile($src)
$w = $img.Width; $h = $img.Height
Write-Output "图片: $w x $h"

function HexOf($c) { '#{0:X2}{1:X2}{2:X2}' -f $c.R, $c.G, $c.B }

function AvgBox($x0, $y0, $x1, $y1) {
  $x0 = [Math]::Max(0, $x0); $y0 = [Math]::Max(0, $y0)
  $x1 = [Math]::Min($w, $x1); $y1 = [Math]::Min($h, $y1)
  $r = 0L; $g = 0L; $b = 0L; $n = 0L
  for ($x = $x0; $x -lt $x1; $x += 2) {
    for ($y = $y0; $y -lt $y1; $y += 2) {
      $c = $img.GetPixel($x, $y); $r += $c.R; $g += $c.G; $b += $c.B; $n++
    }
  }
  if ($n -eq 0) { return [System.Drawing.Color]::Black }
  return [System.Drawing.Color]::FromArgb([int]($r / $n), [int]($g / $n), [int]($b / $n))
}

function MostSat($x0, $y0, $x1, $y1) {
  $x0 = [Math]::Max(0, $x0); $y0 = [Math]::Max(0, $y0)
  $x1 = [Math]::Min($w, $x1); $y1 = [Math]::Min($h, $y1)
  $best = $null; $bestSat = -1
  for ($x = $x0; $x -lt $x1; $x++) {
    for ($y = $y0; $y -lt $y1; $y++) {
      $c = $img.GetPixel($x, $y)
      $sat = [Math]::Max($c.R, [Math]::Max($c.G, $c.B)) - [Math]::Min($c.R, [Math]::Min($c.G, $c.B))
      if ($sat -gt $bestSat) { $bestSat = $sat; $best = $c }
    }
  }
  return , @($best, $bestSat)
}

Write-Output ''
Write-Output '=== 竖向渐变扫描（每行取中间 60% 宽度均值）==='
$step = [int]($h / 26)
for ($y = 0; $y -lt $h; $y += $step) {
  $c = AvgBox ([int]($w * 0.2)) $y ([int]($w * 0.8)) ([Math]::Min($y + $step, $h))
  Write-Output ("  y={0,5}  {1,5:N1}%   {2}" -f $y, ($y / $h * 100), (HexOf $c))
}

Write-Output ''
Write-Output '=== 横向扫描（看左右是否偏色）==='
foreach ($frac in 0.03, 0.25, 0.5, 0.75, 0.97) {
  $y0 = [int]($h * $frac)
  $y1 = [Math]::Min($y0 + [int]($h / 50) + 1, $h)
  $cols = @()
  foreach ($xf in 0.05, 0.25, 0.5, 0.75, 0.95) {
    $x0 = [int]($w * $xf)
    $x1 = [Math]::Min($x0 + [int]($w / 25) + 1, $w)
    $cols += (HexOf (AvgBox $x0 $y0 $x1 $y1))
  }
  Write-Output ("  y={0,5}  {1,4:N0}%   {2}" -f $y0, ($frac * 100), ($cols -join '  '))
}

Write-Output ''
Write-Output '=== 文字 / 插画取色 ==='
$regions = @(
  @('大标题「学习平台」', 0.05, 0.475, 0.95, 0.525),
  @('副标题「提升技能」', 0.10, 0.530, 0.90, 0.570),
  @('底部 logo 文字',   0.35, 0.880, 0.95, 0.940),
  @('桌面/抽屉（中性）',  0.28, 0.310, 0.42, 0.440),
  @('插画蓝（衣服/屏幕）', 0.40, 0.200, 0.70, 0.300),
  @('插画黄块',        0.36, 0.170, 0.44, 0.220),
  @('插画红块',        0.36, 0.190, 0.44, 0.240)
)
foreach ($r in $regions) {
  $x0 = [int]($w * $r[1]); $y0 = [int]($h * $r[2])
  $x1 = [int]($w * $r[3]); $y1 = [int]($h * $r[4])
  $res = MostSat $x0 $y0 $x1 $y1
  $c = $res[0]; $sat = $res[1]
  Write-Output ("  {0,-22} 饱和色 {1} (sat={2,3})   均值 {3}" -f $r[0], (HexOf $c), $sat, (HexOf (AvgBox $x0 $y0 $x1 $y1)))
}

$img.Dispose()
