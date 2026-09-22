# 从源图生成 Android 应用图标（launcher + 自适应 + 启动页共用）。
#
# 用法：
#   1. 把图标源图（正方形 PNG）复制到 $env:TEMP\app_icon_source.png
#   2. powershell -NoProfile -ExecutionPolicy Bypass -File tools\make_icons.ps1
#   3. 把脚本打印的 background color 同步到 res\drawable\ic_launcher_background.xml
#
# 生成两套，尺寸策略**不同**（这是关键，别用同一套）：
#
#   1. legacy 图标 mipmap-*/ic_launcher.png + ic_launcher_round.png
#      整图铺满，scale = 1.0。API 24-25 按方形/圆形直接显示，没有安全区概念。
#
#   2. 自适应前景 drawable/ic_launcher_foreground.png
#      系统只显示 108dp 视口中的中间 72dp，等于把前景再放大 1.5 倍后裁切。
#      若前景按 1.0 铺满，裁完只剩主体中间一块 —— 实测连右上角装饰、
#      底部底座都会被切掉。所以主体缩到 $foregroundScale 并居中，
#      保证放大 1.5 倍后仍落在直径 66dp 的安全圆内。
#
# 背景色从源图四角采样，保证自适应图标外圈与图片底色一致、看不出接缝。

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$src = "$env:TEMP\app_icon_source.png"
$res = 'D:\Android\edu-learning-platform-android-main\app\src\main\res'

# 自适应前景的主体缩放比例。0.62 = 「放大 1.5 倍后仍完整落在安全圆内」
# 且不至于显得太小；调大就会重新出现裁切。
$foregroundScale = 0.62

if (-not (Test-Path $src)) { throw "source image missing: $src" }
$img = [System.Drawing.Bitmap]::FromFile($src)
Write-Output "source: $($img.Width)x$($img.Height)"

function AvgCorner([int]$cx, [int]$cy, [int]$half) {
  $r = 0L; $g = 0L; $b = 0L; $n = 0L
  for ($x = [Math]::Max(0, $cx - $half); $x -lt [Math]::Min($img.Width, $cx + $half); $x++) {
    for ($y = [Math]::Max(0, $cy - $half); $y -lt [Math]::Min($img.Height, $cy + $half); $y++) {
      $p = $img.GetPixel($x, $y); $r += $p.R; $g += $p.G; $b += $p.B; $n++
    }
  }
  return [System.Drawing.Color]::FromArgb([int]($r/$n), [int]($g/$n), [int]($b/$n))
}
$c1 = AvgCorner 12 12 10
$c2 = AvgCorner ($img.Width - 12) 12 10
$c3 = AvgCorner 12 ($img.Height - 12) 10
$c4 = AvgCorner ($img.Width - 12) ($img.Height - 12) 10
$bgR = [int](($c1.R + $c2.R + $c3.R + $c4.R) / 4)
$bgG = [int](($c1.G + $c2.G + $c3.G + $c4.G) / 4)
$bgB = [int](($c1.B + $c2.B + $c3.B + $c4.B) / 4)
Write-Output ("background sampled: #{0:X2}{1:X2}{2:X2}" -f $bgR, $bgG, $bgB)

# 按短边居中取正方形源区域
$side = [Math]::Min($img.Width, $img.Height)
$sx = [int](($img.Width - $side) / 2)
$sy = [int](($img.Height - $side) / 2)
$srcRect = New-Object System.Drawing.Rectangle $sx, $sy, $side, $side

function MakeSquare([int]$size, [string]$path) {
  $bmp = New-Object System.Drawing.Bitmap $size, $size
  $g = [System.Drawing.Graphics]::FromImage($bmp)
  $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
  $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
  $g.DrawImage($img, (New-Object System.Drawing.Rectangle 0, 0, $size, $size), $srcRect, [System.Drawing.GraphicsUnit]::Pixel)
  $g.Dispose()
  $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
  $bmp.Dispose()
}

function MakeRound([int]$size, [string]$path) {
  $bmp = New-Object System.Drawing.Bitmap $size, $size
  $g = [System.Drawing.Graphics]::FromImage($bmp)
  $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
  $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
  $g.Clear([System.Drawing.Color]::Transparent)
  $clip = New-Object System.Drawing.Drawing2D.GraphicsPath
  $clip.AddEllipse(0, 0, $size, $size)
  $g.SetClip($clip)
  $g.DrawImage($img, (New-Object System.Drawing.Rectangle 0, 0, $size, $size), $srcRect, [System.Drawing.GraphicsUnit]::Pixel)
  $g.Dispose()
  $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
  $bmp.Dispose()
}

$densities = @(
  @{ dir = 'mipmap-mdpi';    size = 48  },
  @{ dir = 'mipmap-hdpi';    size = 72  },
  @{ dir = 'mipmap-xhdpi';   size = 96  },
  @{ dir = 'mipmap-xxhdpi';  size = 144 },
  @{ dir = 'mipmap-xxxhdpi'; size = 192 }
)

foreach ($d in $densities) {
  $dir = Join-Path $res $d['dir']
  New-Item -ItemType Directory -Force -Path $dir | Out-Null
  # 删掉旧的 webp，避免与 png 同名资源冲突
  Remove-Item (Join-Path $dir 'ic_launcher.webp') -Force -ErrorAction SilentlyContinue
  Remove-Item (Join-Path $dir 'ic_launcher_round.webp') -Force -ErrorAction SilentlyContinue
  MakeSquare $d['size'] (Join-Path $dir 'ic_launcher.png')
  MakeRound  $d['size'] (Join-Path $dir 'ic_launcher_round.png')
  Write-Output ("  {0}: {1}x{1}" -f $d['dir'], $d['size'])
}

$fgSize = 432
$fg = New-Object System.Drawing.Bitmap $fgSize, $fgSize
$g2 = [System.Drawing.Graphics]::FromImage($fg)
$g2.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$g2.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
# 先铺满背景色：主体缩小后露出的外圈与图片底色一致，放大后不会有色带
$g2.Clear([System.Drawing.Color]::FromArgb(255, $bgR, $bgG, $bgB))
$inner = [int]($fgSize * $foregroundScale)
$off = [int](($fgSize - $inner) / 2)
$g2.DrawImage($img, (New-Object System.Drawing.Rectangle $off, $off, $inner, $inner), $srcRect, [System.Drawing.GraphicsUnit]::Pixel)
$g2.Dispose()
$fg.Save((Join-Path $res 'drawable\ic_launcher_foreground.png'), [System.Drawing.Imaging.ImageFormat]::Png)
$fg.Dispose()
Write-Output ("  drawable/ic_launcher_foreground.png: {0}x{0}, content scale {1}" -f $fgSize, $foregroundScale)

$img.Dispose()
Write-Output ''
Write-Output ("done. adaptive background color = #{0:X2}{1:X2}{2:X2}" -f $bgR, $bgG, $bgB)
