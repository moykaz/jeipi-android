package io.element.android.features.messages.impl

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import io.element.android.compound.theme.ElementTheme

@Composable
fun ChatWallpaper(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val wallpaper = if (ElementTheme.isLightTheme) R.drawable.chat_wallpaper_light else R.drawable.chat_wallpaper_dark
    Box(modifier = modifier) {
        Image(
            painter = painterResource(wallpaper),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop,
        )
        content()
    }
}
