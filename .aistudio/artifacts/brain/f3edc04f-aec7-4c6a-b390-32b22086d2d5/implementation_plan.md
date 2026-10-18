# LinguaBridge — Arabic-First AI English Learning App

LinguaBridge is a native Android application crafted specifically for Arabic speakers to master general, academic (IELTS), and technical (IT industry) English. It fuses gamified learning paths, SM-2 spaced repetition flashcards, dual-subtitle interactive transcripts with audio playback, real-time AI pronunciation coaching focused on Arabic phoneme challenges, and an adaptive AI conversation partner powered by the Gemini API.

## User Review & Critical Decisions

> [!IMPORTANT]
> Please review the confirmed architectural and product decisions below before we begin building the application.

- **Confirmed Decision 1 — On-Device Room Persistence**: All vocabulary decks (Oxford 5000, IELTS Core, IT Industry), SM-2 spaced repetition intervals, user XP/streaks, weak words, and AI response caches are stored locally on the device using Android Room SQLite database.
- **Confirmed Decision 2 — Interactive Transcripts with Audio Playback**: The Watch & Learn experience provides pre-loaded rich interactive transcripts (plus custom SRT/transcript import and YouTube context simulation) paired with synchronized Text-to-Speech audio playback, sentence looping, variable speed, auto-pause, and one-tap word lookup.
- **Confirmed Decision 3 — Arabic Dialect Support**: The app defaults to an Arabic-first RTL interface using Modern Standard Arabic (الفصحى) for explanations, with a global and contextual toggle for Egyptian Arabic (العامية المصرية) hints and pronunciation tips.
- **Native Android Adaptation**: Built natively with Kotlin and Jetpack Compose (Material 3) for smooth mobile performance, native Android TextToSpeech, SpeechRecognizer / AudioRecord integration, and offline-first database access.

---

## 1. Overview & Core Concept

- **What It Does**: Provides an end-to-end English mastery suite tailored to Arabic native speakers—combining structured vocabulary acquisition across three specialized decks with active listening, pronunciation diagnostics, and scenario-based AI tutoring.
- **Target Audience / Persona**: Arabic-speaking learners preparing for the IELTS exam, working or interviewing in the software/IT industry, or building core English fluency from A1 to C1.
- **Key Value**: Unlike generic language apps, LinguaBridge directly addresses Arabic-to-English linguistic interference—such as `/p/` vs `/b/`, `/v/` vs `/f/`, `/θ/` & `/ð/`, consonant clusters, and English false friends—while explaining grammar and mouth positioning in clear Arabic.

---

## 2. User Experience & Visual Design

### Key User Flows
1. **Onboarding & Personalization**:
   - First-launch welcome with friendly mascot ("Noor the Owl-Falcon"), UI language toggle (Arabic RTL default / English LTR), and dialect toggle (MSA / Egyptian).
   - Goal picker (IELTS Preparation, IT Career, or General Fluency), daily commitment selector (5, 10, or 20 minutes), and an interactive 5-question CEFR placement mini-test that generates a personalized learning plan.
2. **Home Dashboard & Deck Management**:
   - Top status bar displaying Daily Streak, Streak Freeze status, XP level badge, Hearts count, and quick UI language/dialect switcher.
   - Three color-coded Deck Progress Cards (**Oxford 5000** in Gold, **IELTS Core** in Teal, **IT Industry** in Maroon/Crimson) showing learned/total counts, deck filter toggles, and a **"Review Due Today"** SRS action button with badge count.
   - Quick-access cards for **CSV/JSON Custom Deck Import** (with Gemini batch auto-fill for missing Arabic translations, IPA, and examples), **Daily Word Notification preview**, and **Practice Weak Words**.
3. **Gamified Learning Path & Specialized Modes (Learn Tab)**:
   - Vertical node path grouping words into 8–10 word lessons ordered by CEFR level (A1–C1) and selected decks, alongside dedicated **IELTS Mode** (academic quizzes + Gemini Band Score Writing/Speaking evaluator) and **IT Industry Mode** (tech flashcards, tappable tech mini-articles, and mock software engineering interview prompts).
   - Interactive multi-format exercises: Multiple Choice (EN→AR & AR→EN), Listening comprehension, Spelling/Typing, Sentence Fill-in-the-Blank, Match Pairs, Word-Order sentence builder, and Speaking check.
   - Arabic-specific Hint Banners highlighting false friends, silent letters, and grammar contrasts, plus celebratory completion animations and a local Weekly Leaderboard.
4. **Watch & Learn Interactive Player (Watch Tab)**:
   - Curated interactive lessons (TED-style talks, IT standups, IELTS interviews) plus a custom SRT / transcript / URL importer where Gemini automatically generates Arabic line-by-line translations and extracts a Key Vocabulary list.
   - Synchronized dual subtitles (English + Arabic toggleable, adjustable font size) with color-coded highlights for words belonging to Oxford (Gold), IELTS (Teal), or IT (Maroon) decks.
   - Tap any word in a subtitle to open a rich bottom sheet with Arabic meaning, IPA, TTS audio, example sentence, and **"Add to My Words /SRS"**.
   - Transport controls for sentence looping, slow speed (`0.75x` / `1.0x`), previous/next line jumping, and auto-pause after each line.
5. **ELSA-Style Pronunciation Coach (Speak Tab — Coach Mode)**:
   - Focuses on high-priority phoneme challenges for Arabic speakers: `P vs B`, `V vs F`, short vowels (`ɪ/e/æ/ʌ`), `th (θ/ð)`, `ŋ`, consonant clusters (`street`, `spring`), and final consonant clarity.
   - Includes a Minimal-Pair Drill studio (`ship/sheep`, `pen/ben`, `very/ferry`, `think/sink`) with native TTS model playback.
   - Users record their voice via Android speech/audio capture; Gemini analyzes the spoken transcript and phonetic accuracy to return an overall score (0–100%), color-coded word/phoneme feedback (Green/Yellow/Red), and mouth/tongue positioning tips in simple MSA or Egyptian Arabic.
6. **AI Conversation Tutor (Speak Tab — AI Tutor Mode)**:
   - Scenario-driven chat partner calibrated to the user's CEFR level: **IT Job Interview**, **IELTS Speaking Part 1/2/3**, **Daily Life**, and **Customer Support**.
   - Gentle inline grammar corrections, one-tap Arabic explanation of mistakes, vocabulary upgrade suggestions pulled directly from the user's active decks, and full voice input/output.
7. **Progress, Analytics & SRS Review (Profile Tab)**:
   - Comprehensive dashboard with Words Learned per deck, Accuracy percentage, Streak Calendar grid, Time Studied, Pronunciation Score trend chart, Badges showcase, and a Weak Words diagnostic list with a one-tap **"Practice Weak Words"** session.

### Visual Identity & Theme
- **Aesthetic Direction**: Modern Editorial Gamification—warm, inviting, high-contrast surfaces with culturally resonant geometric accents, crisp card elevation, and clear bidirectional (`RTL`/`LTR`) visual hierarchy.
- **Color Palette & Mood**:
  - **Surface / Background**: Deep Obsidian Slate (`#0F141C`) in Dark Mode and Warm Sand Ivory (`#F8F7F4`) in Light Mode.
  - **Primary Brand Accent**: Emerald Oasis (`#10B981`) for primary CTAs, mastery indicators, and active navigation pills.
  - **Deck Identity Tokens**:
    - **Oxford 5000 Deck**: Royal Amber Gold (`#F59E0B` / `#FEF3C7`)
    - **IELTS Core Deck**: Academic Teal (`#0D9488` / `#CCFBF1`)
    - **IT Industry Deck**: Ruby Maroon (`#E11D48` / `#FFE4E6`)
  - **Semantic Feedback**: Phoneme Match Green (`#22C55E`), Near-Phoneme Amber (`#F59E0B`), Phoneme Error Coral (`#EF4444`).
- **Typography & Hierarchy**:
  - **Arabic Font Family**: Bundled Google Font **Cairo** (`R.font.cairo`) for clear, modern Arabic headlines, subtitles, and vowel/diacritic legibility.
  - **English & Phonetic Font Family**: Bundled Google Font **Inter** (`R.font.inter`) for English definitions, IPA transcriptions, and code/IT terminology.
  - **Bidirectional Text Handling**: Explicit `LayoutDirection.Rtl` and `LayoutDirection.Ltr` scoping so English words, IPA strings, and sentences embedded inside Arabic explanations render cleanly without punctuation scrambling.
- **Component Styling & Layout**: Material 3 `NavigationBar` with 5 main destinations (`Home`, `Learn`, `Watch`, `Speak`, `Profile`), minimum `48.dp` touch targets, rounded `20.dp` cards with subtle border highlights, and full edge-to-edge safe-area inset handling.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Native Room SQLite Storage + Pre-Seeded 90+ Word Multi-Deck Dataset**
  - *Chosen Approach*: Seed the Room database on first launch with ~30 curated, authentic words per deck (Oxford 5000, IELTS Core, IT Industry = 90+ words total) complete with CEFR levels, Arabic translations, IPA, synonyms, dual-language examples, and Arabic speaker tips, while scaling the UI progress counters to reflect the full deck targets (`5,836`, `3,575`, and `1,700`) and supporting live CSV/JSON additions.
  - *Why*: Gives the user immediate, zero-latency access to all learning modes, SRS reviews, and deck filtering offline while allowing unlimited custom vocabulary expansion.
- **Decision 2: Direct Gemini REST API (`gemini-3.5-flash`) with Local Response Caching**
  - *Chosen Approach*: Use Retrofit + Kotlinx Serialization calling `gemini-3.5-flash` with structured JSON schemas and local Room caching for AI translations, batch CSV auto-fill, pronunciation diagnostics, and IELTS/IT evaluations.
  - *Why*: Eliminates redundant API calls when re-opening videos or reviewing words, provides deterministic structured JSON for phoneme color-coding, and degrades gracefully with helpful fallback hints if the API key is not yet configured.
- **Decision 3: Hybrid Android Speech Capture + Gemini Phonetic Diagnostic Engine**
  - *Chosen Approach*: Combine Android's native `SpeechRecognizer` / `AudioRecord` and `TextToSpeech` with Gemini's phonetic analysis of Arabic-speaker pronunciation patterns.
  - *Why*: Delivers responsive real-time voice recording and model audio playback alongside deep Arabic mouth-position coaching tailored to specific target words and minimal pairs.

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

### Architecture & Component Diagram

```
┌───────────────────────────────────────────────────────────────────────────────┐
│                         Jetpack Compose UI Layer (RTL/LTR)                    │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌───────────┐ ┌─────────┐ │
│  │  Home & SRS  │ │  Learn Path  │ │ Watch & Learn│ │   Speak   │ │ Profile │ │
│  │ Deck Manager │ │  IELTS / IT  │ │ Dual Player  │ │Coach/Tutor│ │Analytics│ │
│  └──────┬───────┘ └──────┬───────┘ └──────┬───────┘ └─────┬─────┘ └────┬────┘ │
└─────────┼────────────────┼────────────────┼───────────────┼────────────┼──────┘
          │                │                │               │            │
          ▼                ▼                ▼               ▼            ▼
┌───────────────────────────────────────────────────────────────────────────────┐
│                     LinguaBridgeViewModel (StateFlow / MVVM)                  │
│   • Onboarding & Placement State     • Active Lesson & Exercise Engine        │
│   • SM-2 Spaced Repetition Engine    • Subtitle Sync & Word Lookup State      │
│   • Pronunciation & Minimal Pairs    • AI Chat & IELTS/IT Assessment State    │
└──────────────────────────┬───────────────────────────────────┬────────────────┘
                           │                                   │
                           ▼                                   ▼
┌─────────────────────────────────────────┐ ┌───────────────────────────────────┐
│      LinguaRepository & Room Database   │ │   GeminiAiService & Audio Engine  │
│  ┌───────────────────────────────────┐  │ │  ┌─────────────────────────────┐  │
│  │ WordEntity (Oxford / IELTS / IT)  │  │ │  │ Gemini 3.5 Flash REST API   │  │
│  │ UserStatsEntity (XP, Streak, Goal)│  │ │  │ • Batch CSV/JSON Auto-Fill  │  │
│  │ TranscriptEntity (Subtitles/Vocab)│  │ │  │ • Subtitle Translation/Vocab│  │
│  │ PronunciationLogEntity (Scores)   │  │ │  │ • Phoneme & Mouth-Tip Coach │  │
│  │ AiCacheEntity (Hash -> JSON)      │  │ │  │ • Scenario Chat & IELTS Band│  │
│  └───────────────────────────────────┘  │ │  └─────────────────────────────┘  │
│  • SM-2 Calculator (Ease, Interval)     │ │  • Android TTS & Speech Capture   │
└─────────────────────────────────────────┘ └───────────────────────────────────┘
```

### Data Model & State
- **`WordEntity`**: `id`, `word`, `partOfSpeech`, `cefrLevel` (`A1`–`C1`), `deck` (`OXFORD`, `IELTS`, `IT`), `arabicTranslation`, `englishDefinition`, `exampleEn1`, `exampleAr1`, `exampleEn2`, `exampleAr2`, `ipa`, `synonyms`, `arabicSpeakerTip`, `masteryLevel` (`NEW`, `LEARNING`, `REVIEW`, `MASTERED`), `repetitions`, `easeFactor`, `intervalDays`, `nextReviewDate`, `correctCount`, `mistakeCount`, `isWeakWord`.
- **`UserStatsEntity`**: `onboardingCompleted`, `learningGoal`, `cefrLevel`, `dailyGoalMinutes`, `uiLanguage` (`AR`/`EN`), `arabicDialect` (`MSA`/`EGYPTIAN`), `darkMode`, `xp`, `level`, `streakDays`, `streakFreezeAvailable`, `hearts`, `minutesStudiedToday`, `activeDecksCsv`.
- **`TranscriptLessonEntity`**: `id`, `title`, `category` (`OXFORD`, `IELTS`, `IT`, `CUSTOM`), `durationSec`, `linesJson` (timestamped English + Arabic subtitle lines), `keyVocabJson`.
- **`PronunciationAttemptEntity`**: `id`, `targetText`, `spokenText`, `focusCategory` (`P_B`, `V_F`, `SHORT_VOWELS`, `TH`, `NG`, `CLUSTERS`, `FINAL_SOUNDS`), `overallScore`, `phonemeFeedbackJson`, `arabicTip`, `timestamp`.
- **`AiCacheEntity`**: `cacheKey` (SHA-256 of prompt + mode), `responseJson`, `createdAt` to prevent duplicate Gemini API requests.

### Interactive Component & State Mapping
- **SM-2 Spaced Repetition Engine**: Grading a flashcard (`Again`, `Hard`, `Good`, `Easy`) recalculates `easeFactor` (minimum `1.3`), updates `intervalDays` (`1d` → `3d` → `interval * easeFactor`), transitions `masteryLevel` (`NEW` → `LEARNING` → `REVIEW` → `MASTERED`), and schedules `nextReviewDate`.
- **CSV / JSON Deck Importer**: Parses user-pasted or uploaded CSV/JSON rows (`word, pos, level, deck, arabic, definition, example`), identifies missing fields (`arabic`, `ipa`, `example`), and dispatches a structured batch request to Gemini to populate all missing metadata before inserting into Room.
- **Interactive Dual-Subtitle Player**: Synchronizes transcript playback timer with Android TTS speech synthesis; supports sentence loop toggling, `0.75x`/`1.0x` playback rate, auto-pause after each line, deck-color word highlighting, and instant word inspection bottom sheets.
- **Pronunciation Coach & Minimal Pairs**: Captures spoken audio/speech, compares target phonemes against Arabic interference rules via Gemini structured output, renders word-by-word and phoneme-by-phoneme color badges, and displays tailored mouth/tongue placement guidance in MSA or Egyptian Arabic.
