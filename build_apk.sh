#!/usr/bin/env bash
#
# 快怼+（KuaiSnapPlus）命令行构建脚本
#
# 用途：本仓库没有提交 Gradle Wrapper，且模块代码量很小，
#       因此提供一条不依赖 Gradle 的构建路径，方便快速出包。
#       如果你使用 Android Studio，直接在 IDE 里 Build 即可，无需本脚本。
#
# 依赖：JDK 8+、Android SDK（build-tools 与 platform）
#       需要环境变量 ANDROID_HOME，或直接修改下面的 SDK 变量。
#
# 用法：./build_apk.sh
#
set -euo pipefail

SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}}"
BUILD_TOOLS="${BUILD_TOOLS:-$(ls -1 "$SDK/build-tools" | sort -V | tail -1)}"
PLATFORM="${PLATFORM:-$(ls -1 "$SDK/platforms" | sort -V | tail -1)}"

BT="$SDK/build-tools/$BUILD_TOOLS"
ANDROID_JAR="$SDK/platforms/$PLATFORM/android.jar"
XPOSED_JAR="app/libs/compile_only/xposed-api-82_compileonly.jar"

# 版本号（与 app/build.gradle 保持一致）
VERSION_CODE="20260912"
VERSION_NAME="1.3.0"

# 签名配置：请替换为你自己的密钥
KS="${KS:-keystore/kuaisnapplus-release.jks}"
KS_ALIAS="${KS_ALIAS:-kuaisnapplus}"
KS_PASS="${KS_PASS:-KuaiSnap#2026Release}"

OUT_DIR="dist"
WORK="build_tmp"

for f in "$BT/aapt2" "$ANDROID_JAR" "$XPOSED_JAR"; do
  [ -e "$f" ] || { echo "缺少依赖: $f" >&2; exit 1; }
done

echo "==> SDK        : $SDK"
echo "==> build-tools: $BUILD_TOOLS"
echo "==> platform   : $PLATFORM"

rm -rf "$WORK"; mkdir -p "$WORK/res" "$WORK/gen" "$WORK/classes" "$WORK/dex" "$OUT_DIR"

echo "==> [1/6] 编译资源"
"$BT/aapt2" compile --dir app/src/main/res -o "$WORK/res.zip"

echo "==> [2/6] 链接资源并生成 R.java"
"$BT/aapt2" link \
  -o "$WORK/base.apk" \
  -I "$ANDROID_JAR" \
  --manifest app/src/main/AndroidManifest.xml \
  "$WORK/res.zip" \
  --java "$WORK/gen" \
  --min-sdk-version 21 \
  --target-sdk-version 26 \
  --version-code "$VERSION_CODE" \
  --version-name "$VERSION_NAME"

echo "==> [3/6] 编译 Java 源码"
javac -nowarn -encoding UTF-8 -source 8 -target 8 \
  -bootclasspath "$ANDROID_JAR" \
  -cp "$XPOSED_JAR" \
  -d "$WORK/classes" \
  $(find app/src/main/java -name '*.java') \
  "$WORK/gen/com/kite/kuaisnapplus/R.java"

echo "==> [4/6] 生成 classes.dex"
"$BT/d8" --min-api 21 --lib "$ANDROID_JAR" --lib "$XPOSED_JAR" \
  --output "$WORK/dex" $(find "$WORK/classes" -name '*.class')

echo "==> [5/6] 打包（dex + assets）"
python3 - "$WORK" <<'PY'
import sys, zipfile, os
work = sys.argv[1]
src, dst = os.path.join(work, 'base.apk'), os.path.join(work, 'unsigned.apk')
zin = zipfile.ZipFile(src)
zout = zipfile.ZipFile(dst, 'w', zipfile.ZIP_DEFLATED)
# 原样保留 aapt2 的条目与压缩方式（resources.arsc 必须保持未压缩）
for info in zin.infolist():
    zi = zipfile.ZipInfo(info.filename, date_time=info.date_time)
    zi.compress_type = info.compress_type
    zi.external_attr = info.external_attr
    zout.writestr(zi, zin.read(info.filename))
zin.close()
for path, arc in [('classes.dex', 'classes.dex')]:
    p = os.path.join(work, 'dex', path)
    zi = zipfile.ZipInfo(arc, date_time=(1980, 1, 1, 0, 0, 0))
    zi.compress_type = zipfile.ZIP_DEFLATED
    zi.external_attr = 0o644 << 16
    zout.writestr(zi, open(p, 'rb').read())
for name in os.listdir('app/src/main/assets'):
    p = os.path.join('app/src/main/assets', name)
    if os.path.isfile(p):
        zi = zipfile.ZipInfo('assets/' + name, date_time=(1980, 1, 1, 0, 0, 0))
        zi.compress_type = zipfile.ZIP_DEFLATED
        zi.external_attr = 0o644 << 16
        zout.writestr(zi, open(p, 'rb').read())
zout.close()
print('    packaged:', dst)
PY

echo "==> [6/6] 对齐并签名"
"$BT/zipalign" -f -p 4 "$WORK/unsigned.apk" "$WORK/aligned.apk"
APK="$OUT_DIR/KuaiSnapPlus_${VERSION_NAME}.apk"
"$BT/apksigner" sign \
  --ks "$KS" --ks-key-alias "$KS_ALIAS" \
  --ks-pass "pass:$KS_PASS" --key-pass "pass:$KS_PASS" \
  --v1-signing-enabled true --v2-signing-enabled true --v3-signing-enabled true \
  --out "$APK" "$WORK/aligned.apk"
rm -f "$OUT_DIR"/*.idsig

echo
echo "==> 校验签名"
"$BT/apksigner" verify --verbose "$APK" | head -5
echo
echo "构建完成: $APK"
sha256sum "$APK"
