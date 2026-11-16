package com.aaritlabs.trinity.model

enum class TileType(
    val displayName: String,
    val emoji: String,
    val iconRes: Int = 0
) {
    APPLE("Apple", "\uD83C\uDF4E"),
    BANANA("Banana", "\uD83C\uDF4C"),
    STRAWBERRY("Strawberry", "\uD83C\uDF53"),
    ORANGE("Orange", "\uD83C\uDF4A"),
    KIWI("Kiwi", "\uD83E\uDD5D"),
    GRAPE("Grape", "\uD83C\uDF47"),
    WATERMELON("Watermelon", "\uD83C\uDF49"),
    PINEAPPLE("Pineapple", "\uD83C\uDF4D"),
    CHERRY("Cherry", "\uD83C\uDF52"),
    BLUEBERRY("Blueberry", "\uD83E\uDED0"),
    PEACH("Peach", "\uD83C\uDF51"),
    LEMON("Lemon", "\uD83C\uDF4B"),
    AVOCADO("Avocado", "\uD83E\uDD51"),
    CLOVER("Clover", "\uD83C\uDF40"),
    SUNFLOWER("Sunflower", "\uD83C\uDF3B"),
    ROSE("Rose", "\uD83C\uDF39"),
    LEAF("Leaf", "\uD83C\uDF41"),
    HEART("Heart", "\uD83D\uDC96"),
    STAR("Star", "\u2B50")
}
