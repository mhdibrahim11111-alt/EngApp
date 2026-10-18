package com.example.data

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object SeedTranscriptsAndDrills {

    private val json = Json { ignoreUnknownKeys = true }

    fun getInitialTranscripts(): List<TranscriptLessonEntity> {
        val itStandupLines = listOf(
            SubtitleLine(
                0, 0, 5,
                "Good morning team, let's keep our daily standup focused and accurate.",
                "صباح الخير يا فريق، دعونا نبقي اجتماعنا اليومي مركزاً ودقيقاً."
            ),
            SubtitleLine(
                1, 5, 11,
                "Yesterday we noticed a sudden spike in API latency during peak traffic.",
                "لاحظنا بالأمس ارتفاعاً مفاجئاً في زمن استجابة الـ API أثناء ذروة الزيارات."
            ),
            SubtitleLine(
                2, 11, 18,
                "The database query was the main bottleneck, so we decided to refactor it.",
                "كان استعلام قاعدة البيانات هو نقطة الاختناق الرئيسية، لذا قررنا إعادة هيكلته."
            ),
            SubtitleLine(
                3, 18, 25,
                "We also made our payment webhook idempotent to mitigate duplicate retries.",
                "كما جعلنا نقطة استقبال الدفع متطابقة الأثر للتخفيف من المحاولات المكررة."
            ),
            SubtitleLine(
                4, 25, 32,
                "This afternoon's deployment will improve overall scalability and resilience.",
                "عملية النشر بعد ظهر اليوم ستحسن القابلية للتوسع والمرونة الشاملة."
            )
        )
        val itVocab = listOf(
            KeyVocabItem("latency", "زمن الاستجابة / التأخر", "/ˈleɪtnsi/", "IT"),
            KeyVocabItem("bottleneck", "عنق الزجاجة / نقطة اختناق", "/ˈbɑːtlnek/", "IT"),
            KeyVocabItem("refactor", "يعيد هيكلة الكود", "/ˌriːˈfæktər/", "IT"),
            KeyVocabItem("idempotent", "متطابق الأثر عند التكرار", "/ˌaɪdemˈpoʊtənt/", "IT"),
            KeyVocabItem("mitigate", "يخفف من حدة", "/ˈmɪtɪɡeɪt/", "IELTS"),
            KeyVocabItem("scalability", "القابلية للتوسع", "/ˌskeɪləˈbɪləti/", "IT")
        )

        val ieltsTalkLines = listOf(
            SubtitleLine(
                0, 0, 6,
                "Smartphones have become ubiquitous in almost every modern society.",
                "أصبحت الهواتف الذكية واسعة الانتشار في كل مجتمع حديث تقريباً."
            ),
            SubtitleLine(
                1, 6, 13,
                "While digital tools act as a catalyst for learning, excessive use can be detrimental.",
                "بينما تعمل الأدوات الرقمية كمحفز للتعلم، فإن الاستخدام المفرط قد يكون ضاراً."
            ),
            SubtitleLine(
                2, 13, 20,
                "Empirical studies show a strong correlation between deep reading and vocabulary growth.",
                "تُظهر الدراسات التجريبية ارتباطاً قوياً بين القراءة العميقة ونمو المفردات."
            ),
            SubtitleLine(
                3, 20, 27,
                "Even cattle breeders in rural regions now use satellite telemetry to allocate water.",
                "حتى مربو الماشية في المناطق الريفية يستخدمون الآن القياسات الفضائية لتخصيص المياه."
            ),
            SubtitleLine(
                4, 27, 34,
                "Therefore, adopting a sensible and sustainable perspective is inevitable.",
                "لذلك، فإن تبني منظور عاقل ومستدام هو أمر حتمي."
            )
        )
        val ieltsVocab = listOf(
            KeyVocabItem("ubiquitous", "واسع الانتشار", "/juːˈbɪkwɪtəs/", "IELTS"),
            KeyVocabItem("catalyst", "محفّز للتغيير", "/ˈkætəlɪst/", "IELTS"),
            KeyVocabItem("detrimental", "ضار / سلبي الأثر", "/ˌdetrɪˈmentl/", "IELTS"),
            KeyVocabItem("empirical", "تجريبي / بالدليل", "/ɪmˈpɪrɪkl/", "IELTS"),
            KeyVocabItem("cattle", "ماشية / أبقار", "/ˈkætl/", "OXFORD"),
            KeyVocabItem("sensible", "عاقل / حكيم", "/ˈsensəbl/", "OXFORD")
        )

        val oxfordStoryLines = listOf(
            SubtitleLine(
                0, 0, 6,
                "Learning a second language requires a genuine commitment and daily habit.",
                "يتطلب تعلم لغة ثانية التزاماً صادقاً وعادة يومية."
            ),
            SubtitleLine(
                1, 6, 12,
                "At first, many speakers feel reluctant to speak because they fear mistakes.",
                "في البداية، يشعر الكثير من المتحدثين بالتردد في التحدث لأنهم يخشون الأخطاء."
            ),
            SubtitleLine(
                2, 12, 19,
                "However, overcoming that obstacle leads to a remarkable breakthrough in confidence.",
                "ومع ذلك، فإن التغلب على تلك العقبة يقود إلى طفرة مذهلة في الثقة بالنفس."
            ),
            SubtitleLine(
                3, 19, 26,
                "Ten minutes of spontaneous conversation each day is sufficient to build fluency.",
                "عشر دقائق من المحادثة التلقائية كل يوم كافية لبناء الطلاقة."
            )
        )
        val oxfordVocab = listOf(
            KeyVocabItem("genuine", "حقيقي / صادق", "/ˈdʒenjuɪn/", "OXFORD"),
            KeyVocabItem("reluctant", "متردد / غير راغب", "/rɪˈlʌktənt/", "OXFORD"),
            KeyVocabItem("obstacle", "عقبة / عائق", "/ˈɑːbstəkl/", "OXFORD"),
            KeyVocabItem("breakthrough", "طفرة / إنجاز كبير", "/ˈbreɪkθruː/", "OXFORD"),
            KeyVocabItem("spontaneous", "تلقائي / عفوي", "/spɑːnˈteɪniəs/", "OXFORD")
        )

        return listOf(
            TranscriptLessonEntity(
                titleEn = "Cloud Architecture & Daily Standup",
                titleAr = "اجتماع هندسة البرمجيات وتطوير السحابة",
                speakerOrSource = "Tech Lead Sarah • IT Industry Track",
                category = "IT",
                cefrLevel = "B2-C1",
                durationSec = 32,
                videoUrlOrTag = "https://youtu.be/linguabridge-it-standup",
                linesJson = json.encodeToString(itStandupLines),
                keyVocabJson = json.encodeToString(itVocab)
            ),
            TranscriptLessonEntity(
                titleEn = "Technology, Rural Innovation & Society",
                titleAr = "التكنولوجيا، الابتكار الريفي والمجتمع (IELTS Band 8.5)",
                speakerOrSource = "Dr. James Vance • IELTS Academic Lecture",
                category = "IELTS",
                cefrLevel = "C1",
                durationSec = 34,
                videoUrlOrTag = "https://youtu.be/linguabridge-ielts-lecture",
                linesJson = json.encodeToString(ieltsTalkLines),
                keyVocabJson = json.encodeToString(ieltsVocab)
            ),
            TranscriptLessonEntity(
                titleEn = "Breaking the Fear of Speaking English",
                titleAr = "كسر حاجز الخوف من التحدث بالإنجليزية",
                speakerOrSource = "Noor Coaching • Oxford Core Fluency",
                category = "OXFORD",
                cefrLevel = "B1-B2",
                durationSec = 26,
                videoUrlOrTag = "https://youtu.be/linguabridge-oxford-fluency",
                linesJson = json.encodeToString(oxfordStoryLines),
                keyVocabJson = json.encodeToString(oxfordVocab)
            )
        )
    }

    val minimalPairDrills: List<MinimalPairDrill> = listOf(
        MinimalPairDrill(
            id = "pb_1",
            category = PhonemeChallengeCategory.P_B,
            wordA = "Pen",
            ipaA = "/pen/",
            arabicA = "قلم حبر (بدفعة هواء قوية)",
            wordB = "Ben",
            ipaB = "/ben/",
            arabicB = "اسم عَلم (بصوت مجهور)",
            sentenceEn = "Please put the purple pen in the big bag.",
            sentenceAr = "من فضلك ضع القلم البنفسجي في الحقيبة الكبيرة."
        ),
        MinimalPairDrill(
            id = "pb_2",
            category = PhonemeChallengeCategory.P_B,
            wordA = "Park",
            ipaA = "/pɑːrk/",
            arabicA = "حديقة عامة / يركن السيارة",
            wordB = "Bark",
            ipaB = "/bɑːrk/",
            arabicB = "نباح الكلب / لحاء الشجرة",
            sentenceEn = "People parked near the peaceful public park.",
            sentenceAr = "ركن الناس سياراتهم قرب الحديقة العامة الهادئة."
        ),
        MinimalPairDrill(
            id = "vf_1",
            category = PhonemeChallengeCategory.V_F,
            wordA = "Very",
            ipaA = "/ˈveri/",
            arabicA = "جداً (اهتزاز الشفة السفلية)",
            wordB = "Ferry",
            ipaB = "/ˈferi/",
            arabicB = "عبّارة بحرية (هواء فقط)",
            sentenceEn = "The view from the five ferries is very vivid.",
            sentenceAr = "المنظر من العبّارات الخمس واضح وحيوي جداً."
        ),
        MinimalPairDrill(
            id = "vf_2",
            category = PhonemeChallengeCategory.V_F,
            wordA = "Van",
            ipaA = "/væn/",
            arabicA = "شاحنة صغيرة",
            wordB = "Fan",
            ipaB = "/fæn/",
            arabicB = "مروحة / مشجع",
            sentenceEn = "Victor drove a fast van down the valley.",
            sentenceAr = "قاد فيكتور شاحنة سريعة أسفل الوادي."
        ),
        MinimalPairDrill(
            id = "sv_1",
            category = PhonemeChallengeCategory.SHORT_VOWELS,
            wordA = "Ship",
            ipaA = "/ʃɪp/",
            arabicA = "سفينة (كسرة قصيرة خاطفة)",
            wordB = "Sheep",
            ipaB = "/ʃiːp/",
            arabicB = "خروف (ياء ممدودة طويلة)",
            sentenceEn = "The big ship carried sixty white sheep.",
            sentenceAr = "حملت السفينة الكبيرة ستين خروفاً أبيض."
        ),
        MinimalPairDrill(
            id = "sv_2",
            category = PhonemeChallengeCategory.SHORT_VOWELS,
            wordA = "Sit",
            ipaA = "/sɪt/",
            arabicA = "يجلس (قصيرة)",
            wordB = "Seat",
            ipaB = "/siːt/",
            arabicB = "مقعد (ممدودة)",
            sentenceEn = "Please sit in this comfortable window seat.",
            sentenceAr = "من فضلك اجلس في هذا المقعد المريح بجانب النافذة."
        ),
        MinimalPairDrill(
            id = "th_1",
            category = PhonemeChallengeCategory.TH_SOUNDS,
            wordA = "Think",
            ipaA = "/θɪŋk/",
            arabicA = "يفكر (ثاء بطرف اللسان)",
            wordB = "Sink",
            ipaB = "/sɪŋk/",
            arabicB = "يغرق / حوض (سين)",
            sentenceEn = "I think three thousand things are worth thanking.",
            sentenceAr = "أعتقد أن ثلاثة آلاف شيء تستحق الشكر."
        ),
        MinimalPairDrill(
            id = "ng_1",
            category = PhonemeChallengeCategory.NG_SOUND,
            wordA = "Sing",
            ipaA = "/sɪŋ/",
            arabicA = "يغني (غنة أنفية بدون ج)",
            wordB = "Sink",
            ipaB = "/sɪŋk/",
            arabicB = "حوض (ينتهي بكاف)",
            sentenceEn = "Speaking and reading every morning brings strong progress.",
            sentenceAr = "التحدث والقراءة كل صباح يجلبان تقدماً قوياً."
        ),
        MinimalPairDrill(
            id = "cl_1",
            category = PhonemeChallengeCategory.CLUSTERS,
            wordA = "Street",
            ipaA = "/striːt/",
            arabicA = "شارع (ابدأ بالسين مباشرة بدون إِ)",
            wordB = "Spring",
            ipaB = "/sprɪŋ/",
            arabicB = "فصل الربيع (3 سواكن متتالية)",
            sentenceEn = "Strong streams flow down the straight street in spring.",
            sentenceAr = "تتدفق الجداول القوية في الشارع المستقيم في فصل الربيع."
        ),
        MinimalPairDrill(
            id = "fn_1",
            category = PhonemeChallengeCategory.FINAL_SOUNDS,
            wordA = "Worked",
            ipaA = "/wɜːrkt/",
            arabicA = "عملَ (تنطق الـ ed كحرف /t/ خفيف)",
            wordB = "Played",
            ipaB = "/pleɪd/",
            arabicB = "لعبَ (تنطق الـ ed كحرف /d/)",
            sentenceEn = "She worked hard, fixed the bug, and deployed the update.",
            sentenceAr = "عملت بجد، وأصلحت الخطأ البرمجي، ونشرت التحديث."
        )
    )

    val techArticles: List<TechArticle> = listOf(
        TechArticle(
            id = "art_1",
            titleEn = "Designing High-Resilience Microservices",
            titleAr = "تصميم الخدمات المصغرة عالية المرونة",
            level = "B2-C1",
            paragraphsEn = listOf(
                "In modern cloud architecture, reducing latency and eliminating database bottlenecks are crucial for scalability.",
                "When an external service fails, asynchronous queues and idempotent endpoints mitigate the risk of data corruption during deployment."
            ),
            paragraphsAr = listOf(
                "في معمارية السحابة الحديثة، يُعد تقليل زمن الاستجابة وإزالة نقاط اختناق قواعد البيانات أمراً حاسماً للقابلية للتوسع.",
                "عندما تتعطل خدمة خارجية، فإن الطوابير غير المتزامنة ونقاط الاتصال متطابقة الأثر تخفف من خطر تلف البيانات أثناء النشر."
            ),
            interviewQuestionEn = "How do you identify and resolve a performance bottleneck in a backend API?",
            interviewTipAr = "استخدم مصطلحات مثل: telemetry, latency, database indexing, caching payload, و horizontal scalability."
        ),
        TechArticle(
            id = "art_2",
            titleEn = "Zero-Trust Security: Authentication vs Authorization",
            titleAr = "الأمن الصفري: الفرق بين المصادقة والتفويض",
            level = "B2",
            paragraphsEn = listOf(
                "Every request passing through API middleware must first complete authentication to verify user identity.",
                "Next, fine-grained authorization ensures data integrity and patches potential security vulnerabilities across ephemeral containers."
            ),
            paragraphsAr = listOf(
                "كل طلب يمر عبر البرمجيات الوسيطة للـ API يجب أن يكمل أولاً المصادقة للتحقق من هوية المستخدم.",
                "بعد ذلك، يضمن التفويض الدقيق سلامة البيانات ويسد الثغرات الأمنية المحتملة عبر الحاويات المؤقتة."
            ),
            interviewQuestionEn = "Can you explain the difference between Authentication and Authorization with a real example?",
            interviewTipAr = "وضح أن Authentication هي (من أنت؟ - الهوية)، بينما Authorization هي (ماذا يحق لك فعله؟ - الصلاحيات)."
        )
    )

    val weeklyLeaderboard: List<LeaderboardEntry> = listOf(
        LeaderboardEntry(1, "سارة المنصور (الرياض)", "🇸🇦", 1480, "🔥 19 يوم"),
        LeaderboardEntry(2, "عمر الشافعي (القاهرة)", "🇪🇬", 1320, "⚡ IELTS 8.0"),
        LeaderboardEntry(3, "أنت (المتعلم النشط)", "🌟", 420, "🛡️ درع الحماسة", isCurrentUser = true),
        LeaderboardEntry(4, "ليلى الكيلاني (عمّان)", "🇯🇴", 390, "💻 IT Pro"),
        LeaderboardEntry(5, "يوسف بن علي (دبي)", "🇦🇪", 340, "📚 Oxford")
    )
}
