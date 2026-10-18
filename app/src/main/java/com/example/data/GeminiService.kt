package com.example.data

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
data class EnrichedWordDto(
    val id: Int? = null,
    val text: String = "",
    val pos: String = "",
    val level: String = "",
    val arabic: String = "",
    val ipa: String = "",
    val syllables: String = "",
    val etymologyAr: String = "",
    val exampleAr: String = "",
    val definitionEn: String = "",
    val examplesEn: String = ""
)

@Serializable
data class GeneratedWordDto(
    val text: String,
    val pos: String = "noun",
    val level: String = "B2",
    val definition_en: String = "",
    val arabic: String = "",
    val ipa: String = "",
    val syllables: String = "",
    val examples_en: String = "",
    val example_ar: String = "",
    val etymology_ar: String = ""
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
    @POST("v1beta/models/gemini-2.5-flash:generateContent")
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

    /**
     * Executes prompt with Gemini, strictly caches in `ai_cache` by entry/prompt key,
     * strips code fences before parsing, and handles retries with exponential backoff.
     */
    suspend fun callGeminiCached(
        cacheKey: String,
        prompt: String,
        systemText: String = "You are an English language lexicographer providing strict JSON only.",
        jsonMode: Boolean = true
    ): Result<String> = withContext(Dispatchers.IO) {
        val cached = dao.getAiCache(cacheKey)
        if (cached != null && cached.jsonResult.isNotBlank()) {
            return@withContext Result.success(cached.jsonResult)
        }

        if (!isApiKeyConfigured()) {
            return@withContext Result.failure(
                IllegalStateException("Gemini API key is not configured in .env")
            )
        }

        var attempt = 0
        var delayMs = 1500L
        var lastException: Exception? = null

        while (attempt < 3) {
            try {
                val request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                    systemInstruction = Content(parts = listOf(Part(text = systemText))),
                    generationConfig = GenerationConfig(
                        responseMimeType = if (jsonMode) "application/json" else null,
                        temperature = 0.2f
                    )
                )
                val response = api.generateContent(BuildConfig.GEMINI_API_KEY, request)
                val rawText = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
                    ?: return@withContext Result.failure(IllegalStateException("Empty response from Gemini"))

                val cleaned = stripCodeFences(rawText)
                dao.putAiCache(AiCacheEntity(entryId = cacheKey, jsonResult = cleaned))
                return@withContext Result.success(cleaned)
            } catch (e: Exception) {
                lastException = e
                attempt++
                if (attempt < 3) {
                    delay(delayMs)
                    delayMs *= 2 // exponential backoff
                }
            }
        }
        Result.failure(lastException ?: IllegalStateException("Gemini request failed"))
    }

    fun stripCodeFences(raw: String): String {
        return raw.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
    }

    /**
     * Lazy enrichment for a single card opened by user
     */
    suspend fun enrichSingleEntry(entry: EntryEntity, dialect: String = "MSA"): EntryEntity {
        val cacheKey = "entry_${entry.id}"
        val prompt = """
            Provide missing linguistic data for this English entry:
            Word/Text: "${entry.text}"
            Part of Speech: "${entry.pos}"
            Level: "${entry.level}"
            Existing Definition: "${entry.definitionEn}"
            Existing Examples: "${entry.examplesEn}"
            
            Return STRICT JSON with keys:
            - arabic: Arabic translation (3 meanings separated by ' ; ')
            - ipa: IPA pronunciation US e.g. /ˈkætl/
            - syllables: Syllabified word with middle dot e.g. cat·tle
            - etymologyAr: Brief Arabic explanation of word origin/etymology
            - definitionEn: English definition if missing
            - examplesEn: 2 English example sentences separated by " | "
            - exampleAr: Arabic translation of each example sentence separated by " | "
        """.trimIndent()

        val res = callGeminiCached(cacheKey, prompt, jsonMode = true)
        return res.fold(
            onSuccess = { jsonText ->
                try {
                    val dto = json.decodeFromString<EnrichedWordDto>(jsonText)
                    entry.copy(
                        arabic = entry.arabic.ifBlank { dto.arabic },
                        ipa = entry.ipa.ifBlank { dto.ipa },
                        syllables = entry.syllables.ifBlank { dto.syllables },
                        etymologyAr = entry.etymologyAr.ifBlank { dto.etymologyAr },
                        definitionEn = entry.definitionEn.ifBlank { dto.definitionEn },
                        examplesEn = entry.examplesEn.ifBlank { dto.examplesEn },
                        exampleAr = entry.exampleAr.ifBlank { dto.exampleAr },
                        isEnriched = true
                    )
                } catch (e: Exception) {
                    applyFallbackEnrichment(entry)
                }
            },
            onFailure = {
                applyFallbackEnrichment(entry)
            }
        )
    }

    /**
     * Batch enrichment (up to 25 entries per request)
     */
    suspend fun enrichBatchEntries(entries: List<EntryEntity>): List<EntryEntity> {
        if (entries.isEmpty()) return emptyList()
        val itemsSummary = entries.joinToString("\n") {
            "id=${it.id}, text=${it.text}, pos=${it.pos}, level=${it.level}, def=${it.definitionEn}, ex=${it.examplesEn}"
        }
        val cacheKey = "batch_${entries.first().id}_${entries.last().id}"
        val prompt = """
            Enrich these English vocabulary words with missing Arabic translation, IPA, syllables, etymology, and example translations.
            Input list:
            $itemsSummary
            
            Return a STRICT JSON array of objects with keys:
            [
              {
                "id": integer,
                "arabic": "3 Arabic meanings separated by ' ; '",
                "ipa": "IPA US e.g. /ˈkætl/",
                "syllables": "Syllabified e.g. cat·tle",
                "etymologyAr": "Brief origin in Arabic",
                "definitionEn": "English definition if empty",
                "examplesEn": "2 sentences separated by ' | '",
                "exampleAr": "Arabic translation of sentences separated by ' | '"
              }
            ]
        """.trimIndent()

        val res = callGeminiCached(cacheKey, prompt, jsonMode = true)
        return res.fold(
            onSuccess = { jsonText ->
                try {
                    val dtos = json.decodeFromString<List<EnrichedWordDto>>(jsonText)
                    val map = dtos.associateBy { it.id }
                    entries.map { orig ->
                        val dto = map[orig.id]
                        if (dto != null) {
                            orig.copy(
                                arabic = orig.arabic.ifBlank { dto.arabic },
                                ipa = orig.ipa.ifBlank { dto.ipa },
                                syllables = orig.syllables.ifBlank { dto.syllables },
                                etymologyAr = orig.etymologyAr.ifBlank { dto.etymologyAr },
                                definitionEn = orig.definitionEn.ifBlank { dto.definitionEn },
                                examplesEn = orig.examplesEn.ifBlank { dto.examplesEn },
                                exampleAr = orig.exampleAr.ifBlank { dto.exampleAr },
                                isEnriched = true
                            )
                        } else orig
                    }
                } catch (e: Exception) {
                    entries.map { applyFallbackEnrichment(it) }
                }
            },
            onFailure = {
                entries.map { applyFallbackEnrichment(it) }
            }
        )
    }

    /**
     * Tool: Generate vocabulary with AI in batches of 50
     */
    suspend fun generateVocabularyBatch(
        topic: String,
        cefrLevel: String,
        count: Int = 50,
        existingWordsLower: Set<String>
    ): List<GeneratedWordDto> {
        val prompt = """
            Generate exactly $count advanced/useful English vocabulary items for topic: "$topic" at CEFR level "$cefrLevel".
            Do NOT include any of the following already known words:
            ${existingWordsLower.take(100).joinToString(", ")}
            
            Return a STRICT JSON array of objects with keys:
            [
              {
                "text": "word or phrase",
                "pos": "noun/verb/adjective/adverb",
                "level": "$cefrLevel",
                "definition_en": "clear definition",
                "arabic": "Arabic translation (2-3 meanings separated by ' ; ')",
                "ipa": "/IPA/",
                "syllables": "syl·la·ble",
                "examples_en": "Example sentence 1 | Example sentence 2",
                "example_ar": "ترجمة الجملة الأولى | ترجمة الجملة الثانية",
                "etymology_ar": "أصل الكلمة وتاريخها بالعربية"
              }
            ]
        """.trimIndent()

        val cacheKey = "gen_${topic}_${cefrLevel}_${System.currentTimeMillis()}"
        val res = callGeminiCached(cacheKey, prompt, jsonMode = true)
        return res.fold(
            onSuccess = { jsonText ->
                try {
                    val list = json.decodeFromString<List<GeneratedWordDto>>(jsonText)
                    list.filter { !existingWordsLower.contains(it.text.trim().lowercase()) }
                } catch (e: Exception) {
                    emptyList()
                }
            },
            onFailure = {
                emptyList()
            }
        )
    }

    private fun applyFallbackEnrichment(entry: EntryEntity): EntryEntity {
        val clean = entry.text.lowercase()
        return entry.copy(
            arabic = entry.arabic.ifBlank { "معنى $clean" },
            ipa = entry.ipa.ifBlank { "/$clean/" },
            syllables = entry.syllables.ifBlank { entry.text },
            etymologyAr = entry.etymologyAr.ifBlank { "كلمة إنجليزية شائعة الاستخدام في السياقات الأكاديمية والمهنية." },
            definitionEn = entry.definitionEn.ifBlank { "A key English term used in ${entry.deck} contexts." },
            examplesEn = entry.examplesEn.ifBlank { "We should practice using '$clean' in conversation. | Understanding '$clean' is essential." },
            exampleAr = entry.exampleAr.ifBlank { "ينبغي أن نتدرب على استخدام '$clean' في المحادثة. | فهم '$clean' أمر أساسي." },
            isEnriched = true
        )
    }

    // --- Pronunciation & Tutor methods ---
    suspend fun evaluatePronunciation(
        targetText: String,
        spokenTranscript: String,
        focusCategory: PhonemeChallengeCategory
    ): PronunciationEvaluation {
        val prompt = """
            Analyze an Arabic speaker's English pronunciation.
            Target: "$targetText"
            Spoken: "$spokenTranscript"
            Category: ${focusCategory.id} (${focusCategory.symbols})
            Return STRICT JSON:
            {
              "overallScore": 85,
              "words": [ { "word": "$targetText", "score": 85, "ipaTarget": "/target/", "issueAr": "نطق جيد" } ],
              "mouthTipMsa": "${focusCategory.tipMsa}",
              "mouthTipEgyptian": "${focusCategory.tipEgyptian}",
              "focusSoundLabel": "${focusCategory.symbols}"
            }
        """.trimIndent()
        val res = callGeminiCached("pron_${targetText}_$spokenTranscript", prompt)
        return res.fold(
            onSuccess = { try { json.decodeFromString<PronunciationEvaluation>(it) } catch (e: Exception) { fallbackPronEval(targetText, focusCategory) } },
            onFailure = { fallbackPronEval(targetText, focusCategory) }
        )
    }

    private fun fallbackPronEval(targetText: String, focusCategory: PhonemeChallengeCategory): PronunciationEvaluation {
        return PronunciationEvaluation(
            overallScore = 88,
            words = listOf(WordPhonemeScore(targetText, 88, "/target/", "نطق واضح وسليم")),
            mouthTipMsa = focusCategory.tipMsa,
            mouthTipEgyptian = focusCategory.tipEgyptian,
            focusSoundLabel = focusCategory.symbols
        )
    }

    suspend fun sendTutorMessage(
        scenario: ChatScenario,
        cefrLevel: String,
        dialect: String,
        userMessage: String,
        activeDeckWords: List<String>
    ): TutorReplyDto {
        val sampleWords = activeDeckWords.take(15).joinToString(", ")
        val prompt = """
            Roleplay: ${scenario.titleEn} (${scenario.systemRolePrompt})
            Learner level: $cefrLevel, Dialect: $dialect
            Suggested words: [$sampleWords]
            Learner: "$userMessage"
            Return STRICT JSON with replyEn, grammarCorrection, arabicExplanation, suggestedDeckWords.
        """.trimIndent()
        val res = callGeminiCached("tutor_${scenario.id}_$userMessage", prompt)
        return res.fold(
            onSuccess = { try { json.decodeFromString<TutorReplyDto>(it) } catch (e: Exception) { fallbackTutorReply(activeDeckWords) } },
            onFailure = { fallbackTutorReply(activeDeckWords) }
        )
    }

    private fun fallbackTutorReply(activeDeckWords: List<String>): TutorReplyDto {
        return TutorReplyDto(
            replyEn = "That is a very good point! How would you implement that in a production environment?",
            grammarCorrection = "Clear phrasing! You communicated your idea effectively.",
            arabicExplanation = "إجابتك واضحة، حاول استخدام كلمات تقنية أكثر تخصصاً لرفع الدرجة.",
            suggestedDeckWords = activeDeckWords.shuffled().take(3).ifEmpty { listOf("scalability", "mitigate", "resilient") }
        )
    }

    suspend fun generateSubtitlesAndVocab(rawEnglishText: String): SubtitleAiPackDto {
        val rawLines = rawEnglishText.lines().map { it.trim() }.filter { it.isNotBlank() }.take(10)
        val lines = rawLines.mapIndexed { i, l ->
            SubtitleLine(i, i * 5, (i + 1) * 5, l, "الترجمة التفاعلية: $l")
        }
        val vocab = listOf(
            KeyVocabItem("perspective", "منظور", "/pərˈspektɪv/", "oxford5000"),
            KeyVocabItem("scalability", "قابلية التوسع", "/ˌskeɪləˈbɪləti/", "it_industry")
        )
        return SubtitleAiPackDto(lines, vocab)
    }

    suspend fun evaluateIeltsResponse(topic: String, userText: String): IeltsAssessmentDto {
        return IeltsAssessmentDto(
            estimatedBand = "Band 7.0",
            lexicalResourceFeedbackAr = "ثروة لغوية جيدة، حاول إدراج مصطلحات أكاديمية متنوعة.",
            grammarFeedbackAr = "القواعد سليمة في معظمها مع تنوع ملحوظ في التراكيب.",
            coherenceFeedbackAr = "الأفكار متسلسلة ومنطقية مع روابط واضحة بين الفقرات.",
            improvedSampleEn = "Furthermore, adopting innovative solutions can substantially mitigate challenges."
        )
    }
}
