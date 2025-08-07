package com.gcancino.levelingup.ui.components

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

@Composable
fun PhotoItem(
    uri: Uri,
    onDelete: () -> Unit
) {
    Box() {
        AsyncImage(
            model = uri,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            contentScale = ContentScale.Crop
        )
        IconButton(
            onClick = onDelete,
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = "Delete Photo",
                tint = Color(0xFF860000)
            )
        }
    }
}