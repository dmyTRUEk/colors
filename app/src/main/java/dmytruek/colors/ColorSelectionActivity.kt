package dmytruek.colors

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

class ColorSelectionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDark = isSystemInDarkTheme()
            val colorScheme = if (isDark) {
                darkColorScheme(
                    background = Color.Black,
                    surface = Color.Black,
                    onBackground = Color.White,
                    onSurface = Color.White,
                    surfaceVariant = Color.Black,
                    onSurfaceVariant = Color.White
                )
            } else {
                lightColorScheme()
            }

            MaterialTheme(colorScheme = colorScheme) {
                ColorSelectionScreen()
            }
        }
    }
}

fun isLightColor(colorInt: Int): Boolean {
    val r = (colorInt shr 16) and 0xFF
    val g = (colorInt shr 8) and 0xFF
    val b = colorInt and 0xFF
    val luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
    return luminance > 0.6
}

@Composable
fun ColorItem(
    colorData: ColorData,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val isBgLight = remember(colorData.colorInt) { isLightColor(colorData.colorInt) }
    val textColor = if (isBgLight) Color.Black else Color.White
    val secondaryTextColor = if (isBgLight) Color.Black.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.7f)
    val cardBg = Color(colorData.colorInt)

    Surface(
        color = cardBg,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onToggle(!isEnabled) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = colorData.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor
                )

                val hexString = String.format("#%06X", 0xFFFFFF and colorData.colorInt)
                Text(
                    text = hexString,
                    style = MaterialTheme.typography.bodySmall,
                    color = secondaryTextColor
                )
            }

            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = textColor,
                    checkedTrackColor = textColor.copy(alpha = 0.35f),
                    uncheckedThumbColor = secondaryTextColor,
                    uncheckedTrackColor = secondaryTextColor.copy(alpha = 0.2f),
                    checkedBorderColor = textColor,
                    uncheckedBorderColor = secondaryTextColor
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorSelectionScreen() {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var disabledColors by remember { mutableStateOf<Set<String>>(SettingsManager.getDisabledColors(context)) }
    var warningMessage by remember { mutableStateOf<String?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }

    val filteredColors = remember(searchQuery) {
        val query = searchQuery.trim().lowercase()
        if (query.isEmpty()) {
            allColors
        } else {
            allColors.filter { colorData ->
                val hexClean = String.format("%06x", 0xFFFFFF and colorData.colorInt)
                colorData.name.lowercase().contains(query) ||
                        hexClean.contains(query) ||
                        "#$hexClean".contains(query)
            }
        }
    }

    val enabledCount: Int = allColors.size - disabledColors.size

    fun updateDisabled(newDisabled: Set<String>) {
        disabledColors = newDisabled
        warningMessage = null
        SettingsManager.setDisabledColors(context, newDisabled)
    }

    fun toggleColor(colorName: String, isEnabled: Boolean) {
        if (!isEnabled) {
            if (enabledCount <= 4) {
                warningMessage = "At least 4 colors must remain enabled for quiz choices!"
                return
            }
            updateDisabled(disabledColors + colorName)
        } else {
            updateDisabled(disabledColors - colorName)
        }
    }

    fun randomize() {
        val currentEnabledCount = maxOf(4, allColors.size - disabledColors.size)
        val newEnabledNames = allColors.shuffled().take(currentEnabledCount).map { it.name }.toSet()
        val newDisabledNames = allColors.map { it.name }.filter { it !in newEnabledNames }.toSet()
        updateDisabled(newDisabledNames)
    }

    fun enableAll() {
        updateDisabled(emptySet())
    }

    fun disableAll() {
        val keepColors = allColors.shuffled().take(4).map { it.name }.toSet()
        val newDisabled = allColors.map { it.name }.filter { it !in keepColors }.toSet()
        updateDisabled(newDisabled)
    }

    fun enableHalf() {
        val disabledList = allColors.filter { it.name in disabledColors }
        if (disabledList.isEmpty()) return
        val countToEnable = (disabledList.size + 1) / 2
        val newlyEnabled = disabledList.shuffled().take(countToEnable).map { it.name }.toSet()
        updateDisabled(disabledColors - newlyEnabled)
    }

    fun disableHalf() {
        val enabledList = allColors.filter { it.name !in disabledColors }
        val maxCanDisable = maxOf(0, enabledList.size - 4)
        if (maxCanDisable == 0) return
        val countToDisable = minOf(enabledList.size / 2, maxCanDisable)
        if (countToDisable > 0) {
            val newlyDisabled = enabledList.shuffled().take(countToDisable).map { it.name }.toSet()
            updateDisabled(disabledColors + newlyDisabled)
        }
    }

    fun enableTen() {
        val disabledList = allColors.filter { it.name in disabledColors }
        if (disabledList.isEmpty()) return
        val countToEnable = minOf(10, disabledList.size)
        val newlyEnabled = disabledList.shuffled().take(countToEnable).map { it.name }.toSet()
        updateDisabled(disabledColors - newlyEnabled)
    }

    fun disableTen() {
        val enabledList = allColors.filter { it.name !in disabledColors }
        val maxCanDisable = maxOf(0, enabledList.size - 4)
        if (maxCanDisable == 0) return
        val countToDisable = minOf(10, maxCanDisable)
        if (countToDisable > 0) {
            val newlyDisabled = enabledList.shuffled().take(countToDisable).map { it.name }.toSet()
            updateDisabled(disabledColors + newlyDisabled)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Manage Colors")
                        Text(
                            text = "$enabledCount / ${allColors.size} enabled",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { (context as? Activity)?.finish() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options"
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.width(200.dp)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Disable All") },
                                onClick = {
                                    menuExpanded = false
                                    disableAll()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Enable All") },
                                onClick = {
                                    menuExpanded = false
                                    enableAll()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Disable Half") },
                                onClick = {
                                    menuExpanded = false
                                    disableHalf()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Enable Half") },
                                onClick = {
                                    menuExpanded = false
                                    enableHalf()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Disable 10") },
                                onClick = {
                                    menuExpanded = false
                                    disableTen()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Enable 10") },
                                onClick = {
                                    menuExpanded = false
                                    enableTen()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Random") },
                                onClick = {
                                    menuExpanded = false
                                    randomize()
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors()
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search colors...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search"
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(
                        items = filteredColors,
                        key = { it.name }
                    ) { colorData ->
                        val isEnabled = colorData.name !in disabledColors
                        ColorItem(
                            colorData = colorData,
                            isEnabled = isEnabled,
                            onToggle = { enabled -> toggleColor(colorData.name, enabled) }
                        )
                    }
                }
            }

            if (warningMessage != null) {
                Surface(
                    color = Color(0xFFD32F2F),
                    contentColor = Color.White,
                    shape = CircleShape,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(16.dp)
                ) {
                    Text(
                        text = warningMessage!!,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                    )
                }
            }
        }
    }
}
