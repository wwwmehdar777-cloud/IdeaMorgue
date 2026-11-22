package com.ideamorgue.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import org.json.JSONObject
import okhttp3.*
import java.io.IOException

private val Ink = Color(0xFF0B1728)
private val Panel = Color(0xFF172A42)
private val Panel2 = Color(0xFF203751)
private val Mint = Color(0xFF39D2B7)
private val Gold = Color(0xFFF2BD61)
private val Paper = Color(0xFFF2F5F8)
private val Muted = Color(0xFFA9B9CC)

data class ClinicalCase(
    val id: Int,
    val specialty: String,
    val title: String,
    val story: String,
    val question: String,
    val options: List<String>,
    val correct: Int,
    val explanation: String,
    val takeHome: String,
    val source: String
)

private val cases = listOf(
    ClinicalCase(
        1, "الطوارئ", "ضيق نفس مفاجئ",
        "رجل يبلغ 58 عاماً حضر إلى الطوارئ بسبب ضيق نفس مفاجئ بدأ قبل ساعة. يذكر ألماً صدرياً يزداد مع الشهيق، مع تسرع في القلب. ما زال التقييم الأولي جارياً.",
        "ما التشخيص الخطير الذي يجب وضعه ضمن الاحتمالات مبكراً؟",
        listOf("الانصمام الرئوي", "نوبة ربو بسيطة", "التهاب جيوب أنفية", "ارتجاع معدي مريئي"),
        0,
        "الانصمام الرئوي احتمال مهم عند وجود ضيق نفس مفاجئ وألم جنبي وتسرع قلب. لا تكفي هذه المعطيات وحدها لتأكيد التشخيص؛ يجب تقدير الاحتمال السريري واختيار الفحوصات وفق حالة المريض.",
        "ابدأ بتقدير الاستقرار hemodynamic stability والاحتمال السريري قبل اختيار الفحص.",
        "مرجع تعليمي مقترح: ESC Guidelines for the diagnosis and management of acute pulmonary embolism (2019). تحقّق من التحديثات المحلية قبل التطبيق."
    ),
    ClinicalCase(
        2, "العناية المركزة", "ضغط لا يتحسن كما توقعت",
        "مريض في العناية المركزة لديه صدمة دورانية. بعد بدء رافع ضغط، ارتفع متوسط الضغط الشرياني، لكن علامات التروية الطرفية ما زالت غير مطمئنة.",
        "ما الخطوة الفكرية الأهم قبل زيادة جرعة رافع الضغط تلقائياً؟",
        listOf("إعادة تقييم سبب الصدمة والتروية والاستجابة", "مضاعفة الجرعة دون إعادة تقييم", "إيقاف كل السوائل دائماً", "الاعتماد على رقم الضغط وحده"),
        0,
        "تحقيق هدف ضغط الدم لا يساوي بالضرورة تحسن التروية. ينبغي إعادة تقييم السبب، ومؤشرات التروية، وحالة الحجم، ووظيفة القلب، والاستجابة للتدخلات.",
        "لا تختزل الإنعاش في رقم MAP واحد؛ اربط الضغط بالتروية والسياق السريري.",
        "مرجع تعليمي مقترح: Surviving Sepsis Campaign Guidelines (2021). راجع التحديثات الأحدث والبروتوكول المحلي."
    )
)

enum class Screen { HOME, CASE, LIBRARY, ASSISTANT, PROFILE }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { IdeaMorgueApp() }
    }
}

@Composable
fun IdeaMorgueApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("idea_morgue", 0) }
    var screen by remember { mutableStateOf(Screen.HOME) }
    var selected by remember { mutableStateOf(cases.first()) }
    var answer by remember { mutableIntStateOf(-1) }
    var saved by remember { mutableStateOf(prefs.getStringSet("saved", emptySet())?.toSet() ?: emptySet()) }
    var completed by remember { mutableStateOf(prefs.getStringSet("completed", emptySet())?.toSet() ?: emptySet()) }
    var specialty by remember { mutableStateOf(prefs.getString("specialty", "الطوارئ") ?: "الطوارئ") }

    MaterialTheme(colorScheme = darkColorScheme(
        primary = Mint, secondary = Gold, background = Ink, surface = Panel,
        onPrimary = Ink, onBackground = Paper, onSurface = Paper
    )) {
        Scaffold(
            containerColor = Ink,
            bottomBar = { BottomBar(screen) { screen = it } }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when (screen) {
                    Screen.HOME -> HomeScreen(
                        completed.size, specialty,
                        onOpen = { selected = it; answer = -1; screen = Screen.CASE },
                        onLibrary = { screen = Screen.LIBRARY }
                    )
                    Screen.CASE -> CaseScreen(
                        selected, answer, { answer = it },
                        saved.contains(selected.id.toString()),
                        onSave = {
                            val next = saved.toMutableSet()
                            if (!next.add(selected.id.toString())) next.remove(selected.id.toString())
                            saved = next
                            prefs.edit().putStringSet("saved", next).apply()
                        },
                        onComplete = {
                            val next = completed.toMutableSet().apply { add(selected.id.toString()) }
                            completed = next
                            prefs.edit().putStringSet("completed", next).apply()
                        }
                    )
                    Screen.LIBRARY -> LibraryScreen(saved, onOpen = {
                        selected = it; answer = -1; screen = Screen.CASE
                    })
                    Screen.ASSISTANT -> AssistantScreen()
                    Screen.PROFILE -> ProfileScreen(specialty) {
                        specialty = it
                        prefs.edit().putString("specialty", it).apply()
                    }
                }
            }
        }
    }
}

@Composable
fun BottomBar(current: Screen, onSelect: (Screen) -> Unit) {
    NavigationBar(containerColor = Color(0xFF101F33), contentColor = Paper) {
        val items = listOf(
            Triple(Screen.HOME, "الرئيسية", Icons.Rounded.Home),
            Triple(Screen.LIBRARY, "مكتبتي", Icons.Rounded.Bookmarks),
            Triple(Screen.ASSISTANT, "المساعد", Icons.Rounded.AutoAwesome),
            Triple(Screen.PROFILE, "ملفي", Icons.Rounded.Person)
        )
        items.forEach { (s, label, icon) ->
            NavigationBarItem(
                selected = current == s, onClick = { onSelect(s) },
                icon = { Icon(icon, null) }, label = { Text(label, fontSize = 10.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Mint, selectedTextColor = Mint,
                    indicatorColor = Panel2, unselectedIconColor = Muted,
                    unselectedTextColor = Muted
                )
            )
        }
    }
}

@Composable
fun HomeScreen(done: Int, specialty: String, onOpen: (ClinicalCase) -> Unit, onLibrary: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("THE IDEA MORGUE", color = Mint, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Text("مشرحة الأفكار", color = Paper, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                    Text("المعرفة تبدأ بسؤال.", color = Muted, fontSize = 13.sp)
                }
                Surface(color = Panel, shape = RoundedCornerShape(16.dp)) {
                    Icon(Icons.Rounded.Psychology, null, tint = Mint, modifier = Modifier.padding(14.dp).size(30.dp))
                }
            }
        }
        item {
            Surface(color = Panel, shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("مساحة تعلّم بلا ضغط", color = Gold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("لا تحتاج إلى ساعة فارغة.\nابدأ بلغز سريري واحد.", color = Paper, fontSize = 22.sp, fontWeight = FontWeight.Bold, lineHeight = 31.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatChip("حالات مكتملة", done.toString())
                        StatChip("اهتمامك", specialty)
                    }
                }
            }
        }
        item { SectionTitle("حالة مقترحة لك", "5–10 دقائق") }
        items(cases.filter { it.specialty == specialty }.ifEmpty { cases }.take(1)) { c ->
            CaseCard(c, onClick = { onOpen(c) })
        }
        item { SectionTitle("مختبر الحالات", "اختر لغزاً") }
        items(cases) { c -> CaseCard(c, onClick = { onOpen(c) }) }
        item {
            OutlinedButton(onClick = onLibrary, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = Mint)) {
                Icon(Icons.Rounded.Bookmarks, null); Spacer(Modifier.width(8.dp)); Text("افتح مكتبة أفكاري")
            }
        }
        item { Text("ملاحظة تعليمية: الحالات تجريبية وليست بديلاً عن التقييم السريري أو الإرشادات المحلية.", color = Muted, fontSize = 10.sp) }
    }
}

@Composable
fun StatChip(label: String, value: String) {
    Surface(color = Panel2, shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(label, color = Muted, fontSize = 10.sp)
            Text(value, color = Mint, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SectionTitle(title: String, sub: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = Paper, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(sub, color = Mint, fontSize = 11.sp)
    }
}

@Composable
fun CaseCard(c: ClinicalCase, onClick: () -> Unit) {
    Surface(color = Panel, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = Panel2, shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Rounded.MedicalInformation, null, tint = Mint, modifier = Modifier.padding(13.dp).size(25.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(c.specialty, color = Gold, fontSize = 11.sp)
                Text(c.title, color = Paper, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("لغز سريري • قراءة قصيرة", color = Muted, fontSize = 11.sp)
            }
            Icon(Icons.Rounded.ChevronLeft, null, tint = Mint)
        }
    }
}


@Composable
fun CaseScreen(c: ClinicalCase, answer: Int, onAnswer: (Int) -> Unit, isSaved: Boolean, onSave: () -> Unit, onComplete: () -> Unit) {
    var clueIndex by remember(c.id) { mutableIntStateOf(0) }
    var revealed by remember(c.id) { mutableStateOf(false) }
    val clues = when (c.id) {
        1 -> listOf(
            "المعطيات الأولية: ضيق نفس مفاجئ وألم صدري يزداد مع الشهيق.",
            "أثناء التقييم: تسرّع القلب. لا توجد حتى الآن معلومات كافية لتأكيد سبب واحد.",
            "مفصل التفكير: ابحث عن عوامل الخطورة، علامات عدم الاستقرار، والاحتمال السريري قبل طلب الفحوصات."
        )
        2 -> listOf(
            "المعطيات الأولية: صدمة دورانية وارتفاع متوسط الضغط بعد بدء رافع الضغط.",
            "الملاحظة الجديدة: مؤشرات التروية الطرفية ما زالت غير مطمئنة.",
            "مفصل التفكير: افصل بين تحقيق هدف الضغط وبين تحسن التروية، ثم أعد تقييم سبب الصدمة ووظيفة القلب والحجم."
        )
        else -> listOf(
            "ابدأ بتحديد المشكلة السريرية الرئيسية.",
            "ابحث عن المعطيات التي تغيّر الاحتمالات التشخيصية.",
            "اربط كل فرضية بفحص أو معلومة يمكن أن تؤيدها أو تضعفها."
        )
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("مسرح التحقيق  /  ${c.specialty}", color = Mint, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(c.title, color = Paper, fontSize = 27.sp, fontWeight = FontWeight.Bold)
            Text("لا تكشف كل شيء دفعة واحدة. اجمع الأدلة ثم كوّن فرضيتك.", color = Muted, fontSize = 13.sp)
        }
        item {
            Surface(color = Panel, shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Text("ملف الحالة", color = Gold, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(c.story, color = Paper, fontSize = 16.sp, lineHeight = 27.sp)
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("الأدلة السريرية", color = Paper, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("${clueIndex + 1} / ${clues.size}", color = Mint, fontSize = 12.sp)
            }
        }
        items(clueIndex + 1) { i ->
            Surface(color = Panel2, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                    Surface(color = Mint, shape = RoundedCornerShape(50)) {
                        Text("${i + 1}", color = Ink, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(clues[i], color = Paper, lineHeight = 23.sp, fontSize = 14.sp)
                }
            }
        }
        if (clueIndex < clues.lastIndex) {
            item {
                OutlinedButton(
                    onClick = { clueIndex++ },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Mint)
                ) {
                    Icon(Icons.Rounded.Search, null)
                    Spacer(Modifier.width(8.dp))
                    Text("اكشف الدليل التالي")
                }
            }
        } else {
            item {
                Surface(color = Panel, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("لحظة اتخاذ القرار", color = Gold, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text(c.question, color = Paper, fontSize = 17.sp, fontWeight = FontWeight.Bold, lineHeight = 25.sp)
                    }
                }
            }
            items(c.options.size) { i ->
                val chosen = answer == i
                Surface(
                    color = if (chosen) Panel2 else Panel,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().clickable { if (!revealed) onAnswer(i) }
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = chosen, onClick = { if (!revealed) onAnswer(i) }, colors = RadioButtonDefaults.colors(selectedColor = Mint))
                        Spacer(Modifier.width(8.dp))
                        Text(c.options[i], color = Paper, fontSize = 14.sp)
                    }
                }
            }
            item {
                Button(
                    onClick = { revealed = true; onComplete() },
                    enabled = answer >= 0 && !revealed,
                    colors = ButtonDefaults.buttonColors(containerColor = Mint, contentColor = Ink),
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (revealed) "تم كشف نتيجة التحقيق" else "اعرض نتيجة التحقيق", fontWeight = FontWeight.Bold) }
            }
        }
        if (revealed) {
            item {
                Surface(color = Panel2, shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Text(if (answer == c.correct) "أحسنت، استدلال موفق" else "لنراجع مسار التفكير", color = Mint, fontWeight = FontWeight.Bold)
                        Text("التفسير السريري", color = Gold, fontWeight = FontWeight.Bold)
                        Text(c.explanation, color = Paper, lineHeight = 24.sp)
                        Text("الفكرة التي تستحق الاحتفاظ", color = Gold, fontWeight = FontWeight.Bold)
                        Text(c.takeHome, color = Paper)
                        Text(c.source, color = Muted, fontSize = 11.sp)
                    }
                }
            }
        }
        item {
            OutlinedButton(onClick = onSave, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = Mint)) {
                Icon(if (isSaved) Icons.Rounded.BookmarkAdded else Icons.Rounded.BookmarkAdd, null)
                Spacer(Modifier.width(8.dp)); Text(if (isSaved) "إزالة من المكتبة" else "احفظ الفكرة في مكتبتي")
            }
        }
    }
}

@Composable
fun LibraryScreen(saved: Set<String>, onOpen: (ClinicalCase) -> Unit) {
    val savedCases = cases.filter { saved.contains(it.id.toString()) }
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("مكتبة الأفكار", color = Paper, fontSize = 27.sp, fontWeight = FontWeight.Bold)
        Text("الحالات التي قررت الاحتفاظ بها.", color = Muted)
        Spacer(Modifier.height(16.dp))
        if (savedCases.isEmpty()) {
            Surface(color = Panel, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.Bookmarks, null, tint = Mint, modifier = Modifier.size(36.dp))
                    Text("مكتبتك تنتظر أول فكرة", color = Paper, fontWeight = FontWeight.Bold)
                    Text("احفظ أي حالة أو معلومة مهمة لتعود إليها لاحقاً.", color = Muted, textAlign = TextAlign.Center, fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(savedCases) { CaseCard(it) { onOpen(it) } }
            }
        }
    }
}

@Composable
fun AssistantScreen() {
    var prompt by remember { mutableStateOf("") }
    var response by remember { mutableStateOf("أنا زميلك في التفكير الطبي. اكتب سؤالاً تعليمياً أو اطلب تفكيك حالة. ستحتاج النسخة المتصلة إلى إعداد خادم آمن ومفتاح API.") }
    var loading by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("زميل التفكير", color = Paper, fontSize = 27.sp, fontWeight = FontWeight.Bold)
        Text("مساعد بحثي، لا بديل عن الحكم السريري.", color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(18.dp))
        Surface(color = Panel, shape = RoundedCornerShape(18.dp), modifier = Modifier.weight(1f).fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("المساعد الذكي", color = Mint, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Text(response, color = Paper, lineHeight = 25.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = prompt, onValueChange = { prompt = it }, modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("مثلاً: ناقش لي أسباب ارتفاع اللاكتات...") },
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Paper, unfocusedTextColor = Paper, focusedBorderColor = Mint, unfocusedBorderColor = Panel2)
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = {
            response = "ميزة الذكاء الاصطناعي جاهزة للربط بالخادم. أضف عنوان الخادم ومفتاح المزود في إعدادات النشر؛ لا تضع مفتاح API داخل التطبيق."
            prompt = ""
        }, enabled = prompt.isNotBlank() && !loading, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Mint, contentColor = Ink)) {
            Icon(Icons.Rounded.AutoAwesome, null); Spacer(Modifier.width(8.dp)); Text("ابدأ التحقيق")
        }
        Text("لا تدخل بيانات تعريفية عن المرضى أو معلومات سرية.", color = Muted, fontSize = 10.sp)
    }
}

@Composable
fun ProfileScreen(specialty: String, onSpecialty: (String) -> Unit) {
    val specialties = listOf("الطوارئ", "العناية المركزة", "الباطنة", "القلب", "الأطفال", "الجراحة")
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("ملفي الطبي التعليمي", color = Paper, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("اختر المجال الذي تريد أن تظهر حالاته أولاً.", color = Muted)
        specialties.forEach { s ->
            Surface(color = if (s == specialty) Panel2 else Panel, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().clickable { onSpecialty(s) }) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(s, color = Paper, modifier = Modifier.weight(1f))
                    if (s == specialty) Icon(Icons.Rounded.CheckCircle, null, tint = Mint)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("إعدادات التعلّم المتقدمة، التنبيهات، اللغة، ومزامنة الحساب ستضاف في مراحل الربط الخلفي.", color = Muted, fontSize = 12.sp, lineHeight = 20.sp)
    }
}
