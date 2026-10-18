package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ManageSearch
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DeckType
import com.example.data.MasteryLevel
import com.example.data.WordEntity
import com.example.ui.theme.*

fun deckAccentColor(deck: String): Color = when (DeckType.fromString(deck)) {
    DeckType.OXFORD -> OxfordGold
    DeckType.IELTS -> IeltsTeal
    DeckType.IT -> ItMaroon
}

fun deckSoftColor(deck: String): Color = when (DeckType.fromString(deck)) {
    DeckType.OXFORD -> OxfordGoldSoft
    DeckType.IELTS -> IeltsTealSoft
    DeckType.IT -> ItMaroonSoft
}

@Composable
fun HighlightedExampleSentence(
    sentence: String,
    targetWord: String,
    highlightColor: Color = OxfordGold,
    fontSizeSp: Int = 15
) {
    val annotated = buildAnnotatedString {
        if (targetWord.isBlank()) {
            append(sentence)
            return@buildAnnotatedString
        }
        val regex = Regex("(?i)\\b${Regex.escape(targetWord)}(?:s|es|ed|ing)?\\b")
        var lastIdx = 0
        regex.findAll(sentence).forEach { match ->
            if (match.range.first > lastIdx) {
                append(sentence.substring(lastIdx, match.range.first))
            }
            withStyle(SpanStyle(color = highlightColor, fontWeight = FontWeight.Bold)) {
                append(match.value)
            }
            lastIdx = match.range.last + 1
        }
        if (lastIdx < sentence.length) {
            append(sentence.substring(lastIdx))
        }
    }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Text(
            text = annotated,
            fontFamily = InterFontFamily,
            fontSize = fontSizeSp.sp,
            color = Color(0xFFF1F5F9),
            lineHeight = (fontSizeSp + 6).sp
        )
    }
}

/**
 * Trancy-inspired Vocabulary Flashcard (modeled directly on Screenshot 1):
 * - Centered bold word + US/GB phonetic audio pill
 * - Golden `n.` / `v.` POS tag + primary Arabic meanings
 * - Golden `web.` tag + extended Arabic synonyms
 * - Two bilingual example sentences with speaker button & gold-highlighted target word
 * - Bottom bar with `Detailed Definition` button and `Mastered [M]` toggle
 */
@Composable
fun TrancyWordCard(
    word: WordEntity,
    arabicDialect: String,
    onSpeakUs: () -> Unit,
    onSpeakGb: () -> Unit,
    onSpeakSentence1: () -> Unit,
    onSpeakSentence2: () -> Unit,
    onOpenAiDefinition: () -> Unit,
    onToggleMastered: () -> Unit,
    modifier: Modifier = Modifier,
    posVariants: List<WordEntity> = emptyList(),
    onSelectVariant: (WordEntity) -> Unit = {},
    isEnriching: Boolean = false
) {
    val accent = deckAccentColor(word.deck)
    val isMastered = word.masteryLevel == MasteryLevel.MASTERED.name
    val tipText = if (arabicDialect == "EGYPTIAN" && word.arabicSpeakerTipEgyptian.isNotBlank()) {
        word.arabicSpeakerTipEgyptian
    } else {
        word.arabicSpeakerTipMsa
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("trancy_word_card_${word.word}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141821)),
        border = BorderStroke(1.dp, Color(0xFF262D3D))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top badges: Deck + CEFR Level + POS Tabs for same word
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = accent.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.45f))
                ) {
                    Text(
                        text = "${DeckType.fromString(word.deck).titleEn} • ${word.cefrLevel}",
                        color = accent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    color = Color(0xFF1F2735),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = MasteryLevel.entries.find { it.name == word.masteryLevel }?.labelAr ?: "جديدة",
                        color = if (isMastered) OxfordGold else Color(0xFFCBD5E1),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Multi-POS Tabs if the word has multiple parts of speech
            if (posVariants.size > 1) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    posVariants.forEach { variant ->
                        val isSelected = variant.id == word.id
                        Surface(
                            color = if (isSelected) accent.copy(alpha = 0.25f) else Color(0xFF1A202C),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (isSelected) accent else Color(0xFF2D3748)),
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSelectVariant(variant) }
                        ) {
                            Text(
                                text = variant.pos.ifBlank { "word" },
                                color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            if (isEnriching) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
                    color = accent,
                    trackColor = Color(0xFF1F2430)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main English Word
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Text(
                    text = word.word,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // US & GB Phonetic Audio Pill Row
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PhoneticAudioChip(
                        region = "US",
                        ipa = "[${word.ipaUs}]",
                        onClick = onSpeakUs
                    )
                    PhoneticAudioChip(
                        region = "GB",
                        ipa = "[${word.ipaGb.ifBlank { word.ipaUs }}]",
                        onClick = onSpeakGb
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Primary Arabic Translation Row: "n. ماشية ; بهائم ; أنعام"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = word.arabicTranslation,
                    fontFamily = CairoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp,
                    color = Color(0xFFF1F5F9),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = word.partOfSpeech,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = OxfordGold
                )
            }

            // Secondary POS row if present (e.g. n. تخلٍّ ; هجران)
            if (word.secondaryPos.isNotBlank() && word.secondaryArabic.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = word.secondaryArabic,
                        fontFamily = CairoFontFamily,
                        fontSize = 16.sp,
                        color = Color(0xFFCBD5E1),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = word.secondaryPos,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = OxfordGold
                    )
                }
            }

            // Secondary Web Arabic Synonyms Row: "web. أبقار ; مواش ; الأنعام"
            if (word.webArabicSynonyms.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = word.webArabicSynonyms,
                        fontFamily = CairoFontFamily,
                        fontSize = 15.sp,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "web.",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = OxfordGold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = Color(0xFF232A38))
            Spacer(modifier = Modifier.height(16.dp))

            // Example Sentence 1 with speaker icon & highlighted word
            ExampleSentenceRow(
                sentenceEn = word.exampleEn1,
                sentenceAr = word.exampleAr1,
                targetWord = word.word,
                onSpeak = onSpeakSentence1
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Example Sentence 2
            ExampleSentenceRow(
                sentenceEn = word.exampleEn2,
                sentenceAr = word.exampleAr2,
                targetWord = word.word,
                onSpeak = onSpeakSentence2
            )

            // Arabic Speaker Hint Box
            if (tipText.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    color = Color(0xFF1B2432),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = "نصيحة للمتحدث العربي",
                            tint = OxfordGold,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = tipText,
                            fontSize = 13.sp,
                            color = Color(0xFFE2E8F0),
                            lineHeight = 19.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            HorizontalDivider(color = Color(0xFF232A38))
            Spacer(modifier = Modifier.height(10.dp))

            // Footer Bar: Detailed AI Definition (left) & Mastered [M] toggle (right)
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onOpenAiDefinition,
                        modifier = Modifier.testTag("detailed_definition_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ManageSearch,
                            contentDescription = "Detailed Definition",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Detailed Definition",
                            color = Color(0xFFCBD5E1),
                            fontSize = 13.sp,
                            fontFamily = InterFontFamily
                        )
                    }

                    TextButton(
                        onClick = onToggleMastered,
                        modifier = Modifier.testTag("toggle_mastered_button")
                    ) {
                        Icon(
                            imageVector = if (isMastered) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                            contentDescription = "Mastered",
                            tint = if (isMastered) OxfordGold else Color(0xFF94A3B8),
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Mastered",
                            color = if (isMastered) OxfordGold else Color(0xFFCBD5E1),
                            fontSize = 13.sp,
                            fontFamily = InterFontFamily
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = Color(0xFF252D3D),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "M",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                fontFamily = InterFontFamily,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PhoneticAudioChip(
    region: String,
    ipa: String,
    onClick: () -> Unit
) {
    Surface(
        color = Color(0xFF1E2430),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFF2E3748)),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = Color(0xFF2D3748),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = region,
                    fontSize = 11.sp,
                    color = Color(0xFFCBD5E1),
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = ipa,
                fontSize = 13.sp,
                color = Color(0xFFCBD5E1),
                fontFamily = InterFontFamily
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = "استمع للنطق $region",
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun ExampleSentenceRow(
    sentenceEn: String,
    sentenceAr: String,
    targetWord: String,
    onSpeak: () -> Unit
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            IconButton(
                onClick = onSpeak,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "تشغيل الجملة",
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                HighlightedExampleSentence(
                    sentence = sentenceEn,
                    targetWord = targetWord,
                    highlightColor = OxfordGold,
                    fontSizeSp = 15
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = sentenceAr,
                    fontFamily = CairoFontFamily,
                    fontSize = 14.sp,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

/**
 * Reusable inner content for the "AI Definition" panel (used both in the ModalBottomSheet and Side-by-Side Drawer).
 */
@Composable
fun AiDefinitionPanelContent(
    word: WordEntity,
    onClose: () -> Unit,
    onSpeakUs: () -> Unit,
    onSpeakGb: () -> Unit,
    onSpeakText: (String) -> Unit,
    onToggleMastered: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val posFull = when (word.partOfSpeech.lowercase()) {
        "n." -> "Noun"
        "v." -> "Verb"
        "adj." -> "Adjective"
        "adv." -> "Adverb"
        else -> word.partOfSpeech
    }
    val isMastered = word.masteryLevel == MasteryLevel.MASTERED.name

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        // Top Header: Close + AI Definition + Heart & Mastered Actions
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق",
                            tint = Color(0xFF94A3B8)
                        )
                    }
                    Text(
                        text = "AI Definition",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                }
                Surface(
                    color = Color(0xFF1E2430),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color(0xFF2D3748))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (word.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "مفضلة",
                                tint = if (word.isFavorite) PhonemeRed else Color(0xFF94A3B8),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onToggleMastered,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isMastered) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                contentDescription = "إتقان الكلمة",
                                tint = if (isMastered) OxfordGold else Color(0xFF94A3B8),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Syllabified Title ("cat·tle")
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Text(
                text = word.syllabified.ifBlank { word.word },
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PhoneticAudioChip(region = "US", ipa = word.ipaUs, onClick = onSpeakUs)
                PhoneticAudioChip(region = "GB", ipa = word.ipaGb.ifBlank { word.ipaUs }, onClick = onSpeakGb)
            }

            Spacer(modifier = Modifier.height(18.dp))

            // [n.] Noun
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = Color(0xFF262D3D),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = word.partOfSpeech,
                        color = OxfordGold,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = posFull,
                    color = Color.White,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Bold Arabic Meanings
        Text(
            text = word.arabicTranslation,
            fontFamily = CairoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(6.dp))

        // English Definition
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Text(
                text = word.englishDefinition,
                fontFamily = InterFontFamily,
                fontSize = 14.sp,
                color = Color(0xFF94A3B8),
                lineHeight = 20.sp
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Vertical Left-Bordered Quote Example
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
            ) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .fillMaxHeight()
                        .background(Color(0xFF475569), RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.weight(1f)) {
                            HighlightedExampleSentence(
                                sentence = word.exampleEn1,
                                targetWord = word.word,
                                fontSizeSp = 14
                            )
                        }
                        IconButton(
                            onClick = { onSpeakText(word.exampleEn1) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "تشغيل المثال",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Text(
                        text = word.exampleAr1,
                        fontFamily = CairoFontFamily,
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        }

        // Grammar / Plural Pill Box (e.g. "جمع cattle")
        if (word.pluralOrGrammarNote.isNotBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                color = Color(0xFF1E2430),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = word.pluralOrGrammarNote,
                    fontFamily = CairoFontFamily,
                    fontSize = 14.sp,
                    color = Color(0xFFE2E8F0),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        }

        // | Etymology Section
        if (word.etymologyAr.isNotBlank()) {
            Spacer(modifier = Modifier.height(20.dp))
            SectionHeaderPipe(title = "Etymology")
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = word.etymologyAr,
                fontFamily = CairoFontFamily,
                fontSize = 14.sp,
                color = Color(0xFFCBD5E1),
                lineHeight = 22.sp
            )
        }

        // | Examples Section
        Spacer(modifier = Modifier.height(20.dp))
        SectionHeaderPipe(title = "Examples")
        Spacer(modifier = Modifier.height(8.dp))
        ExampleSentenceRow(
            sentenceEn = word.exampleEn2,
            sentenceAr = word.exampleAr2,
            targetWord = word.word,
            onSpeak = { onSpeakText(word.exampleEn2) }
        )

        if (word.synonyms.isNotBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Text(
                    text = "Synonyms: ${word.synonyms}",
                    fontFamily = InterFontFamily,
                    fontSize = 13.sp,
                    color = EmeraldPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiDefinitionBottomSheet(
    word: WordEntity,
    arabicDialect: String,
    onDismiss: () -> Unit,
    onSpeakUs: () -> Unit,
    onSpeakGb: () -> Unit,
    onSpeakText: (String) -> Unit,
    onToggleMastered: () -> Unit,
    onToggleFavorite: () -> Unit = {}
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF141821),
        contentColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        AiDefinitionPanelContent(
            word = word,
            onClose = onDismiss,
            onSpeakUs = onSpeakUs,
            onSpeakGb = onSpeakGb,
            onSpeakText = onSpeakText,
            onToggleMastered = onToggleMastered,
            onToggleFavorite = onToggleFavorite,
            modifier = Modifier.navigationBarsPadding()
        )
    }
}

@Composable
private fun SectionHeaderPipe(title: String) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(16.dp)
                    .background(OxfordGold, RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color.White
            )
        }
    }
}
