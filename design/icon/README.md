# App 图标

## 设计

`speed_check.svg` 是唯一的设计稿（`viewBox 0 0 100 100`）：一个粗对勾，加一条比对勾水平跨度更短的基准线。

- **对勾**是主焦点，圆头圆角，线宽 9。语义直白——「答案正确」。
- **基准线**只有 35% 不透明度、且提前收尾（`x 26→61`，而对勾跨到 `x 74`）。它刻意不比对勾长，靠这个不对称把重心压向左上，制造张力。
- 基准线同时暗示**卷面**与**扫描基准**，点出这是个拍照搜题工具，而不用真的画一台相机。

设计遵循「单一焦点 + 负空间 + 不加装饰」：没有渐变、没有阴影、没有多余颜色，通篇只有一个语义元素加一个次级元素。

## 生成

```bash
python3 design/icon/build_icon.py
```

依赖 `rsvg-convert`、`Pillow`、`NumPy`。脚本会写入 17 个文件到 `app/src/main/res`：

| 输出 | 用途 |
|---|---|
| `drawable/ic_launcher_background.xml` | 自适应图标背景层（整幅铺满的纯色） |
| `drawable/ic_launcher_foreground.xml` | 前景层（白色对勾 + 基准线） |
| `drawable/ic_launcher_monochrome.xml` | Android 13+ 主题图标取色用的单色层 |
| `mipmap-anydpi-v26/ic_launcher{,_round}.xml` | 把三层组装成自适应图标 |
| `mipmap-{l,m,h,xh,xxh,xxxh}dpi/ic_launcher{,_round}.png` | API 23–25 的旧版位图（自带圆角容器） |

## 映射规则

Android 自适应图标画布 108dp，遮罩只作用于居中 72dp，**内容必须落在居中 66dp 直径圆内**。脚本不是按包围盒对角线缩放（那样会明显偏小），而是把设计稿光栅化后求内容的**真实外接圆**，再相似变换到直径 66dp，并断言外缘不超出安全区。

当前结果：内容外接半径 **32.94dp / 上限 33dp**，对勾线宽 7.78dp，基准线 4.75dp。

## 配色

背景 `#005AC4` 取自 `material-color-utilities` 对本 App 主题色种子 `#3482FF` 生成的 tonal palette 的 **tone 40**（即 Material 3 的 `primary`），前景为 **tone 100**（`on-primary`）。这样图标与 App 内的 Material 3 调色盘出自同一套色阶。
