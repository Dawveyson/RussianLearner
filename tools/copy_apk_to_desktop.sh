#!/bin/bash
# 构建/更新后，把最新产物同步到用户桌面：
#   1) 最新 APK          -> ~/Desktop/RuLearn-app-release.apk
#   2) 主词库 vocab.json  -> ~/Desktop/RuLearn资源包/vocab.json
# 用法：bash tools/copy_apk_to_desktop.sh
set -uo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DESKTOP="$HOME/Desktop"

# 1) APK
APK_SRC="$ROOT/app/build/outputs/apk/release/app-release.apk"
APK_DST="$DESKTOP/RuLearn-app-release.apk"
if [ -f "$APK_SRC" ]; then
  cp -f "$APK_SRC" "$APK_DST" && echo "APK -> $APK_DST"
else
  echo "跳过 APK：未找到 $APK_SRC（如需更新 APK，先 ./gradlew assembleRelease）"
fi

# 2) 主词库 JSON
VOCAB_SRC="$ROOT/tools/vocab.json"
VOCAB_DST="$DESKTOP/RuLearn资源包/vocab.json"
if [ -f "$VOCAB_SRC" ]; then
  mkdir -p "$DESKTOP/RuLearn资源包"
  cp -f "$VOCAB_SRC" "$VOCAB_DST" && echo "vocab.json -> $VOCAB_DST"
else
  echo "跳过 vocab.json：未找到 $VOCAB_SRC"
fi
