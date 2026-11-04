package com.local.snldemo.ui.components

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Patterns
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.local.snldemo.data.local.NotificationEntity
import com.local.snldemo.ui.i18n.LocalAppStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Loads a local WebP image without relying on third-party internet libraries.
 */
@Composable
fun LocalFileImage(
    path: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    fallbackIcon: ImageVector = Icons.Default.Notifications
) {
    val imageBitmap by produceState<ImageBitmap?>(initialValue = null, key1 = path) {
        value = withContext(Dispatchers.IO) {
            if (!path.isNullOrBlank()) {
                try {
                    val file = File(path)
                    if (file.exists() && file.length() > 0) {
                        BitmapFactory.decodeFile(path)?.asImageBitmap()
                    } else null
                } catch (e: Exception) {
                    null
                }
            } else null
        }
    }

    if (imageBitmap != null) {
        Image(
            bitmap = imageBitmap!!,
            contentDescription = null,
            modifier = modifier,
            contentScale = contentScale
        )
    } else {
        Box(
            modifier = modifier.background(Color(0xFF20202E)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = fallbackIcon,
                contentDescription = null,
                tint = Color(0xFF8E8E9F),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * AMOLED-styled Notification Card with Header, Quick Actions, Badges, Clickable Links, and Image Preview.
 */
@Composable
fun NotificationCard(
    item: NotificationEntity,
    showAppBadge: Boolean = false,
    onToggleStar: () -> Unit,
    onOpenApp: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val clipboardManager = LocalClipboardManager.current

    val formattedTime = remember(item.timestamp) {
        SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(item.timestamp))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF242434), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF16161E)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header Row: Avatar + Sender Name + Timestamp + Quick Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sender Avatar (64x64 WebP resized)
                LocalFileImage(
                    path = item.senderAvatarPath,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                )

                Spacer(modifier = Modifier.width(10.dp))

                // Sender Title & Metadata
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.senderName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = formattedTime,
                            fontSize = 11.sp,
                            color = Color(0xFF8E8E9F)
                        )

                        if (showAppBadge && item.appName.isNotBlank()) {
                            Surface(
                                color = Color(0xFF252538),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = item.appName,
                                    fontSize = 10.sp,
                                    color = Color(0xFF90CAF9),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }

                // Quick Action Bar on Top Right
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Copy Button
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(item.content))
                            Toast.makeText(context, strings.actionCopiedToast, Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = strings.actionCopy,
                            tint = Color(0xFFA4B0BE),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // 2. Star / Pin Button
                    IconButton(
                        onClick = onToggleStar,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = if (item.isStarred) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = strings.actionStar,
                            tint = if (item.isStarred) Color(0xFFFFD700) else Color(0xFFA4B0BE),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // 3. Open Target App Button
                    IconButton(
                        onClick = onOpenApp,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = strings.actionOpenApp,
                            tint = Color(0xFFA4B0BE),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // 4. Delete Single Notification Button
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = strings.actionDelete,
                            tint = Color(0xFFE55039),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Badges Row (Unsent / Removed, Video Message)
            if (item.isRemoved || item.isVideoMessage) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (item.isRemoved) {
                        Surface(
                            color = Color(0xFF381519),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF5252))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = strings.badgeRemoved,
                                    color = Color(0xFFFF6B6B),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (item.removedAt != null) {
                                    val recallTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(item.removedAt))
                                    Text(
                                        text = " ($recallTime)",
                                        color = Color(0xFFFFA8A8),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }

                    if (item.isVideoMessage) {
                        Surface(
                            color = Color(0xFF142B28),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00D2D3))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Videocam,
                                    contentDescription = null,
                                    tint = Color(0xFF00D2D3),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Video",
                                    color = Color(0xFF00D2D3),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Clickable Content with URL detection
            val annotatedContent = remember(item.content) {
                buildAnnotatedContent(item.content)
            }

            ClickableText(
                text = annotatedContent,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFFE2E2EC),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                ),
                onClick = { offset ->
                    annotatedContent.getStringAnnotations(tag = "URL", start = offset, end = offset)
                        .firstOrNull()?.let { annotation ->
                            try {
                                var url = annotation.item
                                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                                    url = "https://$url"
                                }
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                }
            )
        }
    }
}

/**
 * Builds an AnnotatedString that finds all URLs and styles them as clickable links.
 */
private fun buildAnnotatedContent(rawText: String): AnnotatedString {
    return buildAnnotatedString {
        val matcher = Patterns.WEB_URL.matcher(rawText)
        var lastIndex = 0
        while (matcher.find()) {
            val start = matcher.start()
            val end = matcher.end()
            val url = matcher.group()

            if (start > lastIndex) {
                append(rawText.substring(lastIndex, start))
            }

            pushStringAnnotation(tag = "URL", annotation = url)
            pushStyle(
                SpanStyle(
                    color = Color(0xFF4E95FF),
                    textDecoration = TextDecoration.Underline,
                    fontWeight = FontWeight.Medium
                )
            )
            append(url)
            pop()
            pop()

            lastIndex = end
        }

        if (lastIndex < rawText.length) {
            append(rawText.substring(lastIndex))
        }
    }
}

