# 科大er专属的俄语学习器

一个极简的俄语学习静态网页，托管在 GitHub Pages。包含：
- 俄语常用短语速记
- 可下载的《俄语速查表》PDF
- **意见反馈表单**：提交后直接变成你 GitHub 仓库里的 Issue，后台 Issues 里就能看到。

## 文件说明
- `index.html` —— 主页面（含反馈逻辑，已适配竖屏/横屏）
- `app-release.apk` —— **安卓 App 安装包**（你自己的 APK，放到仓库根目录即可被下载）
- `russian-cheatsheet.pdf` —— 备用俄语速查表（PDF）
- `russian-cheatsheet.html` —— 速查表的网页/打印版（可另存为 PDF）

## 部署到 GitHub Pages
1. 在 GitHub 新建一个仓库（例如 `russian-learner`）。
2. 把页面和资料推上去：`index.html`、`app-release.apk`（你的安卓安装包）、`russian-cheatsheet.pdf`、`russian-cheatsheet.html`。
3. 仓库 → **Settings → Pages → Source** 选 `main` 分支根目录，保存。
4. 几分钟后访问 `https://<你的用户名>.github.io/<仓库名>/`。

## 让“意见反馈”能发到 GitHub
打开 `index.html`，找到顶部这段配置并改成你自己的：

```js
const CONFIG = {
  OWNER: "YOUR_GITHUB_USERNAME",   // 改成你的 GitHub 用户名
  REPO:  "YOUR_REPO_NAME",         // 改成仓库名
  TOKEN: ""                        // 见下方，留空则用“预填 Issue”方式
};
```

### 方式一（推荐）：填 Token，任何人都能直接提交
1. GitHub → 右上角头像 → **Settings → Developer settings → Personal access tokens → Fine-grained tokens → Generate new token**。
2. 权限只给这一个仓库，勾选 **Issues: Read and write**。
3. 生成后把那串 `github_pat_xxx` 粘到 `TOKEN` 里。
4. 别人提交反馈 → 自动在你的仓库 **Issues** 里新建一条，标题形如 `[反馈] 建议 · 匿名科大er`。

> 安全提示：Token 会出现在网页源码里，任何人都能看到。
> 因此它只能有“该仓库 Issues 写”这一个权限，且建议设较短过期时间、只用在这个小项目上。
> 如果担心，用下面的方式二。

### 方式二（更省心，无需 Token）
把 `TOKEN` 留空，并填好 `OWNER` / `REPO`。
别人点“提交反馈”会打开一个**已经帮你填好内容**的 GitHub Issues 新建页，
提交者登录 GitHub 后点一下 “Submit new issue” 即可。反馈同样出现在你仓库的 Issues 里。

## 自定义
- 改短语：编辑 `index.html` 里的 `PHRASES` 数组。
- 改速查表：改 `russian-cheatsheet.html` 后，用浏览器“打印 → 另存为 PDF”重新生成 `russian-cheatsheet.pdf`。
- 改下载 App：把你的安卓安装包命名为 `app-release.apk` 放到仓库根目录（或改 `index.html` 里 `id="dlBtn"` 的 `href` 指向你的 apk 文件名）。
- 改竖屏/横屏样式：编辑 `index.html` 里 `@media (orientation: ...)` 那段。
