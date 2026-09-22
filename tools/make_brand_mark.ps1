# 从图标源图裁出应用内品牌图（登录/注册/启动页用）。
#
# 为什么不直接用 ic_launcher_foreground：
#   那是自适应图标前景，为了适配系统的 72/108 安全区，主体被缩到了 0.62 并留了
#   一大圈底色。用在界面的小尺寸（28-56dp）上会显得「图很小、外圈很大」。
#   这里按内容自动裁紧、只留一点边距，专门给界面用。
#
# 输出 brand_mark.png 到 drawable-xhdpi / drawable-xxhdpi / drawable-xxxhdpi，
# 系统按密度自动选。

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$src = "$env:TEMP\app_icon_source.png"
$res = 'D:\Android\edu-learning-platform-android-main\app\src\main\res'
if (-not (Test-Path $src)) { throw "source image missing: $src" }

$img = [System.Drawing.Bitmap]::FromFile($src)
Write-Output "source: $($img.Width)x$($img.Height)"

# 四角采样背景色（与 make_icons.ps1 同一套逻辑）
function AvgCorner([int]$cx, [int]$cy, [int]$half) {
  $r = 0L; $g = 0L; $b = 0L; $n = 0L
  for ($x = [Math]::Max(0, $cx - $half); $x -lt [Math]::Min($img.Width, $cx + $half); $x++) {
    for ($y = [Math]::Max(0, $cy - $half); $y -lt [Math]::Min($img.Height, $cy + $half); $y++) {
      $p = $img.GetPixel($x, $y); $r += $p.R; $g += $p.G; $b += $p.B; $n++
    }
  }
  return [System.Drawing.Color]::FromArgb([int]($r/$n), [int]($g/$n), [int]($b/$n))
}
$c1 = AvgCorner 10 10 8
$c2 = AvgCorner ($img.Width - 10) 10 8
$c3 = AvgCorner 10 ($img.Height - 10) 8
$c4 = AvgCorner ($img.Width - 10) ($img.Height - 10) 8
$bgR = [int](($c1.R + $c2.R + $c3.R + $c4.R) / 4)
$bgG = [int](($c1.G + $c2.G + $c3.G + $c4.G) / 4)
$bgB = [int](($c1.B + $c2.B + $c3.B + $c4.B) / 4)
Write-Output ("background: #{0:X2}{1:X2}{2:X2}" -f $bgR, $bgG, $bgB)

# 用固定裁切比例，不做自动 bbox 检测。
#
# 为什么不自动检测：这张源图的背景本身带渐变加投影，"偏离背景色"和
# "饱和度高 / 亮度低"两种判据都会把整张图判成内容（实测 bbox 占满 100%）。
# 从图上看主体（B + 底座 + 播放键）大致落在四周 14% 边距内，按比例裁最稳。
$insetRatio = 0.14
$minX = [int]($img.Width * $insetRatio)
$maxX = [int]($img.Width * (1 - $insetRatio)) - 1
$minY = [int]($img.Height * $insetRatio)
$maxY = [int]($img.Height * (1 - $insetRatio)) - 1
$cw = $maxX - $minX + 1
$ch = $maxY - $minY + 1
Write-Output "crop: x $minX..$maxX  y $minY..$maxY  (${cw}x${ch})"

# 画成正方形，四周留 4% 边距，背景用采样到的底色（与图内底色一致，看不出接缝）
$side = [int]([Math]::Max($cw, $ch) * 1.08)
$offX = [int](($side - $cw) / 2)
$offY = [int](($side - $ch) / 2)

function MakeMark([int]$size, [string]$path) {
  $bmp = New-Object System.Drawing.Bitmap $size, $size
  $g = [System.Drawing.Graphics]::FromImage($bmp)
  $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
  $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
  $g.Clear([System.Drawing.Color]::FromArgb(255, $bgR, $bgG, $bgB))
  $srcRect = New-Object System.Drawing.Rectangle $minX, $minY, $cw, $ch
  $dstRect = New-Object System.Drawing.Rectangle ([int]($offX * $size / $side)), ([int]($offY * $size / $side)), ([int]($cw * $size / $side)), ([int]($ch * $size / $side))
  $g.DrawImage($img, $dstRect, $srcRect, [System.Drawing.GraphicsUnit]::Pixel)
  $g.Dispose()
  $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
  $bmp.Dispose()
  Write-Output ("  {0}  {1}x{1}" -f (Split-Path $path -Leaf), $size)
}

# 界面用到 28dp（启动页底部）与 56dp（登录/注册），给到 xxxhdpi 需要 224px
$targets = @(
  @{ dir = 'drawable-xhdpi';   size = 112 },
  @{ dir = 'drawable-xxhdpi';  size = 168 },
  @{ dir = 'drawable-xxxhdpi'; size = 224 }
)
foreach ($t in $targets) {
  $dir = Join-Path $res $t['dir']
  New-Item -ItemType Directory -Force -Path $dir | Out-Null
  MakeMark $t['size'] (Join-Path $dir 'brand_mark.png')
}

$img.Dispose()
Write-Output ''
Write-Output ("done. brand mark background = #{0:X2}{1:X2}{2:X2}" -f $bgR, $bgG, $bgB)
