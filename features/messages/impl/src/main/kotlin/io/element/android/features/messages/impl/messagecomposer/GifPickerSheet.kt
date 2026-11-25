package io.element.android.features.messages.impl.messagecomposer

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.element.android.compound.theme.ElementTheme
import io.element.android.features.messages.impl.BuildConfig
import io.element.android.libraries.designsystem.theme.components.CircularProgressIndicator
import io.element.android.libraries.designsystem.theme.components.ModalBottomSheet
import io.element.android.libraries.designsystem.theme.components.Text
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

private data class GiphyGif(
    val id: String,
    val previewUrl: String,
    val originalUrl: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GifPickerSheet(
    onDismiss: () -> Unit,
    onGifSelected: (Uri) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var gifs by remember { mutableStateOf<List<GiphyGif>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var isSending by remember { mutableStateOf(false) }

    LaunchedEffect(query) {
        delay(400)
        isLoading = true
        gifs = withContext(Dispatchers.IO) { loadGifs(query) }
        isLoading = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        scrollable = false,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(480.dp)
                .padding(horizontal = 12.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(ElementTheme.colors.bgSubtleSecondary)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                if (query.isEmpty()) {
                    Text(
                        text = "Поиск GIF",
                        color = ElementTheme.colors.textSecondary,
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = ElementTheme.typography.fontBodyLgRegular.copy(
                        color = ElementTheme.colors.textPrimary,
                    ),
                    cursorBrush = SolidColor(ElementTheme.colors.textPrimary),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 8.dp),
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(gifs, key = { it.id }) { gif ->
                        AsyncImage(
                            model = gif.previewUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(enabled = !isSending) {
                                    scope.launch {
                                        isSending = true
                                        val uri = withContext(Dispatchers.IO) { downloadGif(context, gif) }
                                        isSending = false
                                        if (uri != null) onGifSelected(uri)
                                    }
                                },
                        )
                    }
                }
                if (isLoading || isSending) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }
}

private fun loadGifs(query: String): List<GiphyGif> {
    val key = BuildConfig.GIPHY_API_KEY
    if (key.isEmpty()) return emptyList()
    return try {
        val endpoint = if (query.isBlank()) "trending" else "search"
        val q = if (query.isBlank()) "" else "&q=" + URLEncoder.encode(query.trim(), "UTF-8")
        val url = URL("https://api.giphy.com/v1/gifs/$endpoint?api_key=$key&limit=30&rating=pg-13$q")
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        val text = connection.inputStream.bufferedReader().use { it.readText() }
        connection.disconnect()
        val data = JSONObject(text).getJSONArray("data")
        List(data.length()) { i ->
            val item = data.getJSONObject(i)
            val images = item.getJSONObject("images")
            GiphyGif(
                id = item.getString("id"),
                previewUrl = images.getJSONObject("fixed_width").getString("url"),
                originalUrl = images.getJSONObject("original").getString("url"),
            )
        }
    } catch (e: Exception) {
        emptyList()
    }
}

private fun downloadGif(context: Context, gif: GiphyGif): Uri? {
    return try {
        val dir = File(context.cacheDir, "gifs").apply { mkdirs() }
        val file = File(dir, "giphy_${gif.id}.gif")
        if (!file.exists()) {
            URL(gif.originalUrl).openStream().use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
        }
        Uri.fromFile(file)
    } catch (e: Exception) {
        null
    }
}
