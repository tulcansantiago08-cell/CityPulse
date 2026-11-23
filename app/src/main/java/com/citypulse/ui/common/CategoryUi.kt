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
import com.citypulse.domain.i18n.tr
import com.citypulse.domain.model.Category

/** Opción del filtro de categorías; `category == null` es "Todo". */
data class CategoryOption(val category: Category?, val icon: ImageVector, val imageUrl: String) {
    /** Se lee al dibujar: sigue el idioma de la app aunque la lista se cree una sola vez. */
    val label: String get() = category?.displayLabel ?: tr("Todo", "All")
}

private fun unsplash(id: String) = "https://images.unsplash.com/$id?auto=format&fit=crop&w=200&q=60"

/** Mismo orden e imágenes que las stories del prototipo. */
val CategoryOptions = listOf(
    CategoryOption(null, Icons.Rounded.Apps, unsplash("photo-1514525253161-7a46d19cd819")),
    CategoryOption(Category.Music, Icons.Rounded.MusicNote, unsplash("photo-1501386761578-eac5c94b800a")),
    CategoryOption(Category.Markets, Icons.Rounded.Storefront, unsplash("photo-1488459716781-31db52582fe9")),
    CategoryOption(Category.Fairs, Icons.Rounded.Festival, unsplash("photo-1533174072545-7a4b6ad7a6c3")),
    CategoryOption(Category.Culture, Icons.Rounded.TheaterComedy, unsplash("photo-1503095396549-807759245b35")),
    CategoryOption(Category.Food, Icons.Rounded.Restaurant, unsplash("photo-1555396273-367ea4eb4db5")),
    CategoryOption(Category.Art, Icons.Rounded.Palette, unsplash("photo-1518998053901-5348d3961a04")),
)
