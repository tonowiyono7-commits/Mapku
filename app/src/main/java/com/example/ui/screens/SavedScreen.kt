package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.SavedLocationEntity
import com.example.location.GeoMath
import com.example.ui.components.SaveFavoriteNameDialog
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.MapBlue
import com.example.ui.theme.WarningAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SavedScreen(
    favorites: List<SavedLocationEntity>,
    history: List<SavedLocationEntity>,
    onTeleportTo: (SavedLocationEntity) -> Unit,
    onRenameFavorite: (Long, String) -> Unit,
    onSaveHistoryAsFavorite: (String, String, Double, Double) -> Unit,
    onDeleteLocation: (Long) -> Unit,
    onClearHistory: () -> Unit,
    onNavigateBackToMap: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onNavigateBackToMap()
    }

    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var dialogTarget by remember { mutableStateOf<Pair<SavedLocationEntity, Boolean>?>(null) }
    val activeList = if (selectedTab == 0) favorites else history

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.nav_saved),
                    style = MaterialTheme.typography.headlineSmall
                )
                if (selectedTab == 1 && history.isNotEmpty()) {
                    TextButton(
                        onClick = onClearHistory,
                        modifier = Modifier.testTag("clear_history_button")
                    ) {
                        Text(stringResource(R.string.saved_clear_history))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    modifier = Modifier.testTag("tab_favorites")
                ) {
                    Text("${stringResource(R.string.saved_tab_favorites)} (${favorites.size})")
                }
                SegmentedButton(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    modifier = Modifier.testTag("tab_history")
                ) {
                    Text("${stringResource(R.string.saved_tab_history)} (${history.size})")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (activeList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(MapBlue.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (selectedTab == 0) Icons.Filled.BookmarkBorder else Icons.Filled.History,
                                contentDescription = null,
                                tint = MapBlue,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(
                                if (selectedTab == 0) R.string.saved_empty_favorites else R.string.saved_empty_history
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(activeList, key = { it.id }) { item ->
                        SavedLocationCard(
                            item = item,
                            isFavoriteTab = selectedTab == 0,
                            onSelect = { onTeleportTo(item) },
                            onEditOrPromote = {
                                dialogTarget = item to (selectedTab == 0)
                            },
                            onDelete = { onDeleteLocation(item.id) }
                        )
                    }
                }
            }
        }
    }

    dialogTarget?.let { (entity, isRename) ->
        SaveFavoriteNameDialog(
            titleText = stringResource(
                if (isRename) R.string.favorite_rename_title else R.string.favorite_dialog_title
            ),
            initialName = entity.name,
            latitude = entity.latitude,
            longitude = entity.longitude,
            onDismiss = { dialogTarget = null },
            onConfirm = { newName ->
                if (isRename) {
                    onRenameFavorite(entity.id, newName)
                    Toast.makeText(context, "Nama diperbarui", Toast.LENGTH_SHORT).show()
                } else {
                    onSaveHistoryAsFavorite(newName, entity.address, entity.latitude, entity.longitude)
                    Toast.makeText(context, "\"$newName\" disimpan ke Favorit", Toast.LENGTH_SHORT).show()
                }
                dialogTarget = null
            }
        )
    }
}

@Composable
private fun SavedLocationCard(
    item: SavedLocationEntity,
    isFavoriteTab: Boolean,
    onSelect: () -> Unit,
    onEditOrPromote: () -> Unit,
    onDelete: () -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("dd MMM • HH:mm", Locale.forLanguageTag("id-ID")) }
    val timestampLabel = remember(item.createdAt) { timeFormat.format(Date(item.createdAt)) }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onSelect)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        if (isFavoriteTab) WarningAmber.copy(alpha = 0.14f)
                        else MapBlue.copy(alpha = 0.12f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isFavoriteTab) Icons.Filled.Star else Icons.Filled.History,
                    contentDescription = null,
                    tint = if (isFavoriteTab) WarningAmber else MapBlue,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (isFavoriteTab) item.address else "$timestampLabel • ${item.address}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = GeoMath.formatCoords(item.latitude, item.longitude),
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMono),
                    color = MapBlue
                )
            }

            IconButton(
                onClick = onEditOrPromote,
                modifier = Modifier.testTag("edit_or_fav_saved_${item.id}")
            ) {
                Icon(
                    imageVector = if (isFavoriteTab) Icons.Filled.Edit else Icons.Filled.StarBorder,
                    contentDescription = stringResource(
                        if (isFavoriteTab) R.string.favorite_rename_title else R.string.action_save_favorite
                    ),
                    tint = if (isFavoriteTab) MaterialTheme.colorScheme.onSurfaceVariant else WarningAmber
                )
            }

            IconButton(
                onClick = onSelect,
                modifier = Modifier.testTag("teleport_saved_${item.id}")
            ) {
                Icon(
                    imageVector = Icons.Filled.Navigation,
                    contentDescription = stringResource(R.string.action_teleport_here),
                    tint = MapBlue
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_saved_${item.id}")
            ) {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = "Hapus lokasi",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
