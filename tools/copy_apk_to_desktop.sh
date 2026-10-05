#!/bin/bash
# 把最新构建的安卓 APK 复制一份到用户桌面。
# 用法：bash tools/copy_apk_to_desktop.sh
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SRC="$ROOT/app/build/outputs/apk/release/app-release.apk"
DST_DIR="$HOME/Desktop"

if [ ! -f "$SRC" ]; then
  echo "未找到 APK：$SRC" >&2
  echo "请先构建：./gradlew assembleRelease" >&2
  exit 1
fi

mkdir -p "$DST_DIR"
DST="$DST_DIR/RuLearn-app-release.apk"
cp -f "$SRC" "$DST"
echo "已复制最新 APK 到桌面：$DST"
ls -la "$DST"
