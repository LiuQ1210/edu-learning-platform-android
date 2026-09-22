"""从设计稿采样配色，输出可直接写进 Color.kt 的色值。

用法: python sample_palette.py <图片路径> [输出目录]

会输出:
  1. 指定图:区域的平均色 / 最高饱和色
  2. 竖向扫描:每 N 像素取一行平均色，用来看渐变怎么走
  3. 配色量化:整图出现最多的若干颜色
"""
import sys
import os
from collections import Counter

try:
    from PIL import Image
except ImportError:
    print("NEED_PIL")
    sys.exit(2)


def avg(img, box):
    x0, y0, x1, y1 = box
    px = img.load()
    r = g = b = n = 0
    for x in range(x0, x1):
        for y in range(y0, y1):
            p = px[x, y]
            r += p[0]; g += p[1]; b += p[2]; n += 1
    if n == 0:
        return (0, 0, 0)
    return (r // n, g // n, b // n)


def most_saturated(img, box):
    x0, y0, x1, y1 = box
    px = img.load()
    best, best_sat = None, -1
    for x in range(x0, x1):
        for y in range(y0, y1):
            p = px[x, y]
            sat = max(p[0], p[1], p[2]) - min(p[0], p[1], p[2])
            if sat > best_sat:
                best_sat, best = sat, p
    return best, best_sat


def hexs(c):
    return "#%02X%02X%02X" % (c[0], c[1], c[2])


def main():
    path = sys.argv[1]
    img = Image.open(path).convert("RGB")
    w, h = img.size
    print(f"图片: {path}")
    print(f"尺寸: {w}x{h}")

    # ---- 竖向扫描 ----
    print("\n=== 竖向渐变扫描（每行取中间 60% 宽度的均值）===")
    step = max(1, h // 24)
    for y in range(0, h, step):
        c = avg(img, (int(w * 0.2), y, int(w * 0.8), min(y + step, h)))
        print(f"  y={y:5d} ({y/h*100:5.1f}%)  {hexs(c)}")

    # ---- 横向扫描（取几个关键高度）----
    print("\n=== 横向扫描 ===")
    for frac in (0.05, 0.25, 0.5, 0.75, 0.95):
        y0 = int(h * frac)
        y1 = min(y0 + max(1, h // 40), h)
        cols = []
        for xf in (0.05, 0.25, 0.5, 0.75, 0.95):
            x0 = int(w * xf)
            x1 = min(x0 + max(1, w // 20), w)
            cols.append(hexs(avg(img, (x0, y0, x1, y1))))
        print(f"  y={y0:5d} ({frac*100:4.0f}%)  " + "  ".join(cols))

    # ---- 关键点：文字 / 插画 ----
    print("\n=== 文字与插画取色（最高饱和）===")
    regions = {
        "大标题「学习平台」": (0.05, 0.47, 0.95, 0.53),
        "副标题「提升技能」": (0.10, 0.53, 0.90, 0.57),
        "底部 logo 文字":   (0.35, 0.88, 0.95, 0.94),
        "桌面/抽屉（中性）":  (0.28, 0.31, 0.42, 0.44),
    }
    for name, (a, b, c, d) in regions.items():
        box = (int(w * a), int(h * b), int(w * c), int(h * d))
        c1, sat = most_saturated(img, box)
        print(f"  {name:22s} 饱和色 {hexs(c1)} (sat={sat})  均值 {hexs(avg(img, box))}")

    # ---- 配色量化 ----
    print("\n=== 出现最多的 14 种颜色（量化到 16 级）===")
    small = img.resize((w // 3, h // 3))
    cnt = Counter()
    for p in small.getdata():
        cnt[(p[0] >> 4 << 4, p[1] >> 4 << 4, p[2] >> 4 << 4)] += 1
    total = sum(cnt.values())
    for c, n in cnt.most_common(14):
        print(f"  {hexs(c)}  {n/total*100:5.2f}%")


if __name__ == "__main__":
    main()
