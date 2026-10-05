package com.example.rulearn

/**
 * 导入规范与「交给 AI 转换」的提示词。集中定义格式与可复制的 AI 指令。
 */
object ImportSpec {

    const val VOCAB_SPEC = """
支持两种 UTF-8 文本文件，导入后成为一本独立词书：

① JSON（推荐，AI 最易生成）
[
  {"ru":"привет","zh":"你好"},
  {"ru":"спасибо","zh":"谢谢","audio":"","img":""},
  {"ru":"я люблю тебя","zh":"我爱你"}
]
字段：ru 俄语原文(必填)、zh 中文释义(必填)、audio 音频地址(可选)、img 配图地址(可选)、note 备注(可选)。
别名兼容：word/text 视作 ru；mean/meaning/trans 视作 zh。

② TSV / CSV
每行一条：俄语 + 制表符(或逗号) + 中文，可选第三列音频、第四列配图。
首行若是 ru/zh 表头会自动跳过。
"""

    const val ZIP_SPEC = """
推荐用 ZIP 音频书：把清单和音频打包在一起，导入时直接选 zip。

包结构（audio 只是示例目录名，可以换成别的）：
lesson1.zip
├── book.json          ← 清单，必需
└── audio/
    ├── u1.mp3         ← 整课音轨
    ├── 001.mp3        ← 逐句音轨
    └── 002.mp3

清单文件名可以是下列任意一种，App 会在包里自动找到它：
book.json / lessons.json / manifest.json / index.json

book.json 的内容和「单独 JSON」格式完全一致，唯一区别是
音频要写「包内相对路径」，不要写本机绝对路径。

{
  "lessons": [
    {
      "n": 1,
      "title": "урок 1 · 第1课",
      "audio": "audio/u1.mp3",
      "segments": [
        {"i":1,"ru":"Здравствуйте!","zh":"您好！","start":0.0,"end":2.4,"audio":"audio/001.mp3"}
      ]
    }
  ]
}

要点：
· 一个 zip 可以装多课，清单里 lessons 数组多写几项就行；
· 段级 audio 优先，段里没有就回落到课级 audio 按 start/end 切；
· 两者都没有也能用，App 会走网络发音朗读；
· 音频不想打包也可以继续写 http(s) 外链，App 一样认；
· 推荐 mp3 / m4a / ogg，包里不要塞无关文件。
"""

    const val LESSON_SPEC = """
课程 JSON，外层 {"lessons":[...]} 或直接是数组：

{
  "lessons": [
    {
      "n": 1,
      "title": "урок 1 · 第1课",
      "audio": "https://你的域名/u1.mp3",
      "segments": [
        {"i":1,"ru":"Здравствуйте!","zh":"您好！","start":0.0,"end":2.4},
        {"i":2,"ru":"Как дела?","zh":"最近怎么样？","start":2.4,"end":4.8,"audio":"https://.../002.mp3"}
      ]
    }
  ]
}

音频同步有两种写法，App 都支持：
· 整课一条音轨 — 课级 audio + 每段 start/end 时间戳，播放时自动 seek 并在 end 处停止。
· 每段一条音轨 — 段级 audio，直接播放该片段。
两种可混用：有 audio 的段用段音频，没有的回落到整课音轨按时间戳定位。
audio 可以是 http(s) URL、本地绝对路径、或打包进 App 的 assets 相对路径。
"""

    const val AI_VOCAB_JSON = """
你是一个俄语学习 App 的词库格式转换器。请把我提供的任意格式俄语材料，整理成【严格】的 JSON 数组用于导入 App。规则：
1) 只输出 JSON 数组，不要解释、不要用 markdown 代码块包裹；
2) 每个对象含字段 ru(俄语原文，保留 ё/й 等正确字符，整体小写；专有名词可保留大写)、zh(中文释义，多个义项用"、"分隔)、audio(一律填空字符串"")、img(一律填空字符串"")；
3) 过滤非俄语内容、去掉重复词条；
4) 若原文是整句，整句作为 ru、对应中文整句作为 zh。
以下是我的材料：
"""

    const val AI_VOCAB_TSV = """
请把我提供的俄语词汇材料整理成 TSV 纯文本：每行「俄语<制表符>中文」，不要表头、不要解释、不要空行。以下是我的材料：
"""

    const val AI_LESSON = """
你是一个俄语教材结构化工具。我要把一份俄语课文（可能带音频）导入学习 App，请输出【严格】的 JSON，不要解释、不要用 markdown 代码块包裹。

输出结构：
{"lessons":[{"n":课号数字,"title":"урок N · 第N课","audio":"整课音频URL，没有就填空字符串","segments":[{"i":段号,"ru":"俄文原文","zh":"中文翻译","start":起始秒数,"end":结束秒数}]}]}

规则：
1) 按自然句 / 意群切段，每段 1~8 秒，段之间不要重叠，start 从 0 开始递增；
2) 如果我没有提供时间轴，就按「每句约 2.5 秒」估算 start/end 并保持递增，不要全部填 0；
3) ru 保留正确西里尔字符与标点；zh 用简洁中文；
4) 如果没有音频，audio 一律填空字符串""，App 会用网络发音朗读；不要写本机绝对路径，也不要编造 URL；
5) 如果音频是随包一起发的，audio 写包内相对路径，例如 "audio/001.mp3"（我要把这个 JSON 和 audio 目录一起打成 zip 导入）；
6) 只输出一个 JSON 对象。
以下是我的材料：
"""

    const val AI_ZIP_TIP = """
把上面的 JSON 和音频做成一个 zip：

1) 建一个目录，例如 lesson1/；
2) 目录里放 book.json（把 AI 输出的整段 JSON 原样存进去）；
3) 音频放进子目录，例如 lesson1/audio/001.mp3；
4) JSON 里的 audio 字段写成 "audio/001.mp3" 这种相对路径；
5) 选中整个目录打包成 zip（注意在目录内部选中文件再压缩，
   否则 zip 里会多一层目录，导致第一层的 book.json 找不到）；
6) 在 App 的「导入资源 → 导入 zip 音频书」里选这个 zip。

命令行打包：cd lesson1 && zip -r ../lesson1.zip book.json audio/
"""

    const val AI_AUDIO_TIP = """
音频同步的常见做法：
· 有字幕文件（srt/vtt）→ 直接让 AI 把字幕转成上面的 segments，时间戳照抄。
· 只有一整段音频 → 让 AI（或 Whisper 类转写工具）先转写出带时间戳的文本，再套用上面的 JSON 结构。
· 想做逐句跟读 → 让工具按时间戳把整轨切成 001.mp3、002.mp3……，再把每段 audio 填成对应文件地址。
"""
}
