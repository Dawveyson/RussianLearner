/**
 * 西里尔字母笔画数据（笔顺）。
 *
 * 由 tools/glyph_strokes.py 维护，用 tools/render_glyphs.py 渲染核对过字形后再生成，
 * 要改字形请改 Python 数据源然后重新生成本文件。
 *
 * 笔画类型 StrokeKind：
 *  - LINE  折线，拐角保持尖角（Е Ц Ч П Н 等含直角的字母）
 *  - CURVE 平滑曲线（Б 的肚子、О、С、Э …）
 *  - LOOP  闭合圆圈（О、Ф、а 的外圈）
 *
 * 坐标 0..1 归一化，y 向下；渲染时按画布尺寸缩放。
 */
package com.example.rulearn.ui.alphabet.dict

enum class StrokeKind { LINE, CURVE, LOOP }

/** 一笔：归一化采样点 + 类型。 */
data class GlyphStroke(val pts: List<Pair<Float, Float>>, val kind: StrokeKind)

object CyrillicStrokes {

    // 大写 33 个字母
    private val UPPER: Map<Char, List<GlyphStroke>> = mapOf(
        'А' to listOf(GlyphStroke(listOf((0.5f to 0.05f), (0.05f to 0.95f)), StrokeKind.LINE), GlyphStroke(listOf((0.5f to 0.05f), (0.95f to 0.95f)), StrokeKind.LINE), GlyphStroke(listOf((0.21f to 0.63f), (0.79f to 0.63f)), StrokeKind.LINE)),
        'Б' to listOf(GlyphStroke(listOf((0.22f to 0.05f), (0.22f to 0.95f)), StrokeKind.LINE), GlyphStroke(listOf((0.22f to 0.05f), (0.85f to 0.05f)), StrokeKind.LINE), GlyphStroke(listOf((0.22f to 0.5f), (0.55f to 0.5f), (0.8f to 0.72f), (0.52f to 0.93f), (0.22f to 0.93f)), StrokeKind.CURVE)),
        'В' to listOf(GlyphStroke(listOf((0.22f to 0.05f), (0.22f to 0.95f)), StrokeKind.LINE), GlyphStroke(listOf((0.22f to 0.05f), (0.6f to 0.05f), (0.78f to 0.2f), (0.6f to 0.45f), (0.22f to 0.45f)), StrokeKind.CURVE), GlyphStroke(listOf((0.22f to 0.45f), (0.65f to 0.45f), (0.88f to 0.7f), (0.62f to 0.93f), (0.22f to 0.93f)), StrokeKind.CURVE)),
        'Г' to listOf(GlyphStroke(listOf((0.22f to 0.05f), (0.22f to 0.95f)), StrokeKind.LINE), GlyphStroke(listOf((0.22f to 0.05f), (0.85f to 0.05f)), StrokeKind.LINE)),
        'Д' to listOf(GlyphStroke(listOf((0.38f to 0.05f), (0.38f to 0.7f)), StrokeKind.LINE), GlyphStroke(listOf((0.15f to 0.7f), (0.88f to 0.7f)), StrokeKind.LINE), GlyphStroke(listOf((0.28f to 0.7f), (0.12f to 0.95f)), StrokeKind.LINE), GlyphStroke(listOf((0.72f to 0.7f), (0.88f to 0.95f)), StrokeKind.LINE)),
        'Е' to listOf(GlyphStroke(listOf((0.85f to 0.05f), (0.25f to 0.05f), (0.25f to 0.95f), (0.85f to 0.95f)), StrokeKind.LINE), GlyphStroke(listOf((0.25f to 0.5f), (0.7f to 0.5f)), StrokeKind.LINE)),
        'Ё' to listOf(GlyphStroke(listOf((0.85f to 0.05f), (0.25f to 0.05f), (0.25f to 0.95f), (0.85f to 0.95f)), StrokeKind.LINE), GlyphStroke(listOf((0.25f to 0.5f), (0.7f to 0.5f)), StrokeKind.LINE), GlyphStroke(listOf((0.38f to 0.2f), (0.46f to 0.12f)), StrokeKind.LINE), GlyphStroke(listOf((0.62f to 0.2f), (0.54f to 0.12f)), StrokeKind.LINE)),
        'Ж' to listOf(GlyphStroke(listOf((0.5f to 0.05f), (0.5f to 0.95f)), StrokeKind.LINE), GlyphStroke(listOf((0.13f to 0.05f), (0.13f to 0.95f)), StrokeKind.LINE), GlyphStroke(listOf((0.87f to 0.05f), (0.87f to 0.95f)), StrokeKind.LINE), GlyphStroke(listOf((0.13f to 0.2f), (0.87f to 0.8f)), StrokeKind.LINE), GlyphStroke(listOf((0.87f to 0.2f), (0.13f to 0.8f)), StrokeKind.LINE)),
        'З' to listOf(GlyphStroke(listOf((0.25f to 0.15f), (0.62f to 0.05f), (0.82f to 0.25f), (0.55f to 0.5f), (0.85f to 0.72f), (0.6f to 0.95f), (0.22f to 0.85f)), StrokeKind.CURVE)),
        'И' to listOf(GlyphStroke(listOf((0.18f to 0.05f), (0.18f to 0.95f)), StrokeKind.LINE), GlyphStroke(listOf((0.82f to 0.05f), (0.82f to 0.95f)), StrokeKind.LINE), GlyphStroke(listOf((0.18f to 0.05f), (0.82f to 0.95f)), StrokeKind.LINE)),
        'Й' to listOf(GlyphStroke(listOf((0.18f to 0.05f), (0.18f to 0.95f)), StrokeKind.LINE), GlyphStroke(listOf((0.82f to 0.05f), (0.82f to 0.95f)), StrokeKind.LINE), GlyphStroke(listOf((0.18f to 0.05f), (0.82f to 0.95f)), StrokeKind.LINE), GlyphStroke(listOf((0.38f to 0.22f), (0.46f to 0.14f)), StrokeKind.LINE), GlyphStroke(listOf((0.62f to 0.22f), (0.54f to 0.14f)), StrokeKind.LINE)),
        'К' to listOf(GlyphStroke(listOf((0.22f to 0.05f), (0.22f to 0.95f)), StrokeKind.LINE), GlyphStroke(listOf((0.82f to 0.05f), (0.22f to 0.52f)), StrokeKind.LINE), GlyphStroke(listOf((0.38f to 0.42f), (0.84f to 0.95f)), StrokeKind.LINE)),
        'Л' to listOf(GlyphStroke(listOf((0.1f to 0.95f), (0.5f to 0.08f), (0.9f to 0.95f)), StrokeKind.LINE), GlyphStroke(listOf((0.1f to 0.95f), (0.9f to 0.95f)), StrokeKind.LINE)),
        'М' to listOf(GlyphStroke(listOf((0.12f to 0.95f), (0.12f to 0.05f), (0.5f to 0.62f), (0.88f to 0.05f), (0.88f to 0.95f)), StrokeKind.LINE)),
        'Н' to listOf(GlyphStroke(listOf((0.2f to 0.05f), (0.2f to 0.95f)), StrokeKind.LINE), GlyphStroke(listOf((0.8f to 0.05f), (0.8f to 0.95f)), StrokeKind.LINE), GlyphStroke(listOf((0.2f to 0.5f), (0.8f to 0.5f)), StrokeKind.LINE)),
        'О' to listOf(GlyphStroke(listOf((0.5f to 0.05f), (0.8f to 0.2f), (0.9f to 0.5f), (0.8f to 0.8f), (0.5f to 0.95f), (0.2f to 0.8f), (0.1f to 0.5f), (0.2f to 0.2f), (0.5f to 0.05f)), StrokeKind.LOOP)),
        'П' to listOf(GlyphStroke(listOf((0.2f to 0.05f), (0.2f to 0.95f)), StrokeKind.LINE), GlyphStroke(listOf((0.8f to 0.05f), (0.8f to 0.95f)), StrokeKind.LINE), GlyphStroke(listOf((0.2f to 0.05f), (0.8f to 0.05f)), StrokeKind.LINE)),
        'Р' to listOf(GlyphStroke(listOf((0.22f to 0.05f), (0.22f to 0.95f)), StrokeKind.LINE), GlyphStroke(listOf((0.22f to 0.05f), (0.62f to 0.05f), (0.82f to 0.28f), (0.62f to 0.52f), (0.22f to 0.52f)), StrokeKind.CURVE)),
        'С' to listOf(GlyphStroke(listOf((0.85f to 0.2f), (0.6f to 0.05f), (0.3f to 0.1f), (0.13f to 0.4f), (0.2f to 0.75f), (0.5f to 0.95f), (0.8f to 0.85f)), StrokeKind.CURVE)),
        'Т' to listOf(GlyphStroke(listOf((0.08f to 0.05f), (0.92f to 0.05f)), StrokeKind.LINE), GlyphStroke(listOf((0.5f to 0.05f), (0.5f to 0.95f)), StrokeKind.LINE)),
        'У' to listOf(GlyphStroke(listOf((0.12f to 0.05f), (0.42f to 0.52f)), StrokeKind.LINE), GlyphStroke(listOf((0.88f to 0.05f), (0.42f to 0.52f), (0.3f to 0.78f), (0.22f to 0.95f), (0.45f to 0.95f), (0.58f to 0.82f)), StrokeKind.CURVE)),
        'Ф' to listOf(GlyphStroke(listOf((0.5f to 0.05f), (0.5f to 0.95f)), StrokeKind.LINE), GlyphStroke(listOf((0.5f to 0.25f), (0.74f to 0.32f), (0.82f to 0.52f), (0.7f to 0.72f), (0.5f to 0.78f), (0.3f to 0.72f), (0.18f to 0.52f), (0.26f to 0.32f), (0.5f to 0.25f)), StrokeKind.LOOP)),
        'Х' to listOf(GlyphStroke(listOf((0.15f to 0.05f), (0.85f to 0.95f)), StrokeKind.LINE), GlyphStroke(listOf((0.85f to 0.05f), (0.15f to 0.95f)), StrokeKind.LINE)),
        'Ц' to listOf(GlyphStroke(listOf((0.15f to 0.05f), (0.15f to 0.8f), (0.6f to 0.8f)), StrokeKind.LINE), GlyphStroke(listOf((0.6f to 0.05f), (0.6f to 1.0f), (0.95f to 1.0f)), StrokeKind.LINE)),
        'Ч' to listOf(GlyphStroke(listOf((0.2f to 0.05f), (0.2f to 0.48f), (0.8f to 0.48f)), StrokeKind.LINE), GlyphStroke(listOf((0.8f to 0.05f), (0.8f to 0.95f)), StrokeKind.LINE)),
        'Ш' to listOf(GlyphStroke(listOf((0.12f to 0.05f), (0.12f to 0.85f)), StrokeKind.LINE), GlyphStroke(listOf((0.5f to 0.05f), (0.5f to 0.85f)), StrokeKind.LINE), GlyphStroke(listOf((0.88f to 0.05f), (0.88f to 0.85f)), StrokeKind.LINE), GlyphStroke(listOf((0.12f to 0.85f), (0.88f to 0.85f)), StrokeKind.LINE)),
        'Щ' to listOf(GlyphStroke(listOf((0.12f to 0.05f), (0.12f to 0.85f)), StrokeKind.LINE), GlyphStroke(listOf((0.5f to 0.05f), (0.5f to 0.85f)), StrokeKind.LINE), GlyphStroke(listOf((0.88f to 0.05f), (0.88f to 0.85f)), StrokeKind.LINE), GlyphStroke(listOf((0.12f to 0.85f), (0.72f to 0.85f)), StrokeKind.LINE), GlyphStroke(listOf((0.72f to 0.05f), (0.72f to 1.0f), (1.0f to 1.0f)), StrokeKind.LINE)),
        'Ъ' to listOf(GlyphStroke(listOf((0.15f to 0.05f), (0.15f to 0.95f)), StrokeKind.LINE), GlyphStroke(listOf((0.15f to 0.05f), (0.8f to 0.05f)), StrokeKind.LINE), GlyphStroke(listOf((0.15f to 0.5f), (0.6f to 0.5f), (0.8f to 0.72f), (0.55f to 0.93f), (0.15f to 0.93f)), StrokeKind.CURVE)),
        'Ы' to listOf(GlyphStroke(listOf((0.15f to 0.05f), (0.15f to 0.95f)), StrokeKind.LINE), GlyphStroke(listOf((0.15f to 0.5f), (0.55f to 0.5f), (0.75f to 0.72f), (0.5f to 0.93f), (0.15f to 0.93f)), StrokeKind.CURVE), GlyphStroke(listOf((0.85f to 0.05f), (0.85f to 0.95f)), StrokeKind.LINE)),
        'Ь' to listOf(GlyphStroke(listOf((0.22f to 0.05f), (0.22f to 0.95f)), StrokeKind.LINE), GlyphStroke(listOf((0.22f to 0.5f), (0.62f to 0.5f), (0.82f to 0.72f), (0.58f to 0.93f), (0.22f to 0.93f)), StrokeKind.CURVE)),
        'Э' to listOf(GlyphStroke(listOf((0.8f to 0.2f), (0.5f to 0.05f), (0.2f to 0.2f), (0.3f to 0.5f), (0.5f to 0.5f), (0.8f to 0.5f)), StrokeKind.CURVE), GlyphStroke(listOf((0.5f to 0.5f), (0.82f to 0.75f), (0.5f to 0.95f), (0.18f to 0.82f)), StrokeKind.CURVE)),
        'Ю' to listOf(GlyphStroke(listOf((0.12f to 0.05f), (0.12f to 0.95f)), StrokeKind.LINE), GlyphStroke(listOf((0.5f to 0.05f), (0.5f to 0.95f)), StrokeKind.LINE), GlyphStroke(listOf((0.5f to 0.5f), (0.88f to 0.5f)), StrokeKind.CURVE), GlyphStroke(listOf((0.68f to 0.5f), (0.9f to 0.7f), (0.68f to 0.95f), (0.5f to 0.95f), (0.5f to 0.5f)), StrokeKind.CURVE)),
        'Я' to listOf(GlyphStroke(listOf((0.8f to 0.05f), (0.2f to 0.05f), (0.2f to 0.45f), (0.8f to 0.45f)), StrokeKind.LINE), GlyphStroke(listOf((0.8f to 0.45f), (0.62f to 0.7f), (0.2f to 0.95f)), StrokeKind.LINE), GlyphStroke(listOf((0.8f to 0.05f), (0.8f to 0.95f)), StrokeKind.LINE)),
    )

    // 小写（未单独列出的复用大写，渲染时缩到 x 高度）
    private val LOWER: Map<Char, List<GlyphStroke>> = mapOf(
        'б' to listOf(GlyphStroke(listOf((0.85f to 0.05f), (0.3f to 0.05f), (0.3f to 0.95f)), StrokeKind.LINE), GlyphStroke(listOf((0.3f to 0.05f), (0.28f to 0.22f), (0.28f to 0.5f)), StrokeKind.LINE), GlyphStroke(listOf((0.28f to 0.5f), (0.6f to 0.5f), (0.82f to 0.68f), (0.55f to 0.9f), (0.28f to 0.9f)), StrokeKind.CURVE)),
        'д' to listOf(GlyphStroke(listOf((0.38f to 0.25f), (0.38f to 0.75f)), StrokeKind.LINE), GlyphStroke(listOf((0.18f to 0.75f), (0.85f to 0.75f)), StrokeKind.LINE), GlyphStroke(listOf((0.3f to 0.75f), (0.12f to 1.0f)), StrokeKind.LINE), GlyphStroke(listOf((0.72f to 0.75f), (0.88f to 1.0f)), StrokeKind.LINE)),
        'з' to listOf(GlyphStroke(listOf((0.25f to 0.35f), (0.6f to 0.25f), (0.8f to 0.45f), (0.55f to 0.62f), (0.82f to 0.8f), (0.55f to 0.95f), (0.25f to 0.85f)), StrokeKind.CURVE)),
        'и' to listOf(GlyphStroke(listOf((0.2f to 0.25f), (0.2f to 0.95f)), StrokeKind.LINE), GlyphStroke(listOf((0.8f to 0.25f), (0.8f to 0.95f)), StrokeKind.LINE), GlyphStroke(listOf((0.2f to 0.25f), (0.8f to 0.95f)), StrokeKind.LINE)),
        'й' to listOf(GlyphStroke(listOf((0.2f to 0.25f), (0.2f to 0.95f)), StrokeKind.LINE), GlyphStroke(listOf((0.8f to 0.25f), (0.8f to 0.95f)), StrokeKind.LINE), GlyphStroke(listOf((0.2f to 0.25f), (0.8f to 0.95f)), StrokeKind.LINE), GlyphStroke(listOf((0.38f to 0.4f), (0.46f to 0.32f)), StrokeKind.LINE), GlyphStroke(listOf((0.62f to 0.4f), (0.54f to 0.32f)), StrokeKind.LINE)),
        'л' to listOf(GlyphStroke(listOf((0.15f to 0.95f), (0.5f to 0.28f), (0.85f to 0.95f)), StrokeKind.LINE), GlyphStroke(listOf((0.15f to 0.95f), (0.85f to 0.95f)), StrokeKind.LINE)),
        'п' to listOf(GlyphStroke(listOf((0.22f to 0.25f), (0.22f to 0.95f)), StrokeKind.LINE), GlyphStroke(listOf((0.78f to 0.25f), (0.78f to 0.95f)), StrokeKind.LINE), GlyphStroke(listOf((0.22f to 0.25f), (0.78f to 0.25f)), StrokeKind.LINE)),
        'т' to listOf(GlyphStroke(listOf((0.2f to 0.28f), (0.2f to 0.25f), (0.8f to 0.25f), (0.8f to 0.28f)), StrokeKind.LINE), GlyphStroke(listOf((0.35f to 0.25f), (0.35f to 0.95f)), StrokeKind.LINE), GlyphStroke(listOf((0.65f to 0.25f), (0.65f to 0.95f)), StrokeKind.LINE)),
        'у' to listOf(GlyphStroke(listOf((0.2f to 0.25f), (0.45f to 0.62f)), StrokeKind.LINE), GlyphStroke(listOf((0.82f to 0.25f), (0.45f to 0.62f), (0.3f to 0.82f), (0.22f to 0.98f), (0.45f to 0.98f), (0.58f to 0.85f)), StrokeKind.CURVE)),
        'ф' to listOf(GlyphStroke(listOf((0.5f to 0.2f), (0.5f to 1.0f)), StrokeKind.LINE), GlyphStroke(listOf((0.5f to 0.4f), (0.72f to 0.47f), (0.8f to 0.65f), (0.68f to 0.83f), (0.5f to 0.88f), (0.32f to 0.83f), (0.2f to 0.65f), (0.28f to 0.47f), (0.5f to 0.4f)), StrokeKind.LOOP)),
        'ц' to listOf(GlyphStroke(listOf((0.2f to 0.25f), (0.2f to 0.82f), (0.62f to 0.82f)), StrokeKind.LINE), GlyphStroke(listOf((0.62f to 0.25f), (0.62f to 1.0f), (0.95f to 1.0f)), StrokeKind.LINE)),
        'ч' to listOf(GlyphStroke(listOf((0.22f to 0.25f), (0.22f to 0.6f), (0.78f to 0.6f)), StrokeKind.LINE), GlyphStroke(listOf((0.78f to 0.25f), (0.78f to 0.95f)), StrokeKind.LINE)),
        'ш' to listOf(GlyphStroke(listOf((0.14f to 0.25f), (0.14f to 0.88f)), StrokeKind.LINE), GlyphStroke(listOf((0.5f to 0.25f), (0.5f to 0.88f)), StrokeKind.LINE), GlyphStroke(listOf((0.86f to 0.25f), (0.86f to 0.88f)), StrokeKind.LINE), GlyphStroke(listOf((0.14f to 0.88f), (0.86f to 0.88f)), StrokeKind.LINE)),
        'щ' to listOf(GlyphStroke(listOf((0.14f to 0.25f), (0.14f to 0.88f)), StrokeKind.LINE), GlyphStroke(listOf((0.5f to 0.25f), (0.5f to 0.88f)), StrokeKind.LINE), GlyphStroke(listOf((0.86f to 0.25f), (0.86f to 0.88f)), StrokeKind.LINE), GlyphStroke(listOf((0.14f to 0.88f), (0.7f to 0.88f)), StrokeKind.LINE), GlyphStroke(listOf((0.7f to 0.25f), (0.7f to 1.0f), (0.98f to 1.0f)), StrokeKind.LINE)),
        'ъ' to listOf(GlyphStroke(listOf((0.18f to 0.25f), (0.18f to 0.95f)), StrokeKind.LINE), GlyphStroke(listOf((0.18f to 0.25f), (0.82f to 0.25f)), StrokeKind.LINE), GlyphStroke(listOf((0.18f to 0.55f), (0.58f to 0.55f), (0.78f to 0.74f), (0.55f to 0.93f), (0.18f to 0.93f)), StrokeKind.CURVE)),
        'ы' to listOf(GlyphStroke(listOf((0.18f to 0.25f), (0.18f to 0.95f)), StrokeKind.LINE), GlyphStroke(listOf((0.18f to 0.55f), (0.55f to 0.55f), (0.75f to 0.74f), (0.52f to 0.93f), (0.18f to 0.93f)), StrokeKind.CURVE), GlyphStroke(listOf((0.85f to 0.25f), (0.85f to 0.95f)), StrokeKind.LINE)),
        'ь' to listOf(GlyphStroke(listOf((0.25f to 0.25f), (0.25f to 0.95f)), StrokeKind.LINE), GlyphStroke(listOf((0.25f to 0.55f), (0.62f to 0.55f), (0.82f to 0.74f), (0.58f to 0.93f), (0.25f to 0.93f)), StrokeKind.CURVE)),
        'э' to listOf(GlyphStroke(listOf((0.8f to 0.4f), (0.5f to 0.25f), (0.22f to 0.4f), (0.32f to 0.6f), (0.52f to 0.6f), (0.8f to 0.6f)), StrokeKind.CURVE), GlyphStroke(listOf((0.52f to 0.6f), (0.82f to 0.78f), (0.5f to 0.95f), (0.2f to 0.82f)), StrokeKind.CURVE)),
        'ю' to listOf(GlyphStroke(listOf((0.14f to 0.25f), (0.14f to 0.95f)), StrokeKind.LINE), GlyphStroke(listOf((0.5f to 0.25f), (0.5f to 0.95f)), StrokeKind.LINE), GlyphStroke(listOf((0.5f to 0.6f), (0.86f to 0.6f)), StrokeKind.CURVE), GlyphStroke(listOf((0.68f to 0.6f), (0.88f to 0.76f), (0.68f to 0.93f), (0.5f to 0.93f), (0.5f to 0.6f)), StrokeKind.CURVE)),
        'я' to listOf(GlyphStroke(listOf((0.82f to 0.25f), (0.25f to 0.25f), (0.25f to 0.55f), (0.82f to 0.55f)), StrokeKind.LINE), GlyphStroke(listOf((0.82f to 0.55f), (0.62f to 0.75f), (0.25f to 0.95f)), StrokeKind.LINE), GlyphStroke(listOf((0.82f to 0.25f), (0.82f to 0.95f)), StrokeKind.LINE)),
        'а' to listOf(GlyphStroke(listOf((0.62f to 0.3f), (0.4f to 0.24f), (0.18f to 0.42f), (0.18f to 0.72f), (0.4f to 0.92f), (0.62f to 0.76f)), StrokeKind.LOOP), GlyphStroke(listOf((0.62f to 0.3f), (0.62f to 0.95f)), StrokeKind.LINE)),
        'в' to listOf(GlyphStroke(listOf((0.25f to 0.25f), (0.25f to 0.95f)), StrokeKind.LINE), GlyphStroke(listOf((0.25f to 0.25f), (0.58f to 0.25f), (0.75f to 0.4f), (0.58f to 0.58f), (0.25f to 0.58f)), StrokeKind.CURVE), GlyphStroke(listOf((0.25f to 0.58f), (0.62f to 0.58f), (0.85f to 0.76f), (0.6f to 0.95f), (0.25f to 0.95f)), StrokeKind.CURVE)),
        'г' to listOf(GlyphStroke(listOf((0.2f to 0.25f), (0.2f to 0.95f)), StrokeKind.LINE), GlyphStroke(listOf((0.2f to 0.25f), (0.8f to 0.25f)), StrokeKind.LINE)),
        'е' to listOf(GlyphStroke(listOf((0.85f to 0.42f), (0.55f to 0.25f), (0.25f to 0.42f), (0.28f to 0.78f), (0.6f to 0.95f), (0.85f to 0.82f)), StrokeKind.CURVE)),
        'ж' to listOf(GlyphStroke(listOf((0.5f to 0.25f), (0.5f to 0.95f)), StrokeKind.LINE), GlyphStroke(listOf((0.15f to 0.25f), (0.15f to 0.95f)), StrokeKind.LINE), GlyphStroke(listOf((0.85f to 0.25f), (0.85f to 0.95f)), StrokeKind.LINE), GlyphStroke(listOf((0.15f to 0.4f), (0.85f to 0.8f)), StrokeKind.LINE), GlyphStroke(listOf((0.85f to 0.4f), (0.15f to 0.8f)), StrokeKind.LINE)),
        'к' to listOf(GlyphStroke(listOf((0.25f to 0.25f), (0.25f to 0.95f)), StrokeKind.LINE), GlyphStroke(listOf((0.8f to 0.25f), (0.25f to 0.62f)), StrokeKind.LINE), GlyphStroke(listOf((0.42f to 0.54f), (0.85f to 0.95f)), StrokeKind.LINE)),
        'м' to listOf(GlyphStroke(listOf((0.15f to 0.95f), (0.15f to 0.25f), (0.5f to 0.72f), (0.85f to 0.25f), (0.85f to 0.95f)), StrokeKind.LINE)),
        'н' to listOf(GlyphStroke(listOf((0.22f to 0.25f), (0.22f to 0.95f)), StrokeKind.LINE), GlyphStroke(listOf((0.78f to 0.25f), (0.78f to 0.95f)), StrokeKind.LINE), GlyphStroke(listOf((0.22f to 0.6f), (0.78f to 0.6f)), StrokeKind.LINE)),
        'о' to listOf(GlyphStroke(listOf((0.5f to 0.25f), (0.78f to 0.4f), (0.86f to 0.6f), (0.76f to 0.83f), (0.5f to 0.95f), (0.24f to 0.83f), (0.14f to 0.6f), (0.22f to 0.4f), (0.5f to 0.25f)), StrokeKind.LOOP)),
        'р' to listOf(GlyphStroke(listOf((0.25f to 0.25f), (0.25f to 1.05f)), StrokeKind.LINE), GlyphStroke(listOf((0.25f to 0.25f), (0.6f to 0.25f), (0.8f to 0.45f), (0.6f to 0.68f), (0.25f to 0.68f)), StrokeKind.CURVE)),
        'с' to listOf(GlyphStroke(listOf((0.82f to 0.4f), (0.58f to 0.25f), (0.3f to 0.32f), (0.15f to 0.6f), (0.25f to 0.88f), (0.55f to 0.95f), (0.8f to 0.85f)), StrokeKind.CURVE)),
        'х' to listOf(GlyphStroke(listOf((0.18f to 0.25f), (0.82f to 0.95f)), StrokeKind.LINE), GlyphStroke(listOf((0.82f to 0.25f), (0.18f to 0.95f)), StrokeKind.LINE)),
    )

    /** 形态与大写一致的小写：直接复用大写笔画。 */
    private val LOWER_FROM_UPPER: Map<Char, Char> = mapOf(
        'б' to 'Б', 'в' to 'В', 'ж' to 'Ж', 'к' to 'К', 'м' to 'М', 'н' to 'Н', 'о' to 'О', 'с' to 'С', 'у' to 'У', 'х' to 'Х'
    )

    /** 取字符的笔画；小写无定义时退化到对应大写。 */
    fun of(ch: Char): List<GlyphStroke> {
        LOWER[ch]?.let { return it }
        LOWER_FROM_UPPER[ch]?.let { up -> UPPER[up]?.let { return it } }
        if (ch.isLowerCase()) {
            UPPER[ch.uppercaseChar()]?.let { return it }
        }
        return UPPER[ch] ?: emptyList()
    }

    /** 字符是否有笔画数据。 */
    fun has(ch: Char): Boolean = of(ch).isNotEmpty()
}
