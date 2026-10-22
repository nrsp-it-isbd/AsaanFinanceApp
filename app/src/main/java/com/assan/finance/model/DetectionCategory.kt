package com.assan.finance.model

/**
 * Detection categories for the agricultural app.
 * Each has: API key, English title, Urdu title, emoji icon, description
 */
enum class DetectionCategory(
    val apiKey: String,
    val titleEn: String,
    val titleUr: String,
    val emoji: String,
    val descEn: String,
    val descUr: String,
    val color: Long
) {
    PLANT(
        apiKey  = "plant",
        titleEn = "Plant Health & Disease",
        titleUr = "پودوں کی صحت اور بیماری",
        emoji   = "🌿",
        descEn  = "Identify crop health, diseases, nutrient issues",
        descUr  = "فصلوں کی صحت، بیماریوں اور غذائی مسائل کی شناخت",
        color   = 0xFF2E7D32
    ),
    LIVESTOCK(
        apiKey  = "livestock",
        titleEn = "Livestock Health & Disease",
        titleUr = "مویشیوں کی صحت اور بیماری",
        emoji   = "🐄",
        descEn  = "Check livestock health, diagnosis & care",
        descUr  = "مویشیوں کی صحت، تشخیص اور دیکھ بھال کا جائزہ",
        color   = 0xFF5D4037
    ),
    POULTRY(
        apiKey  = "poultry",
        titleEn = "Poultry Health & Disease",
        titleUr = "مرغیوں کی صحت اور بیماری",
        emoji   = "🐔",
        descEn  = "Chicken diseases, health & farm bird diagnostics",
        descUr  = "مرغیوں کی بیماریاں، صحت اور پرندوں کی تشخیص",
        color   = 0xFFE65100
    ),
    MACHINERY(
        apiKey  = "machinery",
        titleEn = "Farm Machinery",
        titleUr = "زرعی مشینری",
        emoji   = "🚜",
        descEn  = "Tractors, implements, pumps & equipment",
        descUr  = "ٹریکٹر، آلات، پمپ اور مشینری",
        color   = 0xFF1565C0
    ),
    INSECT(
        apiKey  = "insect",
        titleEn = "Pest & Insect Detection",
        titleUr = "کیڑے مکوڑے اور آفات",
        emoji   = "🐛",
        descEn  = "Crop pests, beneficial insects & treatment",
        descUr  = "فصل کے کیڑے، مفید حشرات اور ان کا علاج",
        color   = 0xFF6A1B9A
    ),
    FISH(
        apiKey  = "fish",
        titleEn = "Fish Health & Disease",
        titleUr = "مچھلیوں کی صحت اور بیماری",
        emoji   = "🐟",
        descEn  = "Fish diseases, water quality & species diagnostics",
        descUr  = "مچھلیوں کی بیماریاں، پانی کی کوالٹی اور اقسام کی تشخیص",
        color   = 0xFF00695C
    ),
    GRAINS(
        apiKey  = "grains",
        titleEn = "Grains & Produce Quality",
        titleUr = "اناج اور پیداوار کی کوالٹی",
        emoji   = "🌾",
        descEn  = "Wheat, rice, cotton, vegetables & crop quality assessment",
        descUr  = "گندم، چاول، کپاس، سبزی اور فصل کی کوالٹی کا جائزہ",
        color   = 0xFFF57F17
    ),
    BIRDS(
        apiKey  = "birds",
        titleEn = "Wild Birds Identification",
        titleUr = "جنگلی پرندوں کی شناخت",
        emoji   = "🦅",
        descEn  = "Identify wild, migratory & game birds",
        descUr  = "جنگلی، ہجرتی اور شکاری پرندوں کی شناخت",
        color   = 0xFF0277BD
    ),
    COMPARE(
        apiKey  = "compare",
        titleEn = "Compare 2 Images",
        titleUr = "دو تصویروں کا موازنہ",
        emoji   = "⚖️",
        descEn  = "Compare two images for agricultural differences",
        descUr  = "زرعی فرق کے لیے دو تصاویر کا موازنہ کریں",
        color   = 0xFF00ACC1
    ),
    CHEMICALS(
        apiKey  = "chemicals",
        titleEn = "Agro Chemicals",
        titleUr = "زرعی کیمیکل",
        emoji   = "💊",
        descEn  = "Pesticides, fertilizers & herbicide labels",
        descUr  = "کیڑے مار دوا، کھاد اور جڑی بوٹی مار",
        color   = 0xFFBF360C
    ),
    SOIL(
        apiKey  = "soil",
        titleEn = "Soil Health & Quality",
        titleUr = "مٹی کی صحت اور کوالٹی",
        emoji   = "🌱",
        descEn  = "Soil type, fertility & land assessment",
        descUr  = "مٹی کی قسم، زرخیزی اور زمین کا جائزہ",
        color   = 0xFF4E342E
    ),
    SATELLITE(
        apiKey  = "satellite",
        titleEn = "Satellite / Cropland",
        titleUr = "سیٹلائٹ / کھیت کا جائزہ",
        emoji   = "🛰️",
        descEn  = "Aerial & satellite cropland analysis",
        descUr  = "فضائی اور سیٹلائٹ تصویر سے کھیت کا تجزیہ",
        color   = 0xFF00838F
    ),
    LAND_MARKING(
        apiKey  = "land_marking",
        titleEn = "Land Marking",
        titleUr = "زمین کی نشاندہی",
        emoji   = "🗺️",
        descEn  = "Mark agricultural land borders & calculate area",
        descUr  = "زرعی زمین کی حدود اور رقبہ کی نشاندہی",
        color   = 0xFFE65100
    );

    companion object {
        fun fromApiKey(key: String): DetectionCategory =
            values().find { it.apiKey == key } ?: PLANT
    }
}
