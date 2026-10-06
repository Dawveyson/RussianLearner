import Foundation

/// 西里尔字母表（含发音提示与例词）。
let CYRILLIC_ALPHABET: [LetterInfo] = [
    LetterInfo(upper: "А", lower: "а", sound: "a（啊）", sampleRu: "арбуз", sampleZh: "西瓜"),
    LetterInfo(upper: "Б", lower: "б", sound: "b（波）", sampleRu: "банан", sampleZh: "香蕉"),
    LetterInfo(upper: "В", lower: "в", sound: "v（夫）", sampleRu: "вода", sampleZh: "水"),
    LetterInfo(upper: "Г", lower: "г", sound: "g（哥）", sampleRu: "город", sampleZh: "城市"),
    LetterInfo(upper: "Д", lower: "д", sound: "d（得）", sampleRu: "дом", sampleZh: "房子"),
    LetterInfo(upper: "Е", lower: "е", sound: "ye（耶）", sampleRu: "еда", sampleZh: "食物"),
    LetterInfo(upper: "Ё", lower: "ё", sound: "yo（哟）", sampleRu: "ёлка", sampleZh: "圣诞树"),
    LetterInfo(upper: "Ж", lower: "ж", sound: "zh（日）", sampleRu: "журнал", sampleZh: "杂志"),
    LetterInfo(upper: "З", lower: "з", sound: "z（兹）", sampleRu: "зима", sampleZh: "冬天"),
    LetterInfo(upper: "И", lower: "и", sound: "i（伊）", sampleRu: "игра", sampleZh: "游戏"),
    LetterInfo(upper: "Й", lower: "й", sound: "y 短音", sampleRu: "йогурт", sampleZh: "酸奶"),
    LetterInfo(upper: "К", lower: "к", sound: "k（科）", sampleRu: "книга", sampleZh: "书"),
    LetterInfo(upper: "Л", lower: "л", sound: "l（勒）", sampleRu: "лампа", sampleZh: "台灯"),
    LetterInfo(upper: "М", lower: "м", sound: "m（姆）", sampleRu: "мама", sampleZh: "妈妈"),
    LetterInfo(upper: "Н", lower: "н", sound: "n（恩）", sampleRu: "нос", sampleZh: "鼻子"),
    LetterInfo(upper: "О", lower: "о", sound: "o（奥）", sampleRu: "окно", sampleZh: "窗户"),
    LetterInfo(upper: "П", lower: "п", sound: "p（普）", sampleRu: "папа", sampleZh: "爸爸"),
    LetterInfo(upper: "Р", lower: "р", sound: "r 颤音", sampleRu: "рука", sampleZh: "手"),
    LetterInfo(upper: "С", lower: "с", sound: "s（斯）", sampleRu: "солнце", sampleZh: "太阳"),
    LetterInfo(upper: "Т", lower: "т", sound: "t（特）", sampleRu: "там", sampleZh: "那里"),
    LetterInfo(upper: "У", lower: "у", sound: "u（乌）", sampleRu: "утро", sampleZh: "早晨"),
    LetterInfo(upper: "Ф", lower: "ф", sound: "f（福）", sampleRu: "фото", sampleZh: "照片"),
    LetterInfo(upper: "Х", lower: "х", sound: "kh（赫）", sampleRu: "хлеб", sampleZh: "面包"),
    LetterInfo(upper: "Ц", lower: "ц", sound: "ts（茨）", sampleRu: "цветок", sampleZh: "花"),
    LetterInfo(upper: "Ч", lower: "ч", sound: "ch（切）", sampleRu: "чай", sampleZh: "茶"),
    LetterInfo(upper: "Ш", lower: "ш", sound: "sh（什）", sampleRu: "школа", sampleZh: "学校"),
    LetterInfo(upper: "Щ", lower: "щ", sound: "shch（希）", sampleRu: "щи", sampleZh: "菜汤"),
    LetterInfo(upper: "Ъ", lower: "ъ", sound: "硬音符·不发音", sampleRu: "съезд", sampleZh: "代表大会"),
    LetterInfo(upper: "Ы", lower: "ы", sound: "y 后元音", sampleRu: "сын", sampleZh: "儿子"),
    LetterInfo(upper: "Ь", lower: "ь", sound: "软音符·不发音", sampleRu: "соль", sampleZh: "盐"),
    LetterInfo(upper: "Э", lower: "э", sound: "e（埃）", sampleRu: "этаж", sampleZh: "楼层"),
    LetterInfo(upper: "Ю", lower: "ю", sound: "yu（尤）", sampleRu: "юг", sampleZh: "南方"),
    LetterInfo(upper: "Я", lower: "я", sound: "ya（亚）", sampleRu: "яблоко", sampleZh: "苹果")
]

/// 俄文软键盘布局（ЙЦУКЕН）。
let RU_KEYBOARD_ROWS: [[String]] = [
    ["й", "ц", "у", "к", "е", "н", "г", "ш", "щ", "з", "х", "ъ"],
    ["ф", "ы", "в", "а", "п", "р", "о", "л", "д", "ж", "э"],
    ["я", "ч", "с", "м", "и", "т", "ь", "б", "ю", "ё"]
]

/// 俄语谚语与常用句（用户还没导入词库时作为兜底展示）。
let RUSSIAN_QUOTES: [(ru: String, zh: String)] = [
    ("Век живи — век учись.", "活到老，学到老。"),
    ("Повторение — мать учения.", "复习是学习之母。"),
    ("Без труда не выловишь и рыбку из пруда.", "不付出劳动，连池塘里的鱼也钓不上来。"),
    ("Утро вечера мудренее.", "清晨比夜晚更有智慧（先睡一觉再说）。"),
    ("Тише едешь — дальше будешь.", "走得慢些，反而走得更远。"),
    ("Не откладывай на завтра то, что можно сделать сегодня.", "今日事今日毕。"),
    ("Знание — сила.", "知识就是力量。"),
    ("Семь раз отмерь, один раз отрежь.", "量七次，剪一次（三思而后行）。"),
    ("Дорогу осилит идущий.", "路是靠走出来的。"),
    ("Маленькое дело лучше большого безделья.", "做点小事也强过什么都不做。"),
    ("Кто рано встаёт, тому Бог даёт.", "早起的人有回报。"),
    ("Всё хорошо, что хорошо кончается.", "结局好，一切都好。"),
    ("Терпение и труд всё перетрут.", "耐心和努力能克服一切。"),
    ("Москва не сразу строилась.", "冰冻三尺非一日之寒。"),
    ("Лучше поздно, чем никогда.", "迟做总比不做好。")
]
