#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""生成 快怼+ 的 App 图标资源（Material Design 3 自适应图标）。

设计稿为 `speed_check.svg`（viewBox 0 0 100 100）。本脚本把它映射成
Android 自适应图标的三层资源，并导出 API 23-25 使用的旧版位图。

规范依据：
  * 自适应图标画布 108x108dp，遮罩作用于居中 72x72dp
  * 全部内容须落在居中 66dp 直径圆内（安全区）
  * background 层必须整幅铺满；foreground 层只承载一个视觉焦点
  * monochrome 层供 Android 13+ 主题图标取色

映射方式：把设计稿光栅化后求内容的**真实外接圆**（而不是包围盒对角线，
否则会明显偏小），再相似变换到直径 66dp。

用法：
    python3 design/icon/build_icon.py

依赖：rsvg-convert、Pillow、NumPy
"""
import math
import os
import shutil
import subprocess
import sys
import tempfile

import numpy as np
from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
RES = os.path.join(ROOT, "app", "src", "main", "res")
SRC = os.path.join(ROOT, "design", "icon", "speed_check.svg")

CANVAS = 108.0        # 自适应图标画布边长（dp）
SAFE_R = 33.0         # 安全区半径 = 66dp / 2
MASK_INSET = 18.0     # 遮罩作用区域相对画布的内缩（(108-72)/2）

PRIMARY = "#005AC4"   # material-color-utilities: 种子 #3482FF -> tone 40
ON_PRIMARY = "#FFFFFF"

# 旧版位图：无遮罩，自带圆角容器，内容可略放大
LEGACY_PAD = 1.12
LEGACY_RADIUS = 0.225
LEGACY_SS = 4
DENSITIES = {"ldpi": 36, "mdpi": 48, "hdpi": 72,
             "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}


def svg_path_data():
    """解析设计稿，分别取出 <path>（对勾）与 <line>（基准线）的属性"""
    import re
    src = open(SRC, encoding="utf-8").read()
    src = re.sub(r"<!--.*?-->", "", src, flags=re.S)

    def grab(tag):
        m = re.search(rf"<{tag}\b([^>]*?)/>", src)
        if not m:
            sys.exit(f"design/icon/speed_check.svg 缺少 <{tag}> 元素")
        return dict(re.findall(r'([\w-]+)="([^"]*)"', m.group(1)))

    return grab("path"), grab("line")


def rasterize(px, dst):
    subprocess.run(["rsvg-convert", "-w", str(px), "-h", str(px),
                    "--background-color=none", SRC, "-o", dst], check=True)
    return Image.open(dst).convert("RGBA")


def fit_transform(workdir, probe=512, render_px=2048):
    """求 100-box -> 108-box 的相似变换，使内容外接圆恰好等于安全区"""
    im = rasterize(render_px, os.path.join(workdir, "fit.png"))
    im = im.resize((probe, probe), Image.LANCZOS)
    alpha = np.array(im.getchannel("A")) > 8
    ys, xs = np.nonzero(alpha)
    cx, cy = (xs.min() + xs.max() + 1) / 2.0, (ys.min() + ys.max() + 1) / 2.0
    r = float(np.sqrt((xs + 0.5 - cx) ** 2 + (ys + 0.5 - cy) ** 2).max())
    unit = probe / 100.0                 # 1 个设计稿单位 = unit 像素
    r_design = r / unit
    k = SAFE_R / r_design
    return (cx / unit, cy / unit), k, r_design


def xf(pt, center, k):
    return (CANVAS / 2 + (pt[0] - center[0]) * k,
            CANVAS / 2 + (pt[1] - center[1]) * k)


def check_safe_zone(points, k):
    """确认所有描边外缘都落在 66dp 安全圆内"""
    worst = 0.0
    for (x, y), half in points:
        worst = max(worst, math.hypot(x - CANVAS / 2, y - CANVAS / 2) + half)
    return worst


def parse_check(d):
    """从 path data 中取出顶点；支持 'M26 51 L41 66' 与 'M26,51 L41,66' 两种写法"""
    import re
    nums = [float(v) for v in re.findall(r'-?\d+\.?\d*', d)]
    if len(nums) % 2:
        sys.exit(f"path data 坐标数不是偶数：{d}")
    return list(zip(nums[0::2], nums[1::2]))


def write_vectors(center, k):
    path, line = svg_path_data()
    pts = [xf(p, center, k) for p in parse_check(path["d"])]
    cw = float(path["stroke-width"]) * k

    bx1 = xf((float(line["x1"]), float(line["y1"])), center, k)
    bx2 = xf((float(line["x2"]), float(line["y2"])), center, k)
    bw = float(line["stroke-width"]) * k
    bop = float(line.get("opacity", 1.0))

    worst = check_safe_zone(
        [(p, cw / 2) for p in pts] + [(bx1, bw / 2), (bx2, bw / 2)], k)
    if worst > SAFE_R + 0.05:
        sys.exit(f"内容超出安全区：外接半径 {worst:.2f}dp > {SAFE_R}dp")

    check_d = "M{:.3f},{:.3f} L{:.3f},{:.3f} L{:.3f},{:.3f}".format(
        *[c for p in pts for c in p])
    base_d = "M{:.3f},{:.3f} L{:.3f},{:.3f}".format(*bx1, *bx2)

    def vector(color, alpha):
        return ('<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
                '    android:width="108dp"\n'
                '    android:height="108dp"\n'
                '    android:viewportWidth="108"\n'
                '    android:viewportHeight="108">\n'
                '    <!-- 对勾：内容外接圆直径 66dp，位于自适应图标安全区内 -->\n'
                '    <path\n'
                f'        android:pathData="{check_d}"\n'
                f'        android:strokeColor="{color}"\n'
                f'        android:strokeWidth="{cw:.2f}"\n'
                '        android:strokeLineCap="round"\n'
                '        android:strokeLineJoin="round" />\n'
                '    <!-- 基准线：次级元素，压低不透明度让出主焦点 -->\n'
                '    <path\n'
                f'        android:pathData="{base_d}"\n'
                f'        android:strokeColor="{color}"\n'
                f'        android:strokeWidth="{bw:.2f}"\n'
                f'        android:strokeAlpha="{bop}"\n'
                '        android:strokeLineCap="round" />\n'
                '</vector>\n')

    out = os.path.join(RES, "drawable")
    os.makedirs(out, exist_ok=True)
    open(os.path.join(out, "ic_launcher_foreground.xml"), "w", encoding="utf-8").write(
        vector(ON_PRIMARY, bop))
    open(os.path.join(out, "ic_launcher_monochrome.xml"), "w", encoding="utf-8").write(
        vector("#000000", bop))
    open(os.path.join(out, "ic_launcher_background.xml"), "w", encoding="utf-8").write(
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        '    android:width="108dp"\n    android:height="108dp"\n'
        '    android:viewportWidth="108"\n    android:viewportHeight="108">\n'
        '    <!-- 主题图标模式下由系统重新着色，必须整幅铺满 -->\n'
        f'    <path\n        android:fillColor="{PRIMARY}"\n'
        '        android:pathData="M0,0h108v108h-108z" />\n'
        '</vector>\n')

    adaptive = ('<?xml version="1.0" encoding="utf-8"?>\n'
                '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n'
                '    <background android:drawable="@drawable/ic_launcher_background" />\n'
                '    <foreground android:drawable="@drawable/ic_launcher_foreground" />\n'
                '    <monochrome android:drawable="@drawable/ic_launcher_monochrome" />\n'
                '</adaptive-icon>\n')
    anydpi = os.path.join(RES, "mipmap-anydpi-v26")
    os.makedirs(anydpi, exist_ok=True)
    for n in ("ic_launcher", "ic_launcher_round"):
        open(os.path.join(anydpi, n + ".xml"), "w", encoding="utf-8").write(adaptive)

    print(f"  内容外接半径 {worst:.2f}dp / 安全区 {SAFE_R}dp  -> OK")
    print(f"  对勾线宽 {cw:.2f}dp，基准线线宽 {bw:.2f}dp @ alpha {bop}")
    return pts, cw, bx1, bx2, bw, bop


def legacy_bitmap(size, pts, cw, bx1, bx2, bw, bop):
    big = size * LEGACY_SS
    im = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    d.rounded_rectangle([0, 0, big - 1, big - 1], radius=int(big * LEGACY_RADIUS),
                        fill=tuple(int(PRIMARY[i:i + 2], 16) for i in (1, 3, 5)) + (255,))
    u = big / CANVAS * LEGACY_PAD
    mid = big / 2

    def P(p):
        return (mid + (p[0] - CANVAS / 2) * u, mid + (p[1] - CANVAS / 2) * u)

    for p, q in zip(pts, pts[1:]):
        d.line([P(p), P(q)], fill=(255, 255, 255, 255),
               width=int(round(cw * u)), joint="curve")
    for p in pts:
        x, y = P(p)
        r = cw * u / 2
        d.ellipse([x - r, y - r, x + r, y + r], fill=(255, 255, 255, 255))

    a, b = P(bx1), P(bx2)
    al = int(round(255 * bop))
    d.line([a, b], fill=(255, 255, 255, al), width=int(round(bw * u)))
    for x, y in (a, b):
        r = bw * u / 2
        d.ellipse([x - r, y - r, x + r, y + r], fill=(255, 255, 255, al))
    return im.resize((size, size), Image.LANCZOS)


def write_mipmaps(*geom):
    for name, size in DENSITIES.items():
        directory = os.path.join(RES, f"mipmap-{name}")
        os.makedirs(directory, exist_ok=True)
        im = legacy_bitmap(size, *geom)
        im.save(os.path.join(directory, "ic_launcher.png"), optimize=True)
        im.save(os.path.join(directory, "ic_launcher_round.png"), optimize=True)


def main():
    if not os.path.exists(SRC):
        sys.exit(f"找不到设计稿：{SRC}")
    workdir = tempfile.mkdtemp(prefix="kuaisnap-icon-")
    try:
        center, k, r = fit_transform(workdir)
        print(f"设计稿内容外接半径 {r:.2f}，缩放 k={k:.4f}")
        geom = write_vectors(center, k)
        write_mipmaps(*geom)
        print(f"已写入 {RES}")
    finally:
        shutil.rmtree(workdir, ignore_errors=True)


if __name__ == "__main__":
    main()
