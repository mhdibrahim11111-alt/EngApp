package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.components.TrancyWordCard
import com.example.ui.theme.*

/**
 * Trancy + SM-2 Spaced Repetition Flashcard Screen.
 * Uses real totals from the database for the "1 / N" counter.
 */
@Composable
fun SrsReviewScreen(
    queue: List<WordEntity>,
    currentIndex: Int,
    arabicDialect: String,
    totalDeckCount: Int,
    onNavigateCard: (Int) -> Unit,
    onGrade: (SrsGrade) -> Unit,
    onInspectWord: (WordEntity) -> Unit,
    onToggleMastered: (WordEntity) -> Unit,
    onSpeakWord: (WordEntity, Boolean) -> Unit,
    onSwitchToListView: () -> Unit = {},
    posVariants: List<WordEntity> = emptyList(),
    onSelectVariant: (WordEntity) -> Unit = {},
    isEnriching: Boolean = false,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val currentWord = queue.getOrNull(currentIndex)
    val displayTotal = if (totalDeckCount > 0) totalDeckCount else queue.size

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0B0E14)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Trancy-style Header Bar
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilledTonalIconButton(
                            onClick = onBack,
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = Color(0xFF1A202C),
                                contentColor = Color.White
                            ),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "رجوع")
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "CEFR_${currentWord?.level?.ifBlank { "independent" } ?: "independent"}",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = Color.White
                        )
                    }

                    // Gold index / real total counter: e.g. "1 / 5836"
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${currentIndex + 1}",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = OxfordGold
                        )
                        Text(
                            text = " / $displayTotal",
                            fontFamily = InterFontFamily,
                            fontSize = 14.sp,
                            color = Color(0xFF94A3B8)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        FilledTonalIconButton(
                            onClick = onSwitchToListView,
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = Color(0xFF1A202C),
                                contentColor = OxfordGold
                            ),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.FormatListBulleted, contentDescription = "Word List Table")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (currentWord == null) {
                Text(
                    text = "لا توجد بطاقات مستحقة حالياً!",
                    color = Color.White,
                    fontSize = 18.sp
                )
            } else {
                // Card Navigation Row (< Prev | Next >)
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedIconButton(
                            onClick = { onNavigateCard(-1) },
                            enabled = currentIndex > 0,
                            shape = CircleShape,
                            border = BorderStroke(1.dp, Color(0xFF2D3748))
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "السابق",
                                tint = Color.White
                            )
                        }

                        Text(
                            text = "بطاقة ${currentIndex + 1} من ${queue.size} في جلسة اليوم",
                            fontFamily = CairoFontFamily,
                            fontSize = 13.sp,
                            color = Color(0xFF94A3B8)
                        )

                        OutlinedIconButton(
                            onClick = { onNavigateCard(1) },
                            enabled = currentIndex < queue.lastIndex,
                            shape = CircleShape,
                            border = BorderStroke(1.dp, Color(0xFF2D3748))
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "التالي",
                                tint = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Main Trancy Word Card with POS tabs and shimmer
                TrancyWordCard(
                    word = currentWord,
                    arabicDialect = arabicDialect,
                    onSpeakUs = { onSpeakWord(currentWord, false) },
                    onSpeakGb = { onSpeakWord(currentWord, true) },
                    onSpeakSentence1 = { onSpeakWord(currentWord, false) },
                    onSpeakSentence2 = { onSpeakWord(currentWord, false) },
                    onOpenAiDefinition = { onInspectWord(currentWord) },
                    onToggleMastered = { onToggleMastered(currentWord) },
                    posVariants = posVariants,
                    onSelectVariant = onSelectVariant,
                    isEnriching = isEnriching
                )

                Spacer(modifier = Modifier.height(18.dp))

                // SM-2 Spaced Repetition Grading Buttons (Again / Hard / Good / Easy)
                Text(
                    text = "جدولة التكرار المتباعد (SM-2) — كيف كان تذكرك للكلمة؟",
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8)
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SrsGrade.entries.forEach { grade ->
                        val btnColor = when (grade) {
                            SrsGrade.AGAIN -> PhonemeRed
                            SrsGrade.HARD -> PhonemeYellow
                            SrsGrade.GOOD -> EmeraldPrimary
                            SrsGrade.EASY -> FreezeIceBlue
                        }
                        OutlinedButton(
                            onClick = { onGrade(grade) },
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp)
                                .testTag("srs_grade_${grade.name.lowercase()}"),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.5.dp, btnColor.copy(alpha = 0.8f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = btnColor.copy(alpha = 0.12f)
                            ),
                            contentPadding = PaddingValues(4.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = grade.labelAr,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = btnColor
                                )
                                Text(
                                    text = "${grade.labelEn} • ${grade.intervalHintAr}",
                                    fontSize = 10.sp,
                                    color = Color(0xFFCBD5E1)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * STEP 2: Bulk Import Screen with CSV / JSON parsing, first rows preview, row count, target deck picker,
 * duplicate skipping, and progress bar while importing.
 */
@Composable
fun CsvJsonImportScreen(
    preview: ImportPreview?,
    importProgress: Pair<Int, Int>?,
    onPreview: (rawText: String, deck: String) -> Unit,
    onConfirmImport: (targetDeck: String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    var selectedDeck by remember { mutableStateOf("oxford5000") }
    var rawInput by remember {
        mutableStateOf(
            """
            word,pos,level,deck,arabic,definition,example_en,example_ar,ipa
            microservice,noun,C1,it_industry,خدمة مصغرة,independent software component,We split the monolith into microservices.,قسمنا التطبيق إلى خدمات مصغرة,/ˈmaɪkrəʊsɜːrvɪs/
            meticulous,adjective,C1,ielts_core,دقيق للغاية ; حريص,showing great attention to detail,The researcher kept meticulous notes.,احتفظ الباحث بملاحظات دقيقة,/məˈtɪkjələs/
            thrive,verb,B2,oxford5000,يزدهر ; ينمو بقوة,to grow or develop well,Small businesses thrive with digital tools.,تزدهر المشاريع الصغيرة بالحلول الرقمية,/θraɪv/
            """.trimIndent()
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
            }
            Text(
                text = "استيراد البطاقات (CSV / JSON) مع المعاينة والتقدم",
                style = MaterialTheme.typography.titleLarge
            )
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "الأعمدة المدعومة (CSV أو JSON):",
                    fontWeight = FontWeight.Bold,
                    color = OxfordGold
                )
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Text(
                        text = "word/text, pos, level, deck, arabic, definition, example_en, example_ar, ipa",
                        fontFamily = InterFontFamily,
                        fontSize = 12.sp,
                        color = EmeraldPrimary
                    )
                }
                Text(
                    text = "يتم تخطي التكرار تلقائياً (نفس الكلمة + نوع الكلمة + الحزمة). يمكنك اختيار الحزمة المستهدفة أدناه.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Target Deck Picker Chips
        Text("اختر الحزمة المستهدفة للاستيراد:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DeckType.entries.forEach { d ->
                FilterChip(
                    selected = selectedDeck == d.id,
                    onClick = {
                        selectedDeck = d.id
                        if (rawInput.isNotBlank()) onPreview(rawInput, d.id)
                    },
                    label = { Text(d.titleEn, fontSize = 12.sp) }
                )
            }
        }

        // Quick Template Buttons
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    rawInput = """
                        word,pos,level,deck,arabic,definition,example_en,example_ar,ipa
                        microservice,noun,C1,it_industry,خدمة مصغرة,independent software component,We split the monolith into microservices.,قسمنا التطبيق إلى خدمات مصغرة,/ˈmaɪkrəʊsɜːrvɪs/
                        meticulous,adjective,C1,ielts_core,دقيق للغاية,showing great attention to detail,The researcher kept meticulous notes.,احتفظ الباحث بملاحظات دقيقة,/məˈtɪkjələs/
                    """.trimIndent()
                    onPreview(rawInput, selectedDeck)
                }
            ) {
                Text("قالب CSV")
            }
            OutlinedButton(
                onClick = {
                    rawInput = """
                        [
                          {
                            "text": "observability",
                            "pos": "noun",
                            "level": "C1",
                            "deck": "it_industry",
                            "arabic": "القابلية للمراقبة والرصد",
                            "definition_en": "measure of how well internal states can be inferred",
                            "examples_en": "Observability helps engineers debug distributed services."
                          }
                        ]
                    """.trimIndent()
                    onPreview(rawInput, selectedDeck)
                }
            ) {
                Text("قالب JSON")
            }
        }

        // Input Editor
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            OutlinedTextField(
                value = rawInput,
                onValueChange = {
                    rawInput = it
                    onPreview(it, selectedDeck)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .testTag("csv_json_input_field"),
                label = { Text("CSV / JSON Data") },
                textStyle = LocalTextStyle.current.copy(fontFamily = InterFontFamily, fontSize = 12.sp)
            )
        }

        // Preview Section
        if (preview != null && preview.rowCount > 0) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131924)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, OxfordGold.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "معاينة البيانات (الصفوف الأولى)",
                            fontWeight = FontWeight.Bold,
                            color = OxfordGold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "إجمالي الصفوف: ${preview.rowCount}",
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary,
                            fontSize = 13.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    preview.firstRows.forEachIndexed { idx, row ->
                        Text(
                            text = "${idx + 1}. ${row.joinToString("  |  ")}",
                            fontSize = 11.sp,
                            color = Color(0xFFCBD5E1),
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }

        // Progress Bar while importing
        if (importProgress != null) {
            val (current, total) = importProgress
            val progress = if (total > 0) current.toFloat() / total else 0f
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("جاري استيراد وحفظ الكلمات...", fontSize = 13.sp, color = EmeraldPrimary)
                    Text("$current / $total", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = EmeraldPrimary,
                    trackColor = Color(0xFF042F2E)
                )
            }
        }

        // Confirm Button
        Button(
            onClick = { onConfirmImport(selectedDeck) },
            enabled = importProgress == null && preview != null && preview.rowCount > 0,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("confirm_import_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
        ) {
            Icon(Icons.Default.Download, contentDescription = null, tint = Color(0xFF042F2E))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "بدء الاستيراد (${preview?.rowCount ?: 0} صف) إلى ${DeckType.fromString(selectedDeck).titleEn}",
                color = Color(0xFF042F2E),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * STEP 4: Generate Vocabulary with AI Screen
 */
@Composable
fun GenerateVocabularyScreen(
    progress: Pair<Int, Int>?,
    onStartGenerate: (topic: String, level: String, count: Int, deck: String) -> Unit,
    onCancel: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    var topic by remember { mutableStateOf("IT Industry & System Architecture") }
    var selectedLevel by remember { mutableStateOf("B2") }
    var targetCountText by remember { mutableStateOf("50") }
    var selectedDeck by remember { mutableStateOf("it_industry") }

    val levels = listOf("A1", "A2", "B1", "B2", "C1")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
            }
            Text(
                text = "توليد مفردات جديدة بالذكاء الاصطناعي (Gemini)",
                style = MaterialTheme.typography.titleLarge
            )
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("حدد موضوع الحزمة والمستوى والعدد المطلوب:", fontWeight = FontWeight.Bold, color = OxfordGold)
                Text("يقوم Gemini بتوليد دفعات من 50 كلمة جديدة حصراً (بدون أي تكرار مع الكلمات الموجودة في قاعدة البيانات) حتى الوصول للعدد المحدد.", fontSize = 12.sp)
            }
        }

        OutlinedTextField(
            value = topic,
            onValueChange = { topic = it },
            label = { Text("موضوع الحزمة (Topic)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Text("المستوى الأوروبي (CEFR Level):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            levels.forEach { lvl ->
                FilterChip(
                    selected = selectedLevel == lvl,
                    onClick = { selectedLevel = lvl },
                    label = { Text(lvl) }
                )
            }
        }

        OutlinedTextField(
            value = targetCountText,
            onValueChange = { targetCountText = it },
            label = { Text("عدد الكلمات المطلوب (مثلاً: 50، 100، 200)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Text("الحفظ في الحزمة (Target Deck):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DeckType.entries.forEach { d ->
                FilterChip(
                    selected = selectedDeck == d.id,
                    onClick = { selectedDeck = d.id },
                    label = { Text(d.titleEn, fontSize = 11.sp) }
                )
            }
        }

        // Progress display while generating
        if (progress != null) {
            val (current, total) = progress
            val ratio = if (total > 0) current.toFloat() / total else 0f
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("جاري توليد الكلمات دفعة تلو الأخرى...", fontSize = 13.sp, color = OxfordGold)
                    Text("$current / $total", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = OxfordGold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { ratio },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = OxfordGold,
                    trackColor = Color(0xFF282F3E)
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PhonemeRed)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إلغاء التوليد")
                }
            }
        } else {
            Button(
                onClick = {
                    val count = targetCountText.toIntOrNull() ?: 50
                    onStartGenerate(topic, selectedLevel, count, selectedDeck)
                },
                enabled = topic.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OxfordGold)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF0F141C))
                Spacer(modifier = Modifier.width(8.dp))
                Text("بدء توليد الكلمات وحفظها", color = Color(0xFF0F141C), fontWeight = FontWeight.Bold)
            }
        }
    }
}
