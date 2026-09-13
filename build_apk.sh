#!/usr/bin/env bash
#
# 快怼+（KuaiSnapPlus）构建脚本
#
# 构建环境要求（缺一不可）：
#   - JDK 21（AGP 9 要求；source/target 亦为 21）
#   - Android SDK：platform 37 + build-tools 37.0.0
#     Miuix 的 AAR 声明了 minCompileSdk=37，因此必须用 37 编译
#   - 已设置 ANDROID_HOME 指向 SDK 目录
#
# 用法：
#   ./build_apk.sh                # 构建 release
#   ./build_apk.sh assembleDebug  # 构建 debug
#
set -euo pipefail
cd "$(dirname "$0")"

TASK="${1:-assembleRelease}"

if [ -z "${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}" ]; then
  echo "错误：请先设置 ANDROID_HOME 指向 Android SDK 目录" >&2
  exit 1
fi

./gradlew ":app:${TASK}" --console=plain

echo
echo "构建完成，产物："
ls -la app/build/outputs/apk/*/*.apk 2>/dev/null || true
