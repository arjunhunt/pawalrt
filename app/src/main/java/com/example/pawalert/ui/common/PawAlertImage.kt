package com.example.pawalert.ui.common

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.pawalert.ui.theme.Amber40

/**
 * Universal image loader that handles Base64 data URIs, HTTP URLs, content URIs,
 * and displays a dog-paw fallback when no photo is present.
 */
@Composable
fun PawAlertImage(
    photoUrl: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val decodedBitmap = remember(photoUrl) {
        if (photoUrl.isNotBlank() && (photoUrl.startsWith("data:") || photoUrl.contains("base64,"))) {
            try {
                val base64Data = if (photoUrl.contains("base64,")) {
                    photoUrl.substringAfter("base64,")
                } else {
                    photoUrl
                }
                val bytes = Base64.decode(base64Data, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            } catch (e: Throwable) {
                e.printStackTrace()
                null
            }
        } else {
            null
        }
    }

    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        when {
            decodedBitmap != null -> {
                Image(
                    bitmap = decodedBitmap,
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale
                )
            }
            photoUrl.startsWith("http://") || photoUrl.startsWith("https://") ||
                    photoUrl.startsWith("content://") || photoUrl.startsWith("file://") -> {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale
                )
            }
            else -> {
                Icon(
                    imageVector = Icons.Default.Pets,
                    contentDescription = null,
                    tint = Amber40.copy(alpha = 0.5f),
                    modifier = Modifier.size(48.dp)
                )
            }
        }
    }
}
