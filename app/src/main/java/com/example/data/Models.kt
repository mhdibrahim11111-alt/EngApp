package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

enum class DeckType(
    val id: String,
    val titleEn: String,
    val titleAr: String,
    val totalCatalogTarget: Int
) {
    IELTS("IELTS", "IELTS Core", "مفردات الآيلتس الأكاديمية", 3575),
    IT("IT", "IT Industry", "مصطلحات البرمجة والتقنية", 1700),
    OXFORD("OXFORD", "Oxford 5000", "أكسفورد 5000 الأساسي", 5836);

    companion object {
        fun fromString(raw: String): DeckType {
            val upper = raw.trim().uppercase()
            return entries.find { it.id == upper || upper.contains(it.id) } ?: OXFORD
        }
    }
}

enum class MasteryLevel(val labelEn: String, val labelAr: String) {
    NEW("New", "جديدة"),
    LEARNING("Learning", "قيد التعلّم"),
    REVIEW("Review", "مراجعة"),
    MASTERED("Mastered", "مُتقنة")
}

enum class SrsGrade(val quality: Int, val labelEn: String, val labelAr: String, val intervalHintAr: String) {
    AGAIN(1, "Again", "أعد مجدداً", "< 10 دقائق"),
    HARD(3, "Hard", "صعبة", "يوم واحد"),
    GOOD(4, "Good", "جيدة", "3 أيام"),
    EASY(5, "Easy", "سهلة جداً", "7+ أيام")
}

enum class VocabSortOrder(val labelEn: String, val labelAr: String) {
    BY_DATE_DESC("By date ↓", "حسب التاريخ ↓"),
    ALPHABETICAL("A → Z", "أبجدياً A-Z"),
    CEFR_LEVEL("CEFR Level", "حسب المستوى")
}

enum class VocabFilterTab(val labelEn: String) {
    ALL("All"),
    MASTERED("Mastered"),
    FAVORITES("Favorites")
}

@Entity(tableName = "words")
@Serializable
data class WordEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val word: String,
    val syllabified: String = word,
    val partOfSpeech: String, // n., v., adj., adv.
    val secondaryPos: String = "", // e.g. "n." when primary is "v." (like abandon / supplement / jumble)
    val secondaryArabic: String = "",
    val cefrLevel: String,    // A1, A2, B1, B2, C1
    val deck: String,         // OXFORD, IELTS, IT
    val arabicTranslation: String,
    val webArabicSynonyms: String = "",
    val englishDefinition: String,
    val exampleEn1: String,
    val exampleAr1: String,
    val exampleEn2: String,
    val exampleAr2: String,
    val ipaUs: String,
    val ipaGb: String = ipaUs,
    val synonyms: String,
    val pluralOrGrammarNote: String = "",
    val etymologyAr: String = "",
    val arabicSpeakerTipMsa: String = "",
    val arabicSpeakerTipEgyptian: String = "",
    val masteryLevel: String = MasteryLevel.NEW.name,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val repetitions: Int = 0,
    val easeFactor: Float = 2.5f,
    val intervalDays: Int = 0,
    val nextReviewDate: Long = 0L,
    val correctCount: Int = 0,
    val mistakeCount: Int = 0,
    val isWeakWord: Boolean = false
)

@Entity(tableName = "user_stats")
data class UserStatsEntity(
    @PrimaryKey val id: Int = 1,
    val onboardingCompleted: Boolean = false,
    val learningGoal: String = "GENERAL", // IELTS, IT, GENERAL
    val cefrLevel: String = "B1",
    val dailyGoalMinutes: Int = 10,       // 5, 10, 20
    val uiLanguage: String = "AR",        // AR (default RTL) or EN (LTR)
    val arabicDialect: String = "MSA",    // MSA or EGYPTIAN
    val darkMode: Boolean = true,
    val xp: Int = 420,
    val level: Int = 3,
    val streakDays: Int = 5,
    val streakFreezeAvailable: Boolean = true,
    val hearts: Int = 5,
    val maxHearts: Int = 5,
    val minutesStudiedToday: Int = 7,
    val totalMinutesStudied: Int = 185,
    val activeDecksCsv: String = "OXFORD,IELTS,IT",
    val totalAnswers: Int = 48,
    val correctAnswers: Int = 41,
    val streakCalendarCsv: String = "1,1,1,0,1,1,1,1,1"
)

@Serializable
data class SubtitleLine(
    val index: Int,
    val startSec: Int,
    val endSec: Int,
    val textEn: String,
    val textAr: String
)

@Serializable
data class KeyVocabItem(
    val word: String,
    val arabic: String,
    val ipa: String,
    val deck: String = "OXFORD"
)

@Entity(tableName = "transcripts")
data class TranscriptLessonEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val titleEn: String,
    val titleAr: String,
    val speakerOrSource: String,
    val category: String, // OXFORD, IELTS, IT, CUSTOM
    val cefrLevel: String,
    val durationSec: Int,
    val videoUrlOrTag: String,
    val linesJson: String,
    val keyVocabJson: String
)

@Serializable
data class WordPhonemeScore(
    val word: String,
    val score: Int,
    val ipaTarget: String,
    val issueAr: String = ""
)

@Serializable
data class PronunciationEvaluation(
    val overallScore: Int,
    val words: List<WordPhonemeScore>,
    val mouthTipMsa: String,
    val mouthTipEgyptian: String,
    val focusSoundLabel: String
)

@Entity(tableName = "pronunciation_attempts")
data class PronunciationAttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val targetText: String,
    val spokenText: String,
    val focusCategory: String,
    val overallScore: Int,
    val evaluationJson: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "ai_cache")
data class AiCacheEntity(
    @PrimaryKey val cacheKey: String,
    val responseJson: String,
    val createdAt: Long = System.currentTimeMillis()
)

enum class PhonemeChallengeCategory(
    val id: String,
    val titleAr: String,
    val titleEn: String,
    val symbols: String,
    val tipMsa: String,
    val tipEgyptian: String
) {
    P_B(
        "P_B",
        "الفرق بين P و B",
        "P vs B Voicing",
        "/p/ vs /b/",
        "صوت /p/ مهموس يخرج مع دفعة هواء قوية من الشفتين بدون اهتزاز الأحبال الصوتية، عكس /b/.",
        "حرف الـ P بيطلع معاه هوا جامد من شفايفك كأنك بتطفي شمعة، إنما الـ B صوته تقيل من الزور."
    ),
    V_F(
        "V_F",
        "الفرق بين V و F",
        "V vs F Vibration",
        "/v/ vs /f/",
        "ضع أسنانك العلوية على شفتك السفلية؛ مع /v/ أشغل الأحبال الصوتية ليحدث اهتزاز واضح.",
        "حط سنانك اللي فوق على شفتك اللي تحت واعمِل زنة في زورك عشان تطلع V مظبوطة مش F."
    ),
    SHORT_VOWELS(
        "SHORT_VOWELS",
        "الحركات القصيرة (ɪ / e / æ / ʌ)",
        "Short Vowels",
        "/ɪ/ /e/ /æ/ /ʌ/",
        "المتحدث العربي يخلط بين كسرة ship القصيرة وياء sheep الممدودة، أو بين /e/ و /ɪ/.",
        "خلي بالك من الكسرة الخفيفة زي sit والفتحة المرققة زي bed عشان المعنى بيتغير خالص."
    ),
    TH_SOUNDS(
        "TH",
        "أصوات الثاء والذال (θ / ð)",
        "TH Sounds",
        "/θ/ & /ð/",
        "أخرج طرف اللسان قليلاً بين الأسنان كما في حرفي (ث) و(ذ) بالعربية ولا تستبدلهما بـ s أو z.",
        "طلع طرف لسانك بين سنانك زي حرف الثاء والذال في العربي، بلاش تنطقها سين أو زاي!"
    ),
    NG_SOUND(
        "NG",
        "صوت الأنف النهائي (ŋ)",
        "Velar Nasal -ing",
        "/ŋ/",
        "في نهاية الكلمات المنتهية بـ -ing، يخرج الصوت من مؤخرة اللسان والأنف بدون نطق حرف g صريح.",
        "لما الكلمة تخلص بـ ing ما تنطقش حرف الـ ج في الآخر، خلي الصوت يطلع من الأنف بس."
    ),
    CLUSTERS(
        "CLUSTERS",
        "تتابع السواكن (Consonant Clusters)",
        "Consonant Clusters",
        "str- / spr- / -kts",
        "لا تضف همزة مكسورة قبل الكلمات المبدوءة بساكنين مثل street (لا تقل إستريت) أو داخلها.",
        "أوعى تزود همزة في الأول وتقول (إستريت) بدل street، ادخل على حرف الـ s على طول!"
    ),
    FINAL_SOUNDS(
        "FINAL_SOUNDS",
        "نهايات الكلمات (-ed / -s)",
        "Final Consonants",
        "-ed (/t/ /d/ /ɪd/)",
        "لاحقة الماضي -ed تُنطق /t/ بعد المهموس مثل worked وتُنطق /d/ بعد المجهور مثل played.",
        "مش كل الكلمات اللي آخرها ed بنقول فيها (إد)، كلمة زي worked آخرها صوت ت خفيف: وركت."
    )
}

data class MinimalPairDrill(
    val id: String,
    val category: PhonemeChallengeCategory,
    val wordA: String,
    val ipaA: String,
    val arabicA: String,
    val wordB: String,
    val ipaB: String,
    val arabicB: String,
    val sentenceEn: String,
    val sentenceAr: String
)

enum class ExerciseType(val labelAr: String, val labelEn: String) {
    MC_EN_TO_AR("اختر المعنى العربي", "Multiple Choice (EN → AR)"),
    MC_AR_TO_EN("اختر الكلمة الإنجليزية", "Multiple Choice (AR → EN)"),
    LISTENING("استمع واختر الكلمة", "Listening Comprehension"),
    SPELLING("اكتب الكلمة بالإنجليزية", "Spelling & Typing"),
    FILL_BLANK("أكمل الفراغ في الجملة", "Fill in the Blank"),
    MATCH_PAIRS("طابق الكلمات بمعانيها", "Match Pairs"),
    WORD_ORDER("رتّب الكلمات لتكوين جملة", "Sentence Word Order"),
    SPEAKING("تحدّث وانطق الكلمة", "Speaking Check")
}

data class ExerciseItem(
    val type: ExerciseType,
    val targetWord: WordEntity,
    val promptText: String,
    val subPromptText: String = "",
    val options: List<String> = emptyList(),
    val correctAnswer: String,
    val matchPairs: List<Pair<String, String>> = emptyList(),
    val scrambledWords: List<String> = emptyList()
)

enum class ChatScenario(
    val id: String,
    val titleAr: String,
    val titleEn: String,
    val subtitleAr: String,
    val systemRolePrompt: String,
    val starterEn: String,
    val starterAr: String
) {
    IT_INTERVIEW(
        "IT_INTERVIEW",
        "مقابلة عمل تقنية (IT)",
        "IT Job Interview",
        "تدرب على أسئلة هندسة البرمجيات والـ System Design",
        "You are a friendly Senior Engineering Manager interviewing an Arabic-speaking candidate for a Software/IT role.",
        "Welcome! Could you tell me about a recent technical challenge you solved and the architecture you used?",
        "أهلاً بك! هل يمكنك إخباري عن تحدٍّ تقني حللته مؤخراً والمعمارية التي استخدمتها؟"
    ),
    IELTS_SPEAKING(
        "IELTS_SPEAKING",
        "اختبار التحدث IELTS (الأجزاء 1/2/3)",
        "IELTS Speaking Part 1/2/3",
        "محاكاة رسمية مع تقييم Band Score ومفردات أكاديمية",
        "You are a certified IELTS Speaking Examiner conducting Parts 1, 2, and 3.",
        "Good morning. Let's begin with Part 1: How has technology changed the way people study in your country?",
        "صباح الخير. لنبدأ بالجزء الأول: كيف غيّرت التكنولوجيا طريقة دراسة الناس في بلدك؟"
    ),
    DAILY_LIFE(
        "DAILY_LIFE",
        "الحياة اليومية والسفر",
        "Daily Life & Travel",
        "محادثات المطار، المطاعم، والتواصل اليومي بطلاقة",
        "You are a warm native English speaker chatting in daily life scenarios (coffee shop, airport, neighborhood).",
        "Hey there! It's great to meet you. What do you enjoy doing on your weekends?",
        "أهلاً بك! سررت بلقائك. ما الذي تستمتع بفعله في عطلة نهاية الأسبوع؟"
    ),
    CUSTOMER_SUPPORT(
        "CUSTOMER_SUPPORT",
        "دعم العملاء والاجتماعات",
        "Customer Support & Work",
        "التعامل مع العملاء وحل المشكلات المهنية بلباقة",
        "You are a client talking to a technical support / account specialist.",
        "Hi, we noticed higher latency on our cloud dashboard after the latest deployment. Can you help us investigate?",
        "مرحباً، لاحظنا بطئاً في لوحة التحكم السحابية بعد التحديث الأخير. هل يمكنك مساعدتنا في الفحص؟"
    )
}

@Serializable
data class ChatMessage(
    val id: String,
    val isUser: Boolean,
    val textEn: String,
    val grammarCorrection: String = "",
    val arabicExplanation: String = "",
    val vocabularySuggestions: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

data class TechArticle(
    val id: String,
    val titleEn: String,
    val titleAr: String,
    val level: String,
    val paragraphsEn: List<String>,
    val paragraphsAr: List<String>,
    val interviewQuestionEn: String,
    val interviewTipAr: String
)

data class LeaderboardEntry(
    val rank: Int,
    val name: String,
    val countryFlag: String,
    val xp: Int,
    val badge: String,
    val isCurrentUser: Boolean = false
)
