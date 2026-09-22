# 配色量化 + 精确取色：拿到设计稿的主色和渐变停靠点。
Add-Type -AssemblyName System.Drawing

$src = 'D:\Android\edu-learning-platform-android-main\docs\screenshots\_design\App启动页.jpg'
$img = [System.Drawing.Bitmap]::FromFile($src)
$w = $img.Width; $h = $img.Height
function HexOf($c) { '#{0:X2}{1:X2}{2:X2}' -f $c.R, $c.G, $c.B }

# ---- 颜色量化：统计出现最多的颜色 ----
Write-Output '=== 整图颜色分布（量化到 8 级，取前 18）==='
$cnt = @{}
$step = 3
for ($x = 0; $x -lt $w; $x += $step) {
  for ($y = 0; $y -lt $h; $y += $step) {
    $c = $img.GetPixel($x, $y)
    $k = '{0:X2}{1:X2}{2:X2}' -f ($c.R -band 0xF8), ($c.G -band 0xF8), ($c.B -band 0xF8)
    if ($cnt.ContainsKey($k)) { $cnt[$k]++ } else { $cnt[$k] = 1 }
  }
}
$total = ($cnt.Values | Measure-Object -Sum).Sum
$cnt.GetEnumerator() | Sort-Object Value -Descending | Select-Object -First 18 | ForEach-Object {
  Write-Output ("  #{0}   {1,6:N2}%" -f $_.Key, ($_.Value / $total * 100))
}

# ---- 四角与四边中点：判断渐变方向 ----
Write-Output ''
Write-Output '=== 边缘采样（判断渐变方向）==='
function P($x, $y) { HexOf $img.GetPixel([Math]::Min($x, $w - 1), [Math]::Min($y, $h - 1)) }
Write-Output ("  左上 {0}   上中 {1}   右上 {2}" -f (P 3 3), (P ([int]($w/2)) 3), (P ($w-4) 3))
Write-Output ("  左中 {0}   正中 {1}   右中 {2}" -f (P 3 ([int]($h/2))), (P ([int]($w/2)) ([int]($h/2))), (P ($w-4) ([int]($h/2))))
Write-Output ("  左下 {0}   下中 {1}   右下 {2}" -f (P 3 ($h-4)), (P ([int]($w/2)) ($h-4)), (P ($w-4) ($h-4)))

# ---- 纯背景列：避开插画，只看最左 6% 宽度 ----
Write-Output ''
Write-Output '=== 纯背景竖向渐变（只取最左 6% 宽度，避开插画与文字）==='
$stepY = [int]($h / 32)
for ($y = 0; $y -lt $h; $y += $stepY) {
  $r = 0L; $g = 0L; $b = 0L; $n = 0L
  for ($x = 2; $x -lt [int]($w * 0.06); $x += 2) {
    for ($yy = $y; $yy -lt [Math]::Min($y + $stepY, $h); $yy += 2) {
      $c = $img.GetPixel($x, $yy); $r += $c.R; $g += $c.G; $b += $c.B; $n++
    }
  }
  $col = [System.Drawing.Color]::FromArgb([int]($r/$n), [int]($g/$n), [int]($b/$n))
  $pct = [Math]::Round($y / $h * 100, 1)
  Write-Output ("  {0,5}%   {1}" -f $pct, (HexOf $col))
}

# ---- 右列：对比左右差异 ----
Write-Output ''
Write-Output '=== 右列背景（最右 6% 宽度）==='
for ($y = 0; $y -lt $h; $y += ($stepY * 4)) {
  $r = 0L; $g = 0L; $b = 0L; $n = 0L
  for ($x = [int]($w * 0.94); $x -lt $w - 2; $x += 2) {
    for ($yy = $y; $yy -lt [Math]::Min($y + $stepY, $h); $yy += 2) {
      $c = $img.GetPixel($x, $yy); $r += $c.R; $g += $c.G; $b += $c.B; $n++
    }
  }
  $col = [System.Drawing.Color]::FromArgb([int]($r/$n), [int]($g/$n), [int]($b/$n))
  Write-Output ("  {0,5}%   {1}" -f [Math]::Round($y / $h * 100, 1), (HexOf $col))
}

# ---- 插画里的高饱和色 ----
Write-Output ''
Write-Output '=== 插画区域高饱和色（左半 + 右半分别取）==='
function TopSat($x0, $y0, $x1, $y1, $label) {
  $buckets = @{}
  for ($x = $x0; $x -lt $x1; $x++) {
    for ($y = $y0; $y -lt $y1; $y++) {
      $c = $img.GetPixel($x, $y)
      $sat = [Math]::Max($c.R, [Math]::Max($c.G, $c.B)) - [Math]::Min($c.R, [Math]::Min($c.G, $c.B))
      if ($sat -lt 60) { continue }
      $k = '{0:X2}{1:X2}{2:X2}' -f ($c.R -band 0xF0), ($c.G -band 0xF0), ($c.B -band 0xF0)
      if ($buckets.ContainsKey($k)) { $buckets[$k]++ } else { $buckets[$k] = 1 }
    }
  }
  $top = $buckets.GetEnumerator() | Sort-Object Value -Descending | Select-Object -First 5
  Write-Output "  $label"
  foreach ($t in $top) { Write-Output ("      #{0}   x{1}" -f $t.Key, $t.Value) }
}
TopSat ([int]($w*0.05)) ([int]($h*0.05)) ([int]($w*0.95)) ([int]($h*0.46)) '上半部（插画主体）'
TopSat ([int]($w*0.05)) ([int]($h*0.46)) ([int]($w*0.95)) ([int]($h*0.58)) '中部（大标题）'
TopSat ([int]($w*0.05)) ([int]($h*0.86)) ([int]($w*0.95)) ([int]($h*0.98)) '底部（logo）'

$img.Dispose()
