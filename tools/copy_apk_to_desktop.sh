#!/bin/bash
# 构建/更新后，把最新产物同步到用户桌面：
#   1) 最新 APK（用版本号命名） -> ~/Desktop/RuLearner-<版本号>.apk
#   2) 主词库 vocab.json         -> ~/Desktop/RuLearn资源包/vocab.json
# 用法：bash tools/copy_apk_to_desktop.sh
set -uo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DESKTOP="$HOME/Desktop"
GRADLE="$ROOT/app/build.gradle.kts"

# ---- 从 build.gradle.kts 读取版本号，用于桌面副本命名 ----
VERSION_NAME=$(grep -oE 'versionName\s*=\s*"[^"]+"' "$GRADLE" 2>/dev/null | sed -E 's/.*"([^"]+)".*/\1/' | head -1)

# 1) APK
APK_SRC="$ROOT/app/build/outputs/apk/release/app-release.apk"
if [ -f "$APK_SRC" ]; then
  if [ -n "$VERSION_NAME" ]; then
    APK_DST="$DESKTOP/RuLearner-${VERSION_NAME}.apk"
  else
    APK_DST="$DESKTOP/RuLearner-app-release.apk"
  fi
  cp -f "$APK_SRC" "$APK_DST" && echo "APK -> $APK_DST"

  # 清理旧的通用命名副本（若存在）
  LEGACY="$DESKTOP/RuLearn-app-release.apk"
  if [ -f "$LEGACY" ]; then
    rm -f "$LEGACY" && echo "已移除旧命名副本: $LEGACY"
  fi
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
