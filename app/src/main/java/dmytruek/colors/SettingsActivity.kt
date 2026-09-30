package dmytruek.colors

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

class SettingsActivity : ComponentActivity() {
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
                SettingsScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var minText by remember { mutableStateOf(SettingsManager.getMinPos(context).toString()) }
    var maxText by remember { mutableStateOf(SettingsManager.getMaxPos(context).toString()) }
    var candidatesText by remember { mutableStateOf(SettingsManager.getCandidatesCount(context).toString()) }
    var disabledCount by remember { mutableIntStateOf(SettingsManager.getDisabledColors(context).size) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                disabledCount = SettingsManager.getDisabledColors(context).size
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val minVal = minText.toIntOrNull()
    val maxVal = maxText.toIntOrNull()
    val candidatesVal = candidatesText.toIntOrNull()

    val maxCandidates = allColors.size

    val warningMessage: String? = when {
        minVal == null || maxVal == null || candidatesVal == null -> "Please enter valid numbers"
        minVal < 1 || maxVal < 1 -> "Position must be at least 1"
        minVal > maxVal -> "Min position cannot be greater than max position"
        candidatesVal < 3 -> "Candidates count must be at least 3"
        candidatesVal > maxCandidates -> "Candidates count cannot exceed $maxCandidates"
        else -> null
    }

    LaunchedEffect(minVal, maxVal, candidatesVal) {
        if (warningMessage == null && minVal != null && maxVal != null && candidatesVal != null) {
            SettingsManager.saveSettings(context, minVal, maxVal, candidatesVal)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { (context as? Activity)?.finish() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Wrong Answer Re-insertion",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "When an answer is incorrect, the question is placed back into the queue between min and max positions.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = minText,
                    onValueChange = { minText = it },
                    label = { Text("Min position") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = maxText,
                    onValueChange = { maxText = it },
                    label = { Text("Max position") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Question Difficulty",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    text = "Number of closest color candidates to randomly sample wrong choices from (minimum 3, maximum ${allColors.size}).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = candidatesText,
                    onValueChange = { candidatesText = it },
                    label = { Text("Candidates count") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Color Pool",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    text = "Select which colors appear in your quiz questions.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val enabledCount = allColors.size - disabledCount
                OutlinedButton(
                    onClick = {
                        context.startActivity(Intent(context, ColorSelectionActivity::class.java))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Manage Quiz Colors ($enabledCount / ${allColors.size} active)")
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
                        text = warningMessage,
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
