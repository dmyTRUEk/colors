package dmytruek.colors

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.ui.platform.LocalView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.math.abs
import kotlin.random.Random

data class ColorData(val colorInt: Int, val name: String)

data class QuestionState(
    val target: ColorData,
    val choices: List<ColorData>,
    val isQuestionColorValue: Boolean,
)

fun colorDistance(c1: Int, c2: Int): Long {
    val r1 = (c1 shr 16) and 0xFF
    val g1 = (c1 shr 8) and 0xFF
    val b1 = c1 and 0xFF

    val r2 = (c2 shr 16) and 0xFF
    val g2 = (c2 shr 8) and 0xFF
    val b2 = c2 and 0xFF

    val dr = abs(r1 - r2).toLong()
    val dg = abs(g1 - g2).toLong()
    val db = abs(b1 - b2).toLong()

    return dr + dg + db
}

fun generateQuestion(
    colorPool: List<ColorData>,
    candidatesLimit: Int = SettingsManager.DEFAULT_CANDIDATES_COUNT
): QuestionState {
    val target = colorPool.random()
    val sorted = colorPool.sortedBy { colorDistance(it.colorInt, target.colorInt) }

    // Uniformly sample 3 wrong choices from the top closest candidate colors in active pool
    val candidatesCount = minOf(candidatesLimit, sorted.size - 1)
    val topCandidates = sorted.subList(1, 1 + candidatesCount)
    val wrongChoices = topCandidates.shuffled().take(3)

    val choices = (wrongChoices + target).shuffled()
    val isQuestionColorValue = Random.nextBoolean()

    return QuestionState(
        target = target,
        choices = choices,
        isQuestionColorValue = isQuestionColorValue,
    )
}

fun generateQueue(colorPool: List<ColorData>, count: Int, candidatesLimit: Int): List<QuestionState> {
    return List(count) { generateQuestion(colorPool, candidatesLimit) }
}

fun performWrongAnswerHaptic(context: Context, view: View) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val vibrator = context.getSystemService(Vibrator::class.java)
        val effect = VibrationEffect.createWaveform(
            longArrayOf(0, 70, 50, 120),
            intArrayOf(0, 255, 0, 255),
            -1
        )
        vibrator?.vibrate(effect)
    } else {
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }
}

class MainActivity : ComponentActivity() {

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
                ColorLearningApp(colors = allColors)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ColorLearningApp(colors: List<ColorData>) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val view = LocalView.current

    var totalAnswered by remember { mutableIntStateOf(0) }
    var correctCount by remember { mutableIntStateOf(0) }
    var lastAnswerWasCorrect by remember { mutableStateOf<Boolean?>(null) }

    val questionQueue = remember {
        mutableStateListOf<QuestionState>().apply {
            val pool = SettingsManager.getActiveColors(context)
            addAll(
                generateQueue(
                    pool,
                    SettingsManager.getMaxPos(context),
                    SettingsManager.getCandidatesCount(context)
                )
            )
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val pool = SettingsManager.getActiveColors(context)
                val maxPos = SettingsManager.getMaxPos(context)
                val candidatesLimit = SettingsManager.getCandidatesCount(context)
                val poolSet = pool.toSet()

                // Purge/replace any existing question whose target or choices are no longer active
                for (i in questionQueue.indices) {
                    val q = questionQueue[i]
                    if (q.target !in poolSet || q.choices.any { it !in poolSet }) {
                        questionQueue[i] = generateQuestion(pool, candidatesLimit)
                    }
                }

                while (questionQueue.size < maxPos) {
                    questionQueue.add(generateQuestion(pool, candidatesLimit))
                }
                while (questionQueue.size > maxPos && questionQueue.size > 1) {
                    questionQueue.removeAt(questionQueue.size - 1)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val currentQuestion = questionQueue.first()

    fun onAnswerSelected(selected: ColorData) {
        totalAnswered++
        val pool = SettingsManager.getActiveColors(context)
        val isCorrect = (selected == currentQuestion.target)
        val minPos = SettingsManager.getMinPos(context)
        val maxPos = SettingsManager.getMaxPos(context)
        val candidatesLimit = SettingsManager.getCandidatesCount(context)

        if (isCorrect) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            correctCount++
            questionQueue.removeAt(0)
            questionQueue.add(generateQuestion(pool, candidatesLimit))
        } else {
            performWrongAnswerHaptic(context, view)
            val wrongQuestion = questionQueue.removeAt(0)
            val actualMinIndex = (minPos - 1).coerceIn(0, questionQueue.size)
            val actualMaxIndex = (maxPos - 1).coerceIn(actualMinIndex, questionQueue.size)
            val insertIndex = if (actualMinIndex == actualMaxIndex) {
                actualMinIndex
            } else {
                Random.nextInt(actualMinIndex, actualMaxIndex + 1)
            }
            questionQueue.add(insertIndex, wrongQuestion)
        }

        while (questionQueue.size < maxPos) {
            questionQueue.add(generateQuestion(pool, candidatesLimit))
        }
        while (questionQueue.size > maxPos && questionQueue.size > 1) {
            questionQueue.removeAt(questionQueue.size - 1)
        }

        lastAnswerWasCorrect = isCorrect
    }

    val isDark = isSystemInDarkTheme()
    val defaultTextBgColor = if (isDark) Color.Black else Color.White
    val defaultTextColor = if (isDark) Color.White else Color.Black

    val scoreBgColor = when (lastAnswerWasCorrect) {
        true -> Color(0xFF2E7D32)   // Green
        false -> Color(0xFFC62828)  // Red
        null -> Color.Black.copy(alpha = 0.5f)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) Color.Black else Color.White)
            .statusBarsPadding()
    ) {
        // Top Half: Question
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .then(
                    if (currentQuestion.isQuestionColorValue) {
                        Modifier.background(Color(currentQuestion.target.colorInt))
                    } else {
                        Modifier.background(defaultTextBgColor)
                    }
                )
                .combinedClickable(
                    onClick = {},
                    onLongClick = {
                        context.startActivity(Intent(context, SettingsActivity::class.java))
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (!currentQuestion.isQuestionColorValue) {
                Text(
                    text = currentQuestion.target.name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = defaultTextColor,
                    modifier = Modifier.padding(16.dp)
                )
            }

            // Score overlay at top
            Surface(
                color = scoreBgColor,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            ) {
                Text(
                    text = "$correctCount / $totalAnswered",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        }

        // Bottom Half: 2x2 Grid of 4 Answers
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val choices = currentQuestion.choices
            for (row in 0..1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    for (col in 0..1) {
                        val index = row * 2 + col
                        val choice = choices.getOrNull(index)
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(1f)
                                .then(
                                    if (currentQuestion.isQuestionColorValue) {
                                        Modifier.background(defaultTextBgColor)
                                    } else {
                                        if (choice != null) Modifier.background(Color(choice.colorInt)) else Modifier
                                    }
                                )
                                .clickable(enabled = choice != null) {
                                    if (choice != null) {
                                        onAnswerSelected(choice)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (choice != null && currentQuestion.isQuestionColorValue) {
                                Text(
                                    text = choice.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center,
                                    color = defaultTextColor,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

val allColors: List<ColorData> = listOf(
    "#5d8aa8" to "Air Force blue",
    "#f0f8ff" to "Alice blue",
    "#e32636" to "Alizarin crimson",
    "#efdecd" to "Almond",
    "#e52b50" to "Amaranth",
    "#ffbf00" to "Amber",
    "#ff033e" to "American rose",
    "#9966cc" to "Amethyst",
    "#a4c639" to "Android Green",
    "#f2f3f4" to "Anti-flash white",
    "#cd9575" to "Antique brass",
    "#915c83" to "Antique fuchsia",
    "#faebd7" to "Antique white",
    "#008000" to "Ao",
    "#8db600" to "Apple green",
    "#fbceb1" to "Apricot",
    "#00ffff" to "Aqua",
    "#7fffd4" to "Aquamarine",
    "#4b5320" to "Army green",
    "#e9d66b" to "Arylide yellow",
    "#b2beb5" to "Ash grey",
    "#87a96b" to "Asparagus",
    "#ff9966" to "Atomic tangerine",
    "#a52a2a" to "Auburn",
    "#fdee00" to "Aureolin",
    "#6e7f80" to "AuroMetalSaurus",
    "#ff2052" to "Awesome",
    "#007fff" to "Azure",
    "#f0ffff" to "Azure mist/web",
    "#89cff0" to "Baby blue",
    "#a1caf1" to "Baby blue eyes",
    "#f4c2c2" to "Baby pink",
    "#21abcd" to "Ball Blue",
    "#fae7b5" to "Banana Mania",
    "#ffe135" to "Banana yellow",
    "#848482" to "Battleship grey",
    "#98777b" to "Bazaar",
    "#bcd4e6" to "Beau blue",
    "#9f8170" to "Beaver",
    "#f5f5dc" to "Beige",
    "#ffe4c4" to "Bisque",
    "#3d2b1f" to "Bistre",
    "#fe6f5e" to "Bittersweet",
    "#000000" to "Black",
    "#ffebcd" to "Blanched Almond",
    "#318ce7" to "Bleu de France",
    "#ace5ee" to "Blizzard Blue",
    "#faf0be" to "Blond",
    "#0000ff" to "Blue",
    "#a2a2d0" to "Blue Bell",
    "#6699cc" to "Blue Gray",
    "#0d98ba" to "Blue green",
    "#8a2be2" to "Blue purple",
    "#8a2be2" to "Blue violet",
    "#de5d83" to "Blush",
    "#79443b" to "Bole",
    "#0095b6" to "Bondi blue",
    "#e3dac9" to "Bone",
    "#cc0000" to "Boston University Red",
    "#006a4e" to "Bottle green",
    "#873260" to "Boysenberry",
    "#0070ff" to "Brandeis blue",
    "#b5a642" to "Brass",
    "#cb4154" to "Brick red",
    "#1dacd6" to "Bright cerulean",
    "#66ff00" to "Bright green",
    "#bf94e4" to "Bright lavender",
    "#c32148" to "Bright maroon",
    "#ff007f" to "Bright pink",
    "#08e8de" to "Bright turquoise",
    "#d19fe8" to "Bright ube",
    "#f4bbff" to "Brilliant lavender",
    "#ff55a3" to "Brilliant rose",
    "#fb607f" to "Brink pink",
    "#004225" to "British racing green",
    "#cd7f32" to "Bronze",
    "#a52a2a" to "Brown",
    "#ffc1cc" to "Bubble gum",
    "#e7feff" to "Bubbles",
    "#f0dc82" to "Buff",
    "#480607" to "Bulgarian rose",
    "#800020" to "Burgundy",
    "#deb887" to "Burlywood",
    "#cc5500" to "Burnt orange",
    "#e97451" to "Burnt sienna",
    "#8a3324" to "Burnt umber",
    "#bd33a4" to "Byzantine",
    "#702963" to "Byzantium",
    "#007aa5" to "CG Blue",
    "#e03c31" to "CG Red",
    "#536872" to "Cadet",
    "#5f9ea0" to "Cadet blue",
    "#91a3b0" to "Cadet grey",
    "#006b3c" to "Cadmium green",
    "#ed872d" to "Cadmium orange",
    "#e30022" to "Cadmium red",
    "#fff600" to "Cadmium yellow",
    "#a67b5b" to "Café au lait",
    "#4b3621" to "Café noir",
    "#1e4d2b" to "Cal Poly Pomona green",
    "#a3c1ad" to "Cambridge Blue",
    "#c19a6b" to "Camel",
    "#78866b" to "Camouflage green",
    "#ffff99" to "Canary",
    "#ffef00" to "Canary yellow",
    "#ff0800" to "Candy apple red",
    "#e4717a" to "Candy pink",
    "#00bfff" to "Capri",
    "#592720" to "Caput mortuum",
    "#c41e3a" to "Cardinal",
    "#00cc99" to "Caribbean green",
    "#ff0040" to "Carmine",
    "#eb4c42" to "Carmine pink",
    "#ff0038" to "Carmine red",
    "#ffa6c9" to "Carnation pink",
    "#b31b1b" to "Carnelian",
    "#99badd" to "Carolina blue",
    "#ed9121" to "Carrot orange",
    "#ace1af" to "Celadon",
    "#b2ffff" to "Celeste",
    "#4997d0" to "Celestial blue",
    "#de3163" to "Cerise",
    "#ec3b83" to "Cerise pink",
    "#007ba7" to "Cerulean",
    "#2a52be" to "Cerulean blue",
    "#a0785a" to "Chamoisee",
    "#fad6a5" to "Champagne",
    "#36454f" to "Charcoal",
    "#7fff00" to "Chartreuse",
    "#de3163" to "Cherry",
    "#ffb7c5" to "Cherry blossom pink",
    "#cd5c5c" to "Chestnut",
    "#d2691e" to "Chocolate",
    "#ffa700" to "Chrome yellow",
    "#98817b" to "Cinereous",
    "#e34234" to "Cinnabar",
    "#d2691e" to "Cinnamon",
    "#e4d00a" to "Citrine",
    "#fbcce7" to "Classic rose",
    "#0047ab" to "Cobalt",
    "#d2691e" to "Cocoa brown",
    "#6f4e37" to "Coffee",
    "#9bddff" to "Columbia blue",
    "#002e63" to "Cool black",
    "#8c92ac" to "Cool grey",
    "#b87333" to "Copper",
    "#996666" to "Copper rose",
    "#ff3800" to "Coquelicot",
    "#ff7f50" to "Coral",
    "#f88379" to "Coral pink",
    "#ff4040" to "Coral red",
    "#893f45" to "Cordovan",
    "#fbec5d" to "Corn",
    "#b31b1b" to "Cornell Red",
    "#9aceeb" to "Cornflower",
    "#6495ed" to "Cornflower blue",
    "#fff8dc" to "Cornsilk",
    "#fff8e7" to "Cosmic latte",
    "#ffbcd9" to "Cotton candy",
    "#fffdd0" to "Cream",
    "#dc143c" to "Crimson",
    "#990000" to "Crimson Red",
    "#be0032" to "Crimson glory",
    "#00ffff" to "Cyan",
    "#ffff31" to "Daffodil",
    "#f0e130" to "Dandelion",
    "#00008b" to "Dark blue",
    "#654321" to "Dark brown",
    "#5d3954" to "Dark byzantium",
    "#a40000" to "Dark candy apple red",
    "#08457e" to "Dark cerulean",
    "#986960" to "Dark chestnut",
    "#cd5b45" to "Dark coral",
    "#008b8b" to "Dark cyan",
    "#536878" to "Dark electric blue",
    "#b8860b" to "Dark goldenrod",
    "#a9a9a9" to "Dark gray",
    "#013220" to "Dark green",
    "#1a2421" to "Dark jungle green",
    "#bdb76b" to "Dark khaki",
    "#483c32" to "Dark lava",
    "#734f96" to "Dark lavender",
    "#8b008b" to "Dark magenta",
    "#003366" to "Dark midnight blue",
    "#556b2f" to "Dark olive green",
    "#ff8c00" to "Dark orange",
    "#9932cc" to "Dark orchid",
    "#779ecb" to "Dark pastel blue",
    "#03c03c" to "Dark pastel green",
    "#966fd6" to "Dark pastel purple",
    "#c23b22" to "Dark pastel red",
    "#e75480" to "Dark pink",
    "#003399" to "Dark powder blue",
    "#872657" to "Dark raspberry",
    "#8b0000" to "Dark red",
    "#e9967a" to "Dark salmon",
    "#560319" to "Dark scarlet",
    "#8fbc8f" to "Dark sea green",
    "#3c1414" to "Dark sienna",
    "#483d8b" to "Dark slate blue",
    "#2f4f4f" to "Dark slate gray",
    "#177245" to "Dark spring green",
    "#918151" to "Dark tan",
    "#ffa812" to "Dark tangerine",
    "#483c32" to "Dark taupe",
    "#cc4e5c" to "Dark terra cotta",
    "#00ced1" to "Dark turquoise",
    "#9400d3" to "Dark violet",
    "#00693e" to "Dartmouth green",
    "#555555" to "Davy grey",
    "#d70a53" to "Debian red",
    "#a9203e" to "Deep carmine",
    "#ef3038" to "Deep carmine pink",
    "#e9692c" to "Deep carrot orange",
    "#da3287" to "Deep cerise",
    "#fad6a5" to "Deep champagne",
    "#b94e48" to "Deep chestnut",
    "#704241" to "Deep coffee",
    "#c154c1" to "Deep fuchsia",
    "#004b49" to "Deep jungle green",
    "#9955bb" to "Deep lilac",
    "#cc00cc" to "Deep magenta",
    "#ffcba4" to "Deep peach",
    "#ff1493" to "Deep pink",
    "#ff9933" to "Deep saffron",
    "#00bfff" to "Deep sky blue",
    "#1560bd" to "Denim",
    "#c19a6b" to "Desert",
    "#edc9af" to "Desert sand",
    "#696969" to "Dim gray",
    "#1e90ff" to "Dodger blue",
    "#d71868" to "Dogwood rose",
    "#85bb65" to "Dollar bill",
    "#967117" to "Drab",
    "#00009c" to "Duke blue",
    "#e1a95f" to "Earth yellow",
    "#c2b280" to "Ecru",
    "#614051" to "Eggplant",
    "#f0ead6" to "Eggshell",
    "#1034a6" to "Egyptian blue",
    "#7df9ff" to "Electric blue",
    "#ff003f" to "Electric crimson",
    "#00ffff" to "Electric cyan",
    "#00ff00" to "Electric green",
    "#6f00ff" to "Electric indigo",
    "#f4bbff" to "Electric lavender",
    "#ccff00" to "Electric lime",
    "#bf00ff" to "Electric purple",
    "#3f00ff" to "Electric ultramarine",
    "#8f00ff" to "Electric violet",
    "#ffff00" to "Electric yellow",
    "#50c878" to "Emerald",
    "#96c8a2" to "Eton blue",
    "#c19a6b" to "Fallow",
    "#801818" to "Falu red",
    "#ff00ff" to "Famous",
    "#b53389" to "Fandango",
    "#f400a1" to "Fashion fuchsia",
    "#e5aa70" to "Fawn",
    "#4d5d53" to "Feldgrau",
    "#71bc78" to "Fern",
    "#4f7942" to "Fern green",
    "#ff2800" to "Ferrari Red",
    "#6c541e" to "Field drab",
    "#ce2029" to "Fire engine red",
    "#b22222" to "Firebrick",
    "#e25822" to "Flame",
    "#fc8eac" to "Flamingo pink",
    "#f7e98e" to "Flavescent",
    "#eedc82" to "Flax",
    "#fffaf0" to "Floral white",
    "#ffbf00" to "Fluorescent orange",
    "#ff1493" to "Fluorescent pink",
    "#ccff00" to "Fluorescent yellow",
    "#ff004f" to "Folly",
    "#228b22" to "Forest green",
    "#a67b5b" to "French beige",
    "#0072bb" to "French blue",
    "#86608e" to "French lilac",
    "#f64a8a" to "French rose",
    "#ff00ff" to "Fuchsia",
    "#ff77ff" to "Fuchsia pink",
    "#e48400" to "Fulvous",
    "#cc6666" to "Fuzzy Wuzzy",
    "#dcdcdc" to "Gainsboro",
    "#e49b0f" to "Gamboge",
    "#f8f8ff" to "Ghost white",
    "#b06500" to "Ginger",
    "#6082b6" to "Glaucous",
    "#e6e8fa" to "Glitter",
    "#ffd700" to "Gold",
    "#996515" to "Golden brown",
    "#fcc200" to "Golden poppy",
    "#ffdf00" to "Golden yellow",
    "#daa520" to "Goldenrod",
    "#a8e4a0" to "Granny Smith Apple",
    "#808080" to "Gray",
    "#465945" to "Gray asparagus",
    "#00ff00" to "Green",
    "#1164b4" to "Green Blue",
    "#adff2f" to "Green yellow",
    "#a99a86" to "Grullo",
    "#00ff7f" to "Guppie green",
    "#663854" to "Halayà úbe",
    "#446ccf" to "Han blue",
    "#5218fa" to "Han purple",
    "#e9d66b" to "Hansa yellow",
    "#3fff00" to "Harlequin",
    "#c90016" to "Harvard crimson",
    "#da9100" to "Harvest Gold",
    "#808000" to "Heart Gold",
    "#df73ff" to "Heliotrope",
    "#f400a1" to "Hollywood cerise",
    "#f0fff0" to "Honeydew",
    "#49796b" to "Hooker green",
    "#ff1dce" to "Hot magenta",
    "#ff69b4" to "Hot pink",
    "#355e3b" to "Hunter green",
    "#fcf75e" to "Icterine",
    "#b2ec5d" to "Inchworm",
    "#138808" to "India green",
    "#cd5c5c" to "Indian red",
    "#e3a857" to "Indian yellow",
    "#4b0082" to "Indigo",
    "#002fa7" to "International Klein Blue",
    "#ff4f00" to "International orange",
    "#5a4fcf" to "Iris",
    "#f4f0ec" to "Isabelline",
    "#009000" to "Islamic green",
    "#fffff0" to "Ivory",
    "#00a86b" to "Jade",
    "#f8de7e" to "Jasmine",
    "#d73b3e" to "Jasper",
    "#a50b5e" to "Jazzberry jam",
    "#fada5e" to "Jonquil",
    "#bdda57" to "June bud",
    "#29ab87" to "Jungle green",
    "#e8000d" to "KU Crimson",
    "#4cbb17" to "Kelly green",
    "#c3b091" to "Khaki",
    "#087830" to "La Salle Green",
    "#d6cadd" to "Languid lavender",
    "#26619c" to "Lapis lazuli",
    "#fefe22" to "Laser Lemon",
    "#a9ba9d" to "Laurel green",
    "#cf1020" to "Lava",
    "#e6e6fa" to "Lavender",
    "#ccccff" to "Lavender blue",
    "#fff0f5" to "Lavender blush",
    "#c4c3d0" to "Lavender gray",
    "#9457eb" to "Lavender indigo",
    "#ee82ee" to "Lavender magenta",
    "#e6e6fa" to "Lavender mist",
    "#fbaed2" to "Lavender pink",
    "#967bb6" to "Lavender purple",
    "#fba0e3" to "Lavender rose",
    "#7cfc00" to "Lawn green",
    "#fff700" to "Lemon",
    "#fff44f" to "Lemon Yellow",
    "#fffacd" to "Lemon chiffon",
    "#bfff00" to "Lemon lime",
    "#f56991" to "Light Crimson",
    "#e68fac" to "Light Thulian pink",
    "#fdd5b1" to "Light apricot",
    "#add8e6" to "Light blue",
    "#b5651d" to "Light brown",
    "#e66771" to "Light carmine pink",
    "#f08080" to "Light coral",
    "#93ccea" to "Light cornflower blue",
    "#e0ffff" to "Light cyan",
    "#f984ef" to "Light fuchsia pink",
    "#fafad2" to "Light goldenrod yellow",
    "#d3d3d3" to "Light gray",
    "#90ee90" to "Light green",
    "#f0e68c" to "Light khaki",
    "#b19cd9" to "Light pastel purple",
    "#ffb6c1" to "Light pink",
    "#ffa07a" to "Light salmon",
    "#ff9999" to "Light salmon pink",
    "#20b2aa" to "Light sea green",
    "#87cefa" to "Light sky blue",
    "#778899" to "Light slate gray",
    "#b38b6d" to "Light taupe",
    "#ffffed" to "Light yellow",
    "#c8a2c8" to "Lilac",
    "#bfff00" to "Lime",
    "#32cd32" to "Lime green",
    "#195905" to "Lincoln green",
    "#faf0e6" to "Linen",
    "#c19a6b" to "Lion",
    "#534b4f" to "Liver",
    "#e62020" to "Lust",
    "#18453b" to "MSU Green",
    "#ffbd88" to "Macaroni and Cheese",
    "#ff00ff" to "Magenta",
    "#aaf0d1" to "Magic mint",
    "#f8f4ff" to "Magnolia",
    "#c04000" to "Mahogany",
    "#fbec5d" to "Maize",
    "#6050dc" to "Majorelle Blue",
    "#0bda51" to "Malachite",
    "#979aaa" to "Manatee",
    "#ff8243" to "Mango Tango",
    "#74c365" to "Mantis",
    "#800000" to "Maroon",
    "#e0b0ff" to "Mauve",
    "#915f6d" to "Mauve taupe",
    "#ef98aa" to "Mauvelous",
    "#73c2fb" to "Maya blue",
    "#e5b73b" to "Meat brown",
    "#0067a5" to "Medium Persian blue",
    "#66ddaa" to "Medium aquamarine",
    "#0000cd" to "Medium blue",
    "#e2062c" to "Medium candy apple red",
    "#af4035" to "Medium carmine",
    "#f3e5ab" to "Medium champagne",
    "#035096" to "Medium electric blue",
    "#1c352d" to "Medium jungle green",
    "#dda0dd" to "Medium lavender magenta",
    "#ba55d3" to "Medium orchid",
    "#9370db" to "Medium purple",
    "#bb3385" to "Medium red violet",
    "#3cb371" to "Medium sea green",
    "#7b68ee" to "Medium slate blue",
    "#c9dc87" to "Medium spring bud",
    "#00fa9a" to "Medium spring green",
    "#674c47" to "Medium taupe",
    "#0054b4" to "Medium teal blue",
    "#48d1cc" to "Medium turquoise",
    "#c71585" to "Medium violet red",
    "#fdbcb4" to "Melon",
    "#191970" to "Midnight blue",
    "#004953" to "Midnight green",
    "#ffc40c" to "Mikado yellow",
    "#3eb489" to "Mint",
    "#f5fffa" to "Mint cream",
    "#98ff98" to "Mint green",
    "#ffe4e1" to "Misty rose",
    "#faebd7" to "Moccasin",
    "#967117" to "Mode beige",
    "#73a9c2" to "Moonstone blue",
    "#ae0c00" to "Mordant red 19",
    "#addfad" to "Moss green",
    "#30ba8f" to "Mountain Meadow",
    "#997a8d" to "Mountbatten pink",
    "#c54b8c" to "Mulberry",
    "#f2f3f4" to "Munsell",
    "#ffdb58" to "Mustard",
    "#21421e" to "Myrtle",
    "#f6adc6" to "Nadeshiko pink",
    "#2a8000" to "Napier green",
    "#fada5e" to "Naples yellow",
    "#ffdead" to "Navajo white",
    "#000080" to "Navy blue",
    "#ffa343" to "Neon Carrot",
    "#fe59c2" to "Neon fuchsia",
    "#39ff14" to "Neon green",
    "#a4dded" to "Non-photo blue",
    "#059033" to "North Texas Green",
    "#0077be" to "Ocean Boat Blue",
    "#cc7722" to "Ochre",
    "#008000" to "Office green",
    "#cfb53b" to "Old gold",
    "#fdf5e6" to "Old lace",
    "#796878" to "Old lavender",
    "#673147" to "Old mauve",
    "#c08081" to "Old rose",
    "#808000" to "Olive",
    "#6b8e23" to "Olive Drab",
    "#bab86c" to "Olive Green",
    "#9ab973" to "Olivine",
    "#0f0f0f" to "Onyx",
    "#b784a7" to "Opera mauve",
    "#ffa500" to "Orange",
    "#f8d568" to "Orange Yellow",
    "#ff9f00" to "Orange peel",
    "#ff4500" to "Orange red",
    "#da70d6" to "Orchid",
    "#654321" to "Otter brown",
    "#414a4c" to "Outer Space",
    "#ff6e4a" to "Outrageous Orange",
    "#002147" to "Oxford Blue",
    "#1ca9c9" to "Pacific Blue",
    "#006600" to "Pakistan green",
    "#273be2" to "Palatinate blue",
    "#682860" to "Palatinate purple",
    "#bcd4e6" to "Pale aqua",
    "#afeeee" to "Pale blue",
    "#987654" to "Pale brown",
    "#af4035" to "Pale carmine",
    "#9bc4e2" to "Pale cerulean",
    "#ddadaf" to "Pale chestnut",
    "#da8a67" to "Pale copper",
    "#abcdef" to "Pale cornflower blue",
    "#e6be8a" to "Pale gold",
    "#eee8aa" to "Pale goldenrod",
    "#98fb98" to "Pale green",
    "#dcd0ff" to "Pale lavender",
    "#f984e5" to "Pale magenta",
    "#fadadd" to "Pale pink",
    "#dda0dd" to "Pale plum",
    "#db7093" to "Pale red violet",
    "#96ded1" to "Pale robin egg blue",
    "#c9c0bb" to "Pale silver",
    "#ecebbd" to "Pale spring bud",
    "#bc987e" to "Pale taupe",
    "#db7093" to "Pale violet red",
    "#78184a" to "Pansy purple",
    "#ffefd5" to "Papaya whip",
    "#50c878" to "Paris Green",
    "#aec6cf" to "Pastel blue",
    "#836953" to "Pastel brown",
    "#cfcfc4" to "Pastel gray",
    "#77dd77" to "Pastel green",
    "#f49ac2" to "Pastel magenta",
    "#ffb347" to "Pastel orange",
    "#ffd1dc" to "Pastel pink",
    "#b39eb5" to "Pastel purple",
    "#ff6961" to "Pastel red",
    "#cb99c9" to "Pastel violet",
    "#fdfd96" to "Pastel yellow",
    "#800080" to "Patriarch",
    "#536878" to "Payne grey",
    "#ffe5b4" to "Peach",
    "#ffdab9" to "Peach puff",
    "#fadfad" to "Peach yellow",
    "#d1e231" to "Pear",
    "#eae0c8" to "Pearl",
    "#88d8c0" to "Pearl Aqua",
    "#e6e200" to "Peridot",
    "#ccccff" to "Periwinkle",
    "#1c39bb" to "Persian blue",
    "#32127a" to "Persian indigo",
    "#d99058" to "Persian orange",
    "#f77fbe" to "Persian pink",
    "#701c1c" to "Persian plum",
    "#cc3333" to "Persian red",
    "#fe28a2" to "Persian rose",
    "#df00ff" to "Phlox",
    "#000f89" to "Phthalo blue",
    "#123524" to "Phthalo green",
    "#fddde6" to "Piggy pink",
    "#01796f" to "Pine green",
    "#ffc0cb" to "Pink",
    "#fc74fd" to "Pink Flamingo",
    "#f78fa7" to "Pink Sherbet",
    "#e7accf" to "Pink pearl",
    "#93c572" to "Pistachio",
    "#e5e4e2" to "Platinum",
    "#dda0dd" to "Plum",
    "#ff5a36" to "Portland Orange",
    "#b0e0e6" to "Powder blue",
    "#ff8f00" to "Princeton orange",
    "#003153" to "Prussian blue",
    "#df00ff" to "Psychedelic purple",
    "#cc8899" to "Puce",
    "#ff7518" to "Pumpkin",
    "#800080" to "Purple",
    "#69359c" to "Purple Heart",
    "#9d81ba" to "Purple Mountain's Majesty",
    "#9678b6" to "Purple mountain majesty",
    "#fe4eda" to "Purple pizzazz",
    "#50404d" to "Purple taupe",
    "#5d8aa8" to "Rackley",
    "#ff355e" to "Radical Red",
    "#e30b5d" to "Raspberry",
    "#915f6d" to "Raspberry glace",
    "#e25098" to "Raspberry pink",
    "#b3446c" to "Raspberry rose",
    "#d68a59" to "Raw Sienna",
    "#ff33cc" to "Razzle dazzle rose",
    "#e3256b" to "Razzmatazz",
    "#ff0000" to "Red",
    "#ff5349" to "Red Orange",
    "#a52a2a" to "Red brown",
    "#c71585" to "Red violet",
    "#004040" to "Rich black",
    "#d70040" to "Rich carmine",
    "#0892d0" to "Rich electric blue",
    "#b666d2" to "Rich lilac",
    "#b03060" to "Rich maroon",
    "#414833" to "Rifle green",
    "#1fcecb" to "Robin's Egg Blue",
    "#ff007f" to "Rose",
    "#f9429e" to "Rose bonbon",
    "#674846" to "Rose ebony",
    "#b76e79" to "Rose gold",
    "#e32636" to "Rose madder",
    "#ff66cc" to "Rose pink",
    "#aa98a9" to "Rose quartz",
    "#905d5d" to "Rose taupe",
    "#ab4e52" to "Rose vale",
    "#65000b" to "Rosewood",
    "#d40000" to "Rosso corsa",
    "#bc8f8f" to "Rosy brown",
    "#0038a8" to "Royal azure",
    "#4169e1" to "Royal blue",
    "#ca2c92" to "Royal fuchsia",
    "#7851a9" to "Royal purple",
    "#e0115f" to "Ruby",
    "#ff0028" to "Ruddy",
    "#bb6528" to "Ruddy brown",
    "#e18e96" to "Ruddy pink",
    "#a81c07" to "Rufous",
    "#80461b" to "Russet",
    "#b7410e" to "Rust",
    "#00563f" to "Sacramento State green",
    "#8b4513" to "Saddle brown",
    "#ff6700" to "Safety orange",
    "#f4c430" to "Saffron",
    "#23297a" to "Saint Patrick Blue",
    "#ff8c69" to "Salmon",
    "#ff91a4" to "Salmon pink",
    "#c2b280" to "Sand",
    "#967117" to "Sand dune",
    "#ecd540" to "Sandstorm",
    "#f4a460" to "Sandy brown",
    "#967117" to "Sandy taupe",
    "#507d2a" to "Sap green",
    "#0f52ba" to "Sapphire",
    "#cba135" to "Satin sheen gold",
    "#ff2400" to "Scarlet",
    "#ffd800" to "School bus yellow",
    "#76ff7a" to "Screamin Green",
    "#006994" to "Sea blue",
    "#2e8b57" to "Sea green",
    "#321414" to "Seal brown",
    "#fff5ee" to "Seashell",
    "#ffba00" to "Selective yellow",
    "#704214" to "Sepia",
    "#8a795d" to "Shadow",
    "#45cea2" to "Shamrock",
    "#009e60" to "Shamrock green",
    "#fc0fc0" to "Shocking pink",
    "#882d17" to "Sienna",
    "#c0c0c0" to "Silver",
    "#cb410b" to "Sinopia",
    "#007474" to "Skobeloff",
    "#87ceeb" to "Sky blue",
    "#cf71af" to "Sky magenta",
    "#6a5acd" to "Slate blue",
    "#708090" to "Slate gray",
    "#003399" to "Smalt",
    "#933d41" to "Smokey topaz",
    "#100c08" to "Smoky black",
    "#fffafa" to "Snow",
    "#0fc0fc" to "Spiro Disco Ball",
    "#a7fc00" to "Spring bud",
    "#00ff7f" to "Spring green",
    "#4682b4" to "Steel blue",
    "#fada5e" to "Stil de grain yellow",
    "#990000" to "Stizza",
    "#008080" to "Stormcloud",
    "#e4d96f" to "Straw",
    "#ffcc33" to "Sunglow",
    "#fad6a5" to "Sunset",
    "#fd5e53" to "Sunset Orange",
    "#d2b48c" to "Tan",
    "#f94d00" to "Tangelo",
    "#f28500" to "Tangerine",
    "#ffcc00" to "Tangerine yellow",
    "#483c32" to "Taupe",
    "#8b8589" to "Taupe gray",
    "#cd5700" to "Tawny",
    "#d0f0c0" to "Tea green",
    "#f4c2c2" to "Tea rose",
    "#008080" to "Teal",
    "#367588" to "Teal blue",
    "#006d5b" to "Teal green",
    "#e2725b" to "Terra cotta",
    "#d8bfd8" to "Thistle",
    "#de6fa1" to "Thulian pink",
    "#fc89ac" to "Tickle Me Pink",
    "#0abab5" to "Tiffany Blue",
    "#e08d3c" to "Tiger eye",
    "#dbd7d2" to "Timberwolf",
    "#eee600" to "Titanium yellow",
    "#ff6347" to "Tomato",
    "#746cc0" to "Toolbox",
    "#ffc87c" to "Topaz",
    "#fd0e35" to "Tractor red",
    "#808080" to "Trolley Grey",
    "#00755e" to "Tropical rain forest",
    "#0073cf" to "True Blue",
    "#417dc1" to "Tufts Blue",
    "#deaa88" to "Tumbleweed",
    "#b57281" to "Turkish rose",
    "#30d5c8" to "Turquoise",
    "#00ffef" to "Turquoise blue",
    "#a0d6b4" to "Turquoise green",
    "#66424d" to "Tuscan red",
    "#8a496b" to "Twilight lavender",
    "#66023c" to "Tyrian purple",
    "#0033aa" to "UA blue",
    "#d9004c" to "UA red",
    "#536895" to "UCLA Blue",
    "#ffb300" to "UCLA Gold",
    "#3cd070" to "UFO Green",
    "#014421" to "UP Forest green",
    "#7b1113" to "UP Maroon",
    "#990000" to "USC Cardinal",
    "#ffcc00" to "USC Gold",
    "#8878c3" to "Ube",
    "#ff6fff" to "Ultra pink",
    "#120a8f" to "Ultramarine",
    "#4166f5" to "Ultramarine blue",
    "#635147" to "Umber",
    "#5b92e5" to "United Nations blue",
    "#b78727" to "University of California Gold",
    "#ffff66" to "Unmellow Yellow",
    "#ae2029" to "Upsdell red",
    "#e1ad21" to "Urobilin",
    "#d3003f" to "Utah Crimson",
    "#f3e5ab" to "Vanilla",
    "#c5b358" to "Vegas gold",
    "#c80815" to "Venetian red",
    "#43b3ae" to "Verdigris",
    "#e34234" to "Vermilion",
    "#a020f0" to "Veronica",
    "#ee82ee" to "Violet",
    "#324ab2" to "Violet Blue",
    "#f75394" to "Violet Red",
    "#40826d" to "Viridian",
    "#922724" to "Vivid auburn",
    "#9f1d35" to "Vivid burgundy",
    "#da1d81" to "Vivid cerise",
    "#ffa089" to "Vivid tangerine",
    "#9f00ff" to "Vivid violet",
    "#004242" to "Warm black",
    "#00ffff" to "Waterspout",
    "#645452" to "Wenge",
    "#f5deb3" to "Wheat",
    "#ffffff" to "White",
    "#f5f5f5" to "White smoke",
    "#ff43a4" to "Wild Strawberry",
    "#fc6c85" to "Wild Watermelon",
    "#a2add0" to "Wild blue yonder",
    "#722f37" to "Wine",
    "#c9a0dc" to "Wisteria",
    "#738678" to "Xanadu",
    "#0f4d92" to "Yale Blue",
    "#ffff00" to "Yellow",
    "#ffae42" to "Yellow Orange",
    "#9acd32" to "Yellow green",
    "#0014a8" to "Zaffre",
    "#2c1608" to "Zinnwaldite brown",
).map { (hex, name) -> ColorData(hex.toColorInt(), name) }

@Preview(showBackground = true)
@Composable
fun AppLogoPreview() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(16.dp)
    ) {
        // Light Theme
        MaterialTheme(colorScheme = lightColorScheme()) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = "App Logo Light",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(108.dp)
                )
            }
        }
        // Dark Theme
        MaterialTheme(colorScheme = darkColorScheme()) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = "App Logo Dark",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(108.dp)
                )
            }
        }
    }
}


