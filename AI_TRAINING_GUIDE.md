# AI Training Guide for Voice Expense Logger

## How the AI Works

The Voice Expense Logger uses a **hybrid AI system** combining:
1. **Rule-based NLP** (Natural Language Processing)
2. **Keyword matching**
3. **User-supervised learning**

### Three-Layer Intelligence

```
Layer 1: Pattern Matching
├─ Extracts amount using regex
├─ Identifies spending verbs
└─ Removes filler words

Layer 2: Keyword Analysis
├─ Scores description against category keywords
├─ Uses merchant detection
└─ Applies base knowledge

Layer 3: Learning System
├─ Checks user corrections
├─ Prioritizes learned patterns
└─ Stores in SharedPreferences
```

## Training Data Architecture

### Pre-loaded Knowledge Base

Located in: `app/src/main/assets/voice_training_data.json`

Contains:
- **20+ Training Examples**: Real-world voice inputs with expected outputs
- **Category Keywords**: 200+ keywords mapped to categories
- **Parsing Patterns**: Common phrase structures
- **User Tips**: Guidance for better recognition

### Learned Patterns Storage

Location: `SharedPreferences` → `"voice_expense_patterns"`

Format:
```json
{
  "ice cream": "Food & Dining",
  "gym session": "Health & Wellness",
  "movie ticket": "Entertainment",
  "taxi ride": "Transport"
}
```

### Training Algorithm

```kotlin
fun predictCategory(description: String, context: Context): String {
    // 1. Check learned patterns (highest priority)
    val learned = checkLearnedPatterns(description, context)
    if (learned != null) return learned
    
    // 2. Check base keyword knowledge
    var maxScore = 0
    var predictedCategory = "Miscellaneous"
    
    for ((category, keywords) in categoryKeywords) {
        var score = 0
        for (keyword in keywords) {
            if (description.contains(keyword)) {
                // Exact word match = 3 points
                // Partial match = 1 point
                score += if (description.split(" ").contains(keyword)) 3 else 1
            }
        }
        
        if (score > maxScore) {
            maxScore = score
            predictedCategory = category
        }
    }
    
    return predictedCategory
}
```

## How to Train the AI

### Method 1: Automatic Learning (User Corrections)

**Scenario**: User says "spent 100 on ice cream"

```
1. AI predicts: "Miscellaneous" (if "ice cream" not in keywords)
2. User changes to: "Food & Dining" in confirmation dialog
3. System automatically trains: "ice cream" → "Food & Dining"
4. Next time: AI predicts "Food & Dining" immediately
```

**Code Flow**:
```kotlin
// In VoiceExpenseConfirmationDialog
if (selectedCategory != voiceExpense.category) {
    // User changed category - train AI
    VoiceExpenseParser.trainPattern(
        context = context,
        description = editedDescription.lowercase(),
        correctCategory = selectedCategory
    )
}
```

### Method 2: Manual Training (Adding Keywords)

**For Developers**: Add keywords to `VoiceExpenseParser.kt`

```kotlin
// Line 40 in VoiceExpenseParser.kt
private val categoryKeywords = mapOf(
    "Food & Dining" to listOf(
        // Existing keywords
        "food", "lunch", "dinner", "coffee",
        
        // Add your keywords here
        "breakfast", "brunch", "snack", "meal",
        "dosa", "biryani", "thali", "roti"
    ),
    
    "Your Custom Category" to listOf(
        "keyword1", "keyword2", "keyword3"
    )
)
```

### Method 3: Bulk Training (Import Data)

**For Advanced Users**: Create a training script

```kotlin
// TrainingHelper.kt (create this file)
object TrainingHelper {
    fun bulkTrainPatterns(context: Context, patterns: Map<String, String>) {
        patterns.forEach { (description, category) ->
            VoiceExpenseParser.trainPattern(context, description, category)
        }
    }
    
    fun importFromJSON(context: Context, jsonFile: String) {
        val json = context.assets.open(jsonFile).bufferedReader().use { it.readText() }
        val patterns: Map<String, String> = Gson().fromJson(json, Map::class.java)
        bulkTrainPatterns(context, patterns)
    }
}

// Usage in MainActivity or settings screen
TrainingHelper.importFromJSON(context, "my_patterns.json")
```

## Expanding Category Intelligence

### Adding a New Category

**Step 1**: Add category to `ExpenseManager.kt`
```kotlin
// Line 29 in ExpenseManager.kt
val defaultCategories = listOf(
    "Food & Dining",
    "Transport",
    // ... existing categories ...
    "Your New Category" // Add here
)
```

**Step 2**: Add keywords to `VoiceExpenseParser.kt`
```kotlin
// Line 40 in VoiceExpenseParser.kt
private val categoryKeywords = mapOf(
    // ... existing categories ...
    
    "Your New Category" to listOf(
        "keyword1", "keyword2", "keyword3",
        "brand1", "brand2"
    )
)
```

**Step 3**: Update training data JSON
```json
// voice_training_data.json
{
  "category_keywords": {
    "Your New Category": [
      "keyword1", "keyword2", "keyword3"
    ]
  }
}
```

### Example: Adding "Education" Category

```kotlin
// ExpenseManager.kt
val defaultCategories = listOf(
    // ... existing ...
    "Education"
)

// VoiceExpenseParser.kt
"Education" to listOf(
    "book", "course", "tuition", "class", "workshop",
    "udemy", "coursera", "tutorial", "exam", "fees",
    "notebook", "study", "learn"
)
```

Now voice inputs like:
- "paid 500 for course" → Education
- "bought books for 300" → Education
- "udemy subscription 1000" → Education

## Improving Category Accuracy

### Understanding Scoring System

```
Exact Word Match = 3 points
Partial Match = 1 point

Example: "coffee shop"
- "coffee" (exact) = 3 points → Food & Dining
- "shop" (exact) = 3 points → Shopping
- Result: Ambiguous (tie)
```

### Resolving Ambiguities

**Problem**: "coffee shop" matches both Food & Shopping

**Solution 1**: Add compound keywords
```kotlin
"Food & Dining" to listOf(
    "coffee",
    "coffee shop", // More specific
    "cafe"
)
```

**Solution 2**: Increase keyword specificity
```kotlin
// Instead of generic "shop"
"Shopping" to listOf(
    "shopping mall",
    "online shopping",
    "clothes shop"
)
```

**Solution 3**: Use context keywords
```kotlin
"Food & Dining" to listOf(
    "coffee", "shop", "cafe", // Weak signals
    "barista", "latte", "espresso" // Strong signals
)
```

### Regional Customization

**Indian Food Items**:
```kotlin
"Food & Dining" to listOf(
    // English
    "breakfast", "lunch", "dinner",
    
    // Indian
    "chai", "tea", "dosa", "idli", "vada",
    "biryani", "thali", "roti", "paratha",
    "samosa", "pakora", "chaat", "pani puri",
    
    // Regional
    "dhokla", "pav bhaji", "vada pav", "misal pav"
)
```

**Indian Transport**:
```kotlin
"Transport" to listOf(
    // Standard
    "taxi", "cab", "bus", "metro",
    
    // Indian
    "auto", "rickshaw", "auto rickshaw",
    "local train", "ola", "uber", "rapido",
    "bike", "scooter"
)
```

## Advanced Training Techniques

### Pattern Weighting

Give more weight to recently used patterns:

```kotlin
// Enhanced training with timestamp
data class LearnedPattern(
    val description: String,
    val category: String,
    val timestamp: Long,
    val useCount: Int
)

// Prioritize recent + frequent patterns
fun predictCategory(description: String): String {
    val patterns = getLearnedPatterns()
        .sortedByDescending { it.useCount * (1 / (now - it.timestamp)) }
    
    return patterns.firstOrNull { 
        description.contains(it.description) 
    }?.category ?: fallbackPrediction(description)
}
```

### Context-Aware Predictions

Consider time of day:

```kotlin
fun predictCategory(description: String, context: Context): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    
    // Morning (6-11): Boost breakfast items
    // Afternoon (12-16): Boost lunch items
    // Evening (17-22): Boost dinner/entertainment
    
    val timeBoost = when (hour) {
        in 6..11 -> mapOf("breakfast" to 5, "coffee" to 3)
        in 12..16 -> mapOf("lunch" to 5)
        in 17..22 -> mapOf("dinner" to 5, "movie" to 3)
        else -> emptyMap()
    }
    
    // Apply time-based boosting to scores
}
```

### User Habit Learning

Track user's most common categories:

```kotlin
fun getFrequentCategories(context: Context): Map<String, Int> {
    val expenses = ExpenseManager.getExpenses(context)
    return expenses
        .filter { it.type == "Cash" }
        .groupingBy { it.category }
        .eachCount()
        .toList()
        .sortedByDescending { it.second }
        .toMap()
}

// Use in prediction
fun predictCategory(description: String, context: Context): String {
    val baseCategory = keywordPrediction(description)
    val frequentCategories = getFrequentCategories(context)
    
    // If ambiguous, prefer user's frequent categories
    if (score < threshold) {
        return frequentCategories.keys.firstOrNull() ?: baseCategory
    }
    return baseCategory
}
```

## Testing AI Accuracy

### Create a Test Suite

```kotlin
// AIAccuracyTest.kt
class AIAccuracyTest {
    val testCases = mapOf(
        "spent 100 on coffee" to "Food & Dining",
        "paid 200 for uber" to "Transport",
        "bought shirt for 800" to "Shopping",
        // Add 50+ test cases
    )
    
    @Test
    fun testAccuracy() {
        var correct = 0
        testCases.forEach { (input, expected) ->
            val result = VoiceExpenseParser.parse(input, context)
            if (result?.category == expected) correct++
        }
        
        val accuracy = (correct.toFloat() / testCases.size) * 100
        println("AI Accuracy: $accuracy%")
        assertTrue(accuracy >= 80.0) // Target: 80%+ accuracy
    }
}
```

### Benchmark Performance

```kotlin
// Track prediction accuracy over time
data class PredictionMetrics(
    val totalPredictions: Int,
    val correctPredictions: Int,
    val userCorrections: Int,
    val accuracy: Float
) {
    fun save(context: Context) {
        // Save to SharedPreferences
    }
    
    companion object {
        fun calculate(context: Context): PredictionMetrics {
            val allExpenses = ExpenseManager.getExpenses(context)
            val voiceExpenses = getVoiceExpenses(context) // Track voice vs manual
            
            // Count how many were corrected by user
            val corrections = getPatternHistory(context).size
            
            return PredictionMetrics(
                totalPredictions = voiceExpenses.size,
                correctPredictions = voiceExpenses.size - corrections,
                userCorrections = corrections,
                accuracy = ((voiceExpenses.size - corrections).toFloat() / voiceExpenses.size) * 100
            )
        }
    }
}
```

## Monitoring & Analytics

### Add Logging

```kotlin
// In VoiceExpenseParser.kt
fun parse(text: String, context: Context): VoiceExpense? {
    Log.d(TAG, "Parsing: $text")
    
    val result = /* parsing logic */
    
    Log.d(TAG, "Result: amount=${result?.amount}, category=${result?.category}")
    
    // Track analytics
    logParsingEvent(context, text, result)
    
    return result
}

private fun logParsingEvent(context: Context, input: String, result: VoiceExpense?) {
    val event = ParsingEvent(
        timestamp = System.currentTimeMillis(),
        input = input,
        success = result != null,
        category = result?.category
    )
    // Save to analytics file or Firebase
}
```

### Export Training Data

```kotlin
fun exportLearnedPatterns(context: Context): String {
    val prefs = context.getSharedPreferences("FinPulsePrefs", Context.MODE_PRIVATE)
    val json = prefs.getString("voice_expense_patterns", "{}")
    return json ?: "{}"
}

// Usage: Share learned patterns between devices
fun shareTrainingData(context: Context) {
    val json = exportLearnedPatterns(context)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(Intent.EXTRA_TEXT, json)
    }
    context.startActivity(Intent.createChooser(intent, "Share Training Data"))
}
```

## Best Practices

### For Users
1. **Be Consistent**: Use similar phrases for same expenses
2. **Correct Mistakes**: Always fix wrong categories (AI learns)
3. **Be Specific**: "Coffee at Starbucks" better than just "coffee"
4. **Use Brand Names**: "Uber" better than "taxi ride"

### For Developers
1. **Start Simple**: Begin with 5-7 main categories
2. **Test Thoroughly**: Use 50+ test cases for 80% accuracy
3. **Monitor Corrections**: Track how often users fix predictions
4. **Update Keywords**: Add new brands/terms quarterly
5. **Version Training Data**: Keep backups of keyword mappings

### For Businesses
1. **Localize**: Add region-specific keywords
2. **Industry Focus**: Customize categories for target users
3. **A/B Testing**: Test different keyword sets
4. **User Feedback**: Add "Report Wrong Category" feature

## Future AI Enhancements

### 1. ML Kit Integration (Already Added)
```kotlin
// Use ML Kit Entity Extraction for better parsing
val entityExtractor = EntityExtractor.Builder(EntityExtractorOptions.ENGLISH)
    .build()

entityExtractor.downloadModelIfNeeded()
    .addOnSuccessListener {
        entityExtractor.annotate(text)
            .addOnSuccessListener { annotations ->
                // Extract amounts, dates, locations automatically
            }
    }
```

### 2. TensorFlow Lite Model
Train a custom model:
```
Input: "spent 100 on coffee"
Output: {
  category: "Food & Dining" (confidence: 0.95),
  amount: 100,
  merchant: null
}
```

### 3. Multi-language Support
```kotlin
val languageDetector = LanguageIdentification.getClient()
languageDetector.identifyLanguage(text)
    .addOnSuccessListener { languageCode ->
        // Use language-specific keywords
    }
```

### 4. Voice Assistants Integration
- Google Assistant Actions
- Alexa Skills
- Siri Shortcuts

## Conclusion

The AI system is designed to:
- ✅ Work immediately (pre-trained keywords)
- ✅ Improve with use (learns from corrections)
- ✅ Be customizable (add keywords easily)
- ✅ Stay local (no cloud dependency)
- ✅ Be lightweight (< 1MB storage)

**Target Accuracy**: 80-90% with 50+ learned patterns

---
**Questions? Add more keywords or check the test suite for accuracy metrics.**
