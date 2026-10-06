#!/bin/bash
# 把「导出的学习进度 JSON」同步到桌面并上传到 GitHub。
# 用法：bash tools/backup_progress.sh <导出的进度json路径>
#
# 行为：
#   1) 复制到 ~/Desktop/RuLearn进度备份/（桌面留一份）
#   2) 提交并推送到 GitHub 仓库的 progress-backups/ 目录（默认推到 main）
#
# 隐私说明：当前仓库 RussianLearner 是【公开】仓库，进度会公开可见。
#   若不想公开，请把下面 REMOTE / BRANCH 改成一个【私有】仓库或分支。
set -uo pipefail

SRC="${1:-}"
if [ -z "$SRC" ]; then
  echo "用法: $0 <导出的进度json路径>" >&2
  exit 1
fi
if [ ! -f "$SRC" ]; then
  echo "文件不存在: $SRC" >&2
  exit 1
fi

# ---- 1) 桌面留一份 ----
DST_DIR="$HOME/Desktop/RuLearn进度备份"
mkdir -p "$DST_DIR"
NAME="$(basename "$SRC")"
cp -f "$SRC" "$DST_DIR/$NAME"
echo "已复制到桌面: $DST_DIR/$NAME"

# ---- 2) 上传到 GitHub ----
REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BRANCH="main"          # 如需隔离可改成独立分支，如 progress-backups
REMOTE="origin"

cd "$REPO_ROOT" || exit 1
if ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  echo "不是 git 仓库: $REPO_ROOT" >&2
  exit 1
fi

mkdir -p progress-backups
cp -f "$SRC" "progress-backups/$NAME"
git add "progress-backups/$NAME"
if git diff --cached --quiet; then
  echo "没有变更需要提交。"
else
  git commit -m "backup: 学习进度 $NAME"
  git push "$REMOTE" "$BRANCH"
  echo "已推送到 GitHub ($REMOTE/$BRANCH)。"
fi
