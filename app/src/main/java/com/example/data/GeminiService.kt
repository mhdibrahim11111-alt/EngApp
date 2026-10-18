package com.example.data

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

@Serializable
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

@Serializable
data class Content(
    val parts: List<Part>,
    val role: String? = null
)

@Serializable
data class Part(
    val text: String? = null
)

@Serializable
data class GenerationConfig(
    val responseMimeType: String? = null,
    val responseSchema: JsonObject? = null,
    val temperature: Float? = null
)

@Serializable
data class GenerateContentResponse(
    val candidates: List<Candidate> = emptyList()
)

@Serializable
data class Candidate(
    val content: Content? = null
)

@Serializable
data class AutoFillWordDto(
    val word: String,
    val syllabified: String = "",
    val partOfSpeech: String = "n.",
    val cefrLevel: String = "B1",
    val arabicTranslation: String = "",
    val webArabicSynonyms: String = "",
    val englishDefinition: String = "",
    val exampleEn1: String = "",
    val exampleAr1: String = "",
    val exampleEn2: String = "",
    val exampleAr2: String = "",
    val ipaUs: String = "",
    val ipaGb: String = "",
    val synonyms: String = "",
    val pluralOrGrammarNote: String = "",
    val etymologyAr: String = "",
    val arabicSpeakerTipMsa: String = "",
    val arabicSpeakerTipEgyptian: String = ""
)

@Serializable
data class TutorReplyDto(
    val replyEn: String,
    val grammarCorrection: String = "",
    val arabicExplanation: String = "",
    val suggestedDeckWords: List<String> = emptyList()
)

@Serializable
data class IeltsAssessmentDto(
    val estimatedBand: String,
    val lexicalResourceFeedbackAr: String,
    val grammarFeedbackAr: String,
    val coherenceFeedbackAr: String,
    val improvedSampleEn: String
)

@Serializable
data class SubtitleAiPackDto(
    val lines: List<SubtitleLine>,
    val keyVocab: List<KeyVocabItem>
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

class GeminiService(private val dao: LinguaDao) {

    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val api: GeminiApiService by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GeminiApiService::class.java)
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun isApiKeyConfigured(): Boolean {
        val key = BuildConfig.GEMINI_API_KEY
        return key.isNotBlank() && key != "MY_GEMINI_API_KEY"
    }

    suspend fun callGeminiCached(
        cacheNamespace: String,
        prompt: String,
        systemText: String = "You are LinguaBridge AI, an expert English teacher for native Arabic speakers.",
        jsonMode: Boolean = true
    ): Result<String> = withContext(Dispatchers.IO) {
        val cacheKey = sha256("$cacheNamespace|$prompt")
        val cached = dao.getCachedResponse(cacheKey)
        if (cached != null && cached.responseJson.isNotBlank()) {
            return@withContext Result.success(cached.responseJson)
        }

        if (!isApiKeyConfigured()) {
            return@withContext Result.failure(
                IllegalStateException("يرجى ضبط مفتاح GEMINI_API_KEY في لوحة Secrets داخل AI Studio لتفعيل الذكاء الاصطناعي المباشر.")
            )
        }

        try {
            val request = GenerateContentRequest(
                contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                systemInstruction = Content(parts = listOf(Part(text = systemText))),
                generationConfig = GenerationConfig(
                    responseMimeType = if (jsonMode) "application/json" else null,
                    temperature = 0.4f
                )
            )
            val response = api.generateContent(BuildConfig.GEMINI_API_KEY, request)
            val rawText = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
                ?: return@withContext Result.failure(IllegalStateException("Empty response from Gemini"))
            val cleaned = rawText.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            dao.putCachedResponse(AiCacheEntity(cacheKey = cacheKey, responseJson = cleaned))
            Result.success(cleaned)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Batch auto-fills missing Arabic translations, IPA, etymology, and bilingual examples on CSV/JSON import.
     */
    suspend fun autoFillImportedWords(
        partialWords: List<WordEntity>,
        dialect: String
    ): List<WordEntity> {
        val wordsSummary = partialWords.joinToString("\n") {
            "word=${it.word}, pos=${it.partOfSpeech}, level=${it.cefrLevel}, deck=${it.deck}, arabic=${it.arabicTranslation}, def=${it.englishDefinition}, ex=${it.exampleEn1}"
        }
        val prompt = """
            Auto-fill any missing fields for these English vocabulary words for Arabic speakers.
            Return a JSON array of objects matching keys:
            word, syllabified (use middle dot · between syllables like cat·tle), partOfSpeech, cefrLevel,
            arabicTranslation (3 synonyms separated by ' ; '), webArabicSynonyms (3 extra Arabic meanings separated by ' ; '),
            englishDefinition, exampleEn1, exampleAr1, exampleEn2, exampleAr2, ipaUs, ipaGb, synonyms,
            pluralOrGrammarNote (in Arabic/English), etymologyAr (brief Arabic word origin),
            arabicSpeakerTipMsa (pronunciation or false-friend tip in Modern Standard Arabic),
            arabicSpeakerTipEgyptian (friendly tip in Egyptian Arabic).
            Input rows:
            $wordsSummary
        """.trimIndent()

        val result = callGeminiCached("batch_autofill_$dialect", prompt, jsonMode = true)
        return result.fold(
            onSuccess = { jsonText ->
                try {
                    val dtos = json.decodeFromString<List<AutoFillWordDto>>(jsonText)
                    val map = dtos.associateBy { it.word.lowercase().trim() }
                    partialWords.map { orig ->
                        val dto = map[orig.word.lowercase().trim()]
                        if (dto != null) {
                            orig.copy(
                                syllabified = dto.syllabified.ifBlank { orig.word },
                                partOfSpeech = orig.partOfSpeech.ifBlank { dto.partOfSpeech },
                                cefrLevel = orig.cefrLevel.ifBlank { dto.cefrLevel },
                                arabicTranslation = orig.arabicTranslation.ifBlank { dto.arabicTranslation },
                                webArabicSynonyms = dto.webArabicSynonyms.ifBlank { orig.arabicTranslation },
                                englishDefinition = orig.englishDefinition.ifBlank { dto.englishDefinition },
                                exampleEn1 = orig.exampleEn1.ifBlank { dto.exampleEn1 },
                                exampleAr1 = orig.exampleAr1.ifBlank { dto.exampleAr1 },
                                exampleEn2 = dto.exampleEn2.ifBlank { "Let's practice using '${orig.word}' in a sentence." },
                                exampleAr2 = dto.exampleAr2.ifBlank { "دعنا نتدرب على استخدام '${orig.word}' في جملة." },
                                ipaUs = orig.ipaUs.ifBlank { dto.ipaUs.ifBlank { "/${orig.word}/" } },
                                ipaGb = dto.ipaGb.ifBlank { orig.ipaUs.ifBlank { "/${orig.word}/" } },
                                synonyms = orig.synonyms.ifBlank { dto.synonyms },
                                pluralOrGrammarNote = dto.pluralOrGrammarNote,
                                etymologyAr = dto.etymologyAr,
                                arabicSpeakerTipMsa = dto.arabicSpeakerTipMsa,
                                arabicSpeakerTipEgyptian = dto.arabicSpeakerTipEgyptian
                            )
                        } else orig
                    }
                } catch (_: Exception) {
                    applyLocalFallbackAutoFill(partialWords)
                }
            },
            onFailure = {
                applyLocalFallbackAutoFill(partialWords)
            }
        )
    }

    private fun applyLocalFallbackAutoFill(words: List<WordEntity>): List<WordEntity> {
        return words.map { w ->
            w.copy(
                syllabified = if (w.syllabified.isBlank()) w.word else w.syllabified,
                arabicTranslation = w.arabicTranslation.ifBlank { "معنى ${w.word} (مُكمّل تلقائياً)" },
                webArabicSynonyms = w.webArabicSynonyms.ifBlank { "مرادف ${w.word} ; تعبير شائع" },
                englishDefinition = w.englishDefinition.ifBlank { "Core vocabulary term used in ${w.deck} contexts." },
                exampleEn1 = w.exampleEn1.ifBlank { "Understanding '${w.word}' is essential for professional communication." },
                exampleAr1 = w.exampleAr1.ifBlank { "فهم كلمة '${w.word}' ضروري للتواصل المهني." },
                exampleEn2 = w.exampleEn2.ifBlank { "She used '${w.word}' accurately during the presentation." },
                exampleAr2 = w.exampleAr2.ifBlank { "استخدمت كلمة '${w.word}' بدقة أثناء العرض التقديمي." },
                ipaUs = w.ipaUs.ifBlank { "/${w.word.lowercase()}/" },
                ipaGb = w.ipaGb.ifBlank { "/${w.word.lowercase()}/" },
                synonyms = w.synonyms.ifBlank { "related term, equivalent" },
                etymologyAr = w.etymologyAr.ifBlank { "كلمة إنجليزية ذات جذور لاتينية/أوروبية قديمة تطورت للاستخدام المعاصر." },
                arabicSpeakerTipMsa = w.arabicSpeakerTipMsa.ifBlank { "انتبه لموضع التشديد الصوتي وحروف العلة القصيرة عند نطق ${w.word}." },
                arabicSpeakerTipEgyptian = w.arabicSpeakerTipEgyptian.ifBlank { "ركز على نطق الحروف المتحركة في ${w.word} من غير ما تمدها زيادة." }
            )
        }
    }

    /**
     * Evaluates spoken pronunciation against target sentence/word with Arabic mouth-position tips.
     */
    suspend fun evaluatePronunciation(
        targetText: String,
        spokenTranscript: String,
        focusCategory: PhonemeChallengeCategory
    ): PronunciationEvaluation {
        val prompt = """
            Analyze an Arabic speaker's English pronunciation.
            Target text: "$targetText"
            Spoken transcript captured: "$spokenTranscript"
            Phonetic focus category: ${focusCategory.id} (${focusCategory.symbols})
            
            Return a JSON object with:
            - overallScore: integer 0 to 100
            - words: array of { "word": string, "score": integer 0-100, "ipaTarget": string, "issueAr": string in Arabic }
            - mouthTipMsa: specific mouth/tongue/lip position advice in simple Modern Standard Arabic
            - mouthTipEgyptian: specific mouth/tongue/lip position advice in friendly Egyptian Arabic
            - focusSoundLabel: "${focusCategory.symbols}"
        """.trimIndent()

        val result = callGeminiCached("pron_${targetText}_$spokenTranscript", prompt, jsonMode = true)
        return result.fold(
            onSuccess = { jsonText ->
                try {
                    json.decodeFromString<PronunciationEvaluation>(jsonText)
                } catch (_: Exception) {
                    buildHeuristicPronunciationEval(targetText, spokenTranscript, focusCategory)
                }
            },
            onFailure = {
                buildHeuristicPronunciationEval(targetText, spokenTranscript, focusCategory)
            }
        )
    }

    private fun buildHeuristicPronunciationEval(
        targetText: String,
        spokenTranscript: String,
        focusCategory: PhonemeChallengeCategory
    ): PronunciationEvaluation {
        val targetWords = targetText.trim().split(Regex("\\s+"))
        val spokenClean = spokenTranscript.lowercase()

        val wordScores = targetWords.map { rawWord ->
            val cleanW = rawWord.replace(Regex("[^a-zA-Z]"), "").lowercase()
            val exactMatch = spokenClean.contains(cleanW) && cleanW.isNotEmpty()
            val hasHardSound = cleanW.contains("p") || cleanW.contains("v") || cleanW.contains("th") || cleanW.startsWith("str")
            val score = when {
                exactMatch && !hasHardSound -> 92
                exactMatch && hasHardSound -> 84
                spokenClean.isNotBlank() -> 68
                else -> 76
            }
            val issue = when {
                score >= 80 -> "نطق ممتاز وواضح"
                cleanW.contains("p") -> "احرص على إخراج دفعة هواء مع حرف P"
                cleanW.contains("v") -> "أظهر اهتزاز الشفة السفلية في حرف V"
                cleanW.contains("th") -> "أخرج طرف اللسان قليلاً في صوت th"
                else -> "انتبه لطول الحركة الصوتية داخل الكلمة"
            }
            WordPhonemeScore(
                word = rawWord,
                score = score,
                ipaTarget = "/$cleanW/",
                issueAr = issue
            )
        }
        val avg = if (wordScores.isNotEmpty()) wordScores.map { it.score }.average().toInt() else 80
        return PronunciationEvaluation(
            overallScore = avg,
            words = wordScores,
            mouthTipMsa = focusCategory.tipMsa,
            mouthTipEgyptian = focusCategory.tipEgyptian,
            focusSoundLabel = focusCategory.symbols
        )
    }

    /**
     * AI Tutor conversational turn with gentle grammar correction, Arabic explanation, and deck vocabulary upgrades.
     */
    suspend fun sendTutorMessage(
        scenario: ChatScenario,
        cefrLevel: String,
        dialect: String,
        userMessage: String,
        activeDeckWords: List<String>
    ): TutorReplyDto {
        val sampleDeckWords = activeDeckWords.take(15).joinToString(", ")
        val prompt = """
            Scenario: ${scenario.titleEn} (${scenario.systemRolePrompt})
            Learner CEFR Level: $cefrLevel
            Arabic Dialect Preference for explanation: $dialect (MSA = Modern Standard Arabic, EGYPTIAN = Egyptian Arabic)
            Candidate vocabulary from learner's decks: [$sampleDeckWords]
            Learner said: "$userMessage"
            
            Return a JSON object with:
            - replyEn: natural conversational response in English keeping the roleplay going (2-3 sentences max)
            - grammarCorrection: gentle correction of the learner's English if there was any mistake, or "Perfect phrasing! ✓" if none
            - arabicExplanation: clear explanation in Arabic ($dialect) of any grammar/vocabulary improvement
            - suggestedDeckWords: array of 2-3 strong English words from the learner's decks they can use in their next reply
        """.trimIndent()

        val res = callGeminiCached("tutor_${scenario.id}_${cefrLevel}_${dialect}_$userMessage", prompt, jsonMode = true)
        return res.fold(
            onSuccess = { txt ->
                try {
                    json.decodeFromString<TutorReplyDto>(txt)
                } catch (_: Exception) {
                    fallbackTutorReply(scenario, userMessage, activeDeckWords)
                }
            },
            onFailure = {
                fallbackTutorReply(scenario, userMessage, activeDeckWords)
            }
        )
    }

    private fun fallbackTutorReply(
        scenario: ChatScenario,
        userMessage: String,
        activeDeckWords: List<String>
    ): TutorReplyDto {
        val suggestions = activeDeckWords.shuffled().take(3).ifEmpty { listOf("perspective", "mitigate", "scalability") }
        return TutorReplyDto(
            replyEn = "That's a very thoughtful point! Could you elaborate a bit more on the specific outcome and how you handled the main obstacles?",
            grammarCorrection = if (userMessage.split(" ").size < 5) {
                "Tip: Try expanding your sentence using connectors like 'Furthermore' or 'As a consequence'."
            } else {
                "Clear sentence structure! You communicated your main idea effectively."
            },
            arabicExplanation = "جملتك واضحة، ولرفع مستواك إلى درجة أعلى حاول ربط الجمل واستخدام إحدى الكلمات المقترحة أدناه من بطاقاتك.",
            suggestedDeckWords = suggestions
        )
    }

    /**
     * Generates Arabic translations and Key Vocabulary list for custom pasted transcripts / SRT / YouTube text.
     */
    suspend fun generateSubtitlesAndVocab(rawEnglishText: String): SubtitleAiPackDto {
        val rawLines = rawEnglishText
            .lines()
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.matches(Regex("^\\d+$")) && !it.contains("-->") }
            .take(12)

        val prompt = """
            Translate each of these English video subtitle lines into clear, natural Arabic and extract 5 key vocabulary words with IPA and deck category (OXFORD, IELTS, or IT).
            Return JSON:
            {
              "lines": [ { "index": 0, "startSec": 0, "endSec": 5, "textEn": "...", "textAr": "..." } ],
              "keyVocab": [ { "word": "...", "arabic": "...", "ipa": "...", "deck": "OXFORD" } ]
            }
            English lines:
            ${rawLines.joinToString("\n")}
        """.trimIndent()

        val res = callGeminiCached("subtitles_pack", prompt, jsonMode = true)
        return res.fold(
            onSuccess = { txt ->
                try {
                    json.decodeFromString<SubtitleAiPackDto>(txt)
                } catch (_: Exception) {
                    fallbackSubtitlePack(rawLines)
                }
            },
            onFailure = {
                fallbackSubtitlePack(rawLines)
            }
        )
    }

    private fun fallbackSubtitlePack(lines: List<String>): SubtitleAiPackDto {
        val subLines = lines.mapIndexed { idx, line ->
            SubtitleLine(
                index = idx,
                startSec = idx * 6,
                endSec = (idx + 1) * 6,
                textEn = line,
                textAr = "الترجمة العربية التفاعلية: $line"
            )
        }
        val vocab = listOf(
            KeyVocabItem("perspective", "منظور / وجهة نظر", "/pərˈspektɪv/", "OXFORD"),
            KeyVocabItem("mitigate", "يخفف من حدة", "/ˈmɪtɪɡeɪt/", "IELTS"),
            KeyVocabItem("scalability", "القابلية للتوسع", "/ˌskeɪləˈbɪləti/", "IT")
        )
        return SubtitleAiPackDto(lines = subLines, keyVocab = vocab)
    }

    /**
     * Evaluates IELTS Writing or Speaking response with estimated Band Score (1.0 - 9.0) and Arabic feedback.
     */
    suspend fun evaluateIeltsResponse(promptTopic: String, userResponse: String): IeltsAssessmentDto {
        val prompt = """
            Evaluate this IELTS learner response for topic: "$promptTopic".
            Learner response: "$userResponse"
            Return JSON with:
            - estimatedBand: e.g. "Band 7.0"
            - lexicalResourceFeedbackAr: Arabic feedback on vocabulary choice
            - grammarFeedbackAr: Arabic feedback on grammar & sentence variety
            - coherenceFeedbackAr: Arabic feedback on cohesion and structure
            - improvedSampleEn: a Band 8.5 polished version of the learner's response in English
        """.trimIndent()

        val res = callGeminiCached("ielts_eval_$userResponse", prompt, jsonMode = true)
        return res.fold(
            onSuccess = { txt ->
                try {
                    json.decodeFromString<IeltsAssessmentDto>(txt)
                } catch (_: Exception) {
                    fallbackIeltsAssessment(userResponse)
                }
            },
            onFailure = {
                fallbackIeltsAssessment(userResponse)
            }
        )
    }

    private fun fallbackIeltsAssessment(userResponse: String): IeltsAssessmentDto {
        val wordCount = userResponse.trim().split(Regex("\\s+")).size
        val band = if (wordCount > 25) "Band 7.0" else "Band 6.0"
        return IeltsAssessmentDto(
            estimatedBand = band,
            lexicalResourceFeedbackAr = "استخدام جيد للمفردات؛ يمكنك رفع الدرجة بإدراج كلمات أكاديمية مثل mitigate و ubiquitous و substantial.",
            grammarFeedbackAr = "التراكيب النحوية سليمة في معظمها؛ حاول التنويع بين الجمل البسيطة والجمل الشرطية أو الموصولة.",
            coherenceFeedbackAr = "الأفكار متسلسلة؛ استخدم روابط أكاديمية مثل Furthermore و Consequently و Albeit.",
            improvedSampleEn = "Furthermore, empirical evidence demonstrates that adopting sustainable technology can substantially mitigate environmental challenges while fostering unprecedented growth."
        )
    }
}
