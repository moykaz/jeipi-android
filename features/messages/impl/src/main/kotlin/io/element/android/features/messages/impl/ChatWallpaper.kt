package io.element.android.features.messages.impl

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.res.imageResource
import io.element.android.compound.theme.ElementTheme

@Composable
fun Modifier.chatWallpaper(): Modifier {
    val isLight = ElementTheme.isLightTheme
    val bitmap = ImageBitmap.imageResource(
        if (isLight) R.drawable.chat_wallpaper_light else R.drawable.chat_wallpaper_dark
    )
    val brush = remember(bitmap) {
        ShaderBrush(ImageShader(bitmap, TileMode.Repeated, TileMode.Repeated))
    }
    val backgroundColor = if (isLight) Color(0xFFEDF1FF) else Color(0xFF0A0F2C)
    return this
        .background(backgroundColor)
        .drawBehind { drawRect(brush) }
}
