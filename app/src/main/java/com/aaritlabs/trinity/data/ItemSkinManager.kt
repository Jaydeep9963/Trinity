package com.aaritlabs.trinity.data

import android.content.Context
import com.aaritlabs.trinity.model.TileType

data class ItemSkin(
    val id: String,
    val name: String,
    val iconEmoji: String,
    val items: List<String>
) {
    fun getItemForType(type: TileType): String {
        if (items.isEmpty()) return type.emoji
        val index = type.ordinal.coerceIn(0, items.size - 1)
        return items[index]
    }
}

object ItemSkinManager {

    val FRUITS = ItemSkin(
        id = "skin_fruits",
        name = "Fruits & Food",
        iconEmoji = "\uD83C\uDF53", // 🍓
        items = listOf(
            "\uD83C\uDF4E", // 1 Apple
            "\uD83C\uDF4C", // 2 Banana
            "\uD83C\uDF53", // 3 Strawberry
            "\uD83C\uDF4A", // 4 Orange
            "\uD83E\uDD5D", // 5 Kiwi
            "\uD83C\uDF47", // 6 Grape
            "\uD83C\uDF49", // 7 Watermelon
            "\uD83C\uDF4D", // 8 Pineapple
            "\uD83C\uDF52", // 9 Cherry
            "\uD83E\uDED0", // 10 Blueberry
            "\uD83C\uDF51", // 11 Peach
            "\uD83C\uDF4B", // 12 Lemon
            "\uD83E\uDD51", // 13 Avocado
            "\uD83E\uDD55", // 14 Carrot
            "\uD83C\uDF3D", // 15 Corn
            "\uD83E\uDD65", // 16 Coconut
            "\uD83E\uDD6D", // 17 Mango
            "\uD83C\uDF48", // 18 Melon
            "\uD83C\uDF36\uFE0F" // 19 Pepper
        )
    )

    val FLOWERS = ItemSkin(
        id = "skin_flowers",
        name = "Nature & Flowers",
        iconEmoji = "\uD83C\uDF3A", // 🌺
        items = listOf(
            "\uD83C\uDF39", // 1 Rose
            "\uD83C\uDF3B", // 2 Sunflower
            "\uD83C\uDF40", // 3 Clover
            "\uD83C\uDF37", // 4 Tulip
            "\uD83E\uDEB7", // 5 Lotus
            "\uD83C\uDF38", // 6 Cherry Blossom
            "\uD83C\uDF3A", // 7 Hibiscus
            "\uD83C\uDF3C", // 8 Daisy
            "\uD83D\uDC90", // 9 Bouquet
            "\uD83C\uDF41", // 10 Maple Leaf
            "\uD83C\uDF3F", // 11 Herb
            "\uD83C\uDF34", // 12 Palm Tree
            "\uD83C\uDF35", // 13 Cactus
            "\uD83C\uDF3E", // 14 Rice
            "\uD83C\uDF44", // 15 Mushroom
            "\uD83C\uDF42", // 16 Fallen Leaf
            "\uD83C\uDF31", // 17 Seedling
            "\uD83C\uDF32", // 18 Pine Tree
            "\uD83C\uDF30"  // 19 Chestnut
        )
    )

    val EMOTIONS = ItemSkin(
        id = "skin_emotions",
        name = "Smileys & Emotions",
        iconEmoji = "\uD83E\uDD29", // 🤩
        items = listOf(
            "\uD83D\uDE00", // 1 Grinning
            "\uD83D\uDE0E", // 2 Sunglasses
            "\uD83E\uDD29", // 3 Star Eyes
            "\uD83E\uDD70", // 4 Smile Hearts
            "\uD83D\uDE0D", // 5 Heart Eyes
            "\uD83E\uDD17", // 6 Hugging
            "\uD83E\uDD73", // 7 Party
            "\uD83D\uDE02", // 8 Laughing Tears
            "\uD83D\uDC96", // 9 Sparkle Heart
            "\uD83D\uDE0B", // 10 Yummy
            "\uD83D\uDE07", // 11 Halo
            "\uD83D\uDE1C", // 12 Wink Tongue
            "\uD83E\uDD24", // 13 Drool
            "\uD83E\uDD13", // 14 Nerd
            "\uD83D\uDE3B", // 15 Cat Heart Eyes
            "\uD83D\uDC8E", // 16 Diamond
            "\u2B50",       // 17 Star
            "\uD83D\uDD25", // 18 Fire
            "\uD83D\uDCAB"  // 19 Dizzy Star
        )
    )

    val ALL_SKINS = listOf(FRUITS, FLOWERS, EMOTIONS)

    fun getSkinById(id: String): ItemSkin {
        return ALL_SKINS.find { it.id == id } ?: FRUITS
    }

    fun getEmojiForType(context: Context, type: TileType): String {
        val prefs = GamePreferences(context)
        val skin = getSkinById(prefs.equippedItemSkin)
        return skin.getItemForType(type)
    }
}
