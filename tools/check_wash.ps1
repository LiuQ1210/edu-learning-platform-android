# 采样模块底色，量化"淡到几乎看不出"到底有多淡。
Add-Type -AssemblyName System.Drawing
$out = 'D:\Android\edu-learning-platform-android-main\docs\screenshots'

function Sample($file, $x0, $y0, $x1, $y1, $label) {
  $img = [System.Drawing.Bitmap]::FromFile($file)
  $r = 0L; $g = 0L; $b = 0L; $n = 0L
  for ($x = $x0; $x -lt $x1; $x++) { for ($y = $y0; $y -lt $y1; $y++) {
    $p = $img.GetPixel($x, $y); $r += $p.R; $g += $p.G; $b += $p.B; $n++ } }
  $img.Dispose()
  return [System.Drawing.Color]::FromArgb([int]($r/$n), [int]($g/$n), [int]($b/$n))
}
function Hex($c) { '#{0:X2}{1:X2}{2:X2}' -f $c.R, $c.G, $c.B }

$home = "$out\04_课程首页.png"
$prof = "$out\07_个人主页.png"

Write-Output '=== 课程首页 ==='
$pageBg  = Sample $home 300 470 330 490   '页面底（热门课程标题下方空白）'
$search  = Sample $home 200 100 300 118   '搜索框内部'
$cardA   = Sample $home 175 700 200 720   '热门课程卡·上段'
$cardB   = Sample $home 175 860 200 880   '热门课程卡·下段'
$pill1   = Sample $home 300 205 320 215   '分类胶囊·艺术'
$pill2   = Sample $home 245 205 262 215   '分类胶囊·全部（选中外圈）'

Write-Output ("  页面底      {0}" -f (Hex $pageBg))
Write-Output ("  搜索框      {0}" -f (Hex $search))
Write-Output ("  课程卡上段  {0}" -f (Hex $cardA))
Write-Output ("  课程卡下段  {0}" -f (Hex $cardB))
Write-Output ("  胶囊未选中  {0}" -f (Hex $pill1))

Write-Output ''
Write-Output '=== 个人主页 ==='
$pBg   = Sample $prof 350 300 380 320    '页面底'
$pCard = Sample $prof 100 470 140 490   '菜单卡·上段'
$pCard2= Sample $prof 100 900 140 920   '菜单卡·下段'
Write-Output ("  页面底      {0}" -f (Hex $pBg))
Write-Output ("  菜单卡上段  {0}" -f (Hex $pCard))
Write-Output ("  菜单卡下段  {0}" -f (Hex $pCard2))

Write-Output ''
Write-Output '=== 差值（模块 vs 页面底）==='
function Diff($a, $b) { [Math]::Abs($a.R-$b.R) + [Math]::Abs($a.G-$b.G) + [Math]::Abs($a.B-$b.B) }
Write-Output ("  搜索框   Δ={0}" -f (Diff $search $pageBg))
Write-Output ("  课程卡   Δ={0} / {1}" -f (Diff $cardA $pageBg), (Diff $cardB $pageBg))
Write-Output ("  胶囊     Δ={0}" -f (Diff $pill1 $pageBg))
Write-Output ("  菜单卡   Δ={0} / {1}" -f (Diff $pCard $pBg), (Diff $pCard2 $pBg))
Write-Output ''
Write-Output "  参考：模块内部的渐变量（上段 vs 下段）"
Write-Output ("  课程卡内 Δ={0}   菜单卡内 Δ={1}" -f (Diff $cardA $cardB), (Diff $pCard $pCard2))
