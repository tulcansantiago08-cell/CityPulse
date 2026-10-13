package com.citypulse.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Festival
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.TheaterComedy
import androidx.compose.ui.graphics.vector.ImageVector
import com.citypulse.domain.model.Category

/** Opción del filtro de categorías; `category == null` es "Todo". */
data class CategoryOption(val category: Category?, val label: String, val icon: ImageVector, val imageUrl: String)

private fun unsplash(id: String) = "https://images.unsplash.com/$id?auto=format&fit=crop&w=200&q=60"

/** Mismo orden e imágenes que las stories del prototipo. */
val CategoryOptions = listOf(
    CategoryOption(null, "Todo", Icons.Rounded.Apps, unsplash("photo-1514525253161-7a46d19cd819")),
    CategoryOption(Category.Music, Category.Music.label, Icons.Rounded.MusicNote, unsplash("photo-1501386761578-eac5c94b800a")),
    CategoryOption(Category.Markets, Category.Markets.label, Icons.Rounded.Storefront, unsplash("photo-1488459716781-31db52582fe9")),
    CategoryOption(Category.Fairs, Category.Fairs.label, Icons.Rounded.Festival, unsplash("photo-1533174072545-7a4b6ad7a6c3")),
    CategoryOption(Category.Culture, Category.Culture.label, Icons.Rounded.TheaterComedy, unsplash("photo-1503095396549-807759245b35")),
    CategoryOption(Category.Food, Category.Food.label, Icons.Rounded.Restaurant, unsplash("photo-1555396273-367ea4eb4db5")),
    CategoryOption(Category.Art, Category.Art.label, Icons.Rounded.Palette, unsplash("photo-1518998053901-5348d3961a04")),
)
