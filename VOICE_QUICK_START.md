# Voice Expense Logger - Quick Start Guide

## For Users

### How to Use Voice Input

1. **Open the Dashboard**
   - Launch the Fin-Pulse app
   - Navigate to the main dashboard screen

2. **Tap the Mic Button**
   - Look for the blue microphone button above the green + button
   - Tap it to open the voice input dialog

3. **Grant Permission (First Time Only)**
   - If prompted, tap "Allow" to grant microphone permission
   - This is required for voice recognition

4. **Speak Your Expense**
   - Tap the large mic button in the dialog
   - Wait for "Listening... Speak now!" message
   - Speak clearly: "spent 100 on ice cream"
   - The app will process your voice automatically

5. **Confirm & Edit**
   - Review the parsed expense details
   - Edit amount, description, or category if needed
   - Tap "Log Expense" to save

6. **Done!**
   - Your expense is logged as "Cash" type
   - The dashboard refreshes automatically
   - The AI learns from your corrections

### Voice Input Examples

**Good Examples:**
- "spent 100 on ice cream" → ₹100, Ice cream, Food & Dining
- "paid 500 for lunch" → ₹500, Lunch, Food & Dining
- "bought coffee for 150" → ₹150, Coffee, Food & Dining
- "50 rupees for auto" → ₹50, Auto, Transport
- "uber 200" → ₹200, Uber, Transport
- "movie ticket 300" → ₹300, Movie ticket, Entertainment

**Tips for Better Recognition:**
- Speak clearly and at normal pace
- Mention both amount and item
- Use common words (coffee, lunch, uber)
- Speak in a quiet environment
- Hold phone at normal distance

### AI Category Learning

The app gets smarter with use:

1. **Initial Prediction**
   - Based on keywords like "coffee" → Food & Dining

2. **User Correction**
   - You change "Coffee" from "Miscellaneous" to "Food & Dining"

3. **AI Learns**
   - Next time "coffee" is mentioned, it auto-selects "Food & Dining"

4. **Continuous Improvement**
   - The more you use it, the better it gets at your spending patterns

### Supported Categories

The AI can detect these categories:
- 🍔 Food & Dining (coffee, lunch, restaurant, pizza)
- 🚗 Transport (uber, taxi, metro, petrol)
- 🛍️ Shopping (amazon, clothes, shoes, mall)
- 🎬 Entertainment (movie, netflix, concert, party)
- 💡 Bills & Utilities (electricity, mobile, internet)
- 💊 Health & Wellness (medicine, gym, doctor)
- 🛒 Groceries (vegetables, milk, supermarket)
- 🎁 Gifts (birthday, present, anniversary)
- 📝 Stationery (pen, notebook, paper)
- 🔹 Miscellaneous (everything else)

### Troubleshooting

**"Couldn't understand" error:**
- Make sure you mention an amount (number)
- Try format: "spent [amount] on [item]"
- Example: "spent 100 on coffee"

**Wrong category detected:**
- No problem! Edit it in the confirmation dialog
- The AI will learn and improve next time

**Microphone not working:**
- Check app permissions in phone settings
- Make sure no other app is using the microphone
- Ensure device has Google app installed

**Speech not recognized:**
- Speak more clearly
- Reduce background noise
- Check internet connection (required for speech recognition)

## For Developers

### Integration Summary

The voice feature has been fully integrated into your Fin-Pulse app:

**New Files Added:**
- `VoiceInputHelper.kt` - Speech-to-text handler
- `VoiceExpenseParser.kt` - NLP parser with AI
- `VoiceComposables.kt` - UI components
- `voice_training_data.json` - Training data
- `VOICE_FEATURE_DOCUMENTATION.md` - Full docs

**Modified Files:**
- `MainActivity.kt` - Added voice button & dialogs to Dashboard
- `AndroidManifest.xml` - Added RECORD_AUDIO permission
- `build.gradle.kts` - Added ML Kit dependency
- `libs.versions.toml` - Added ML Kit version

**New Dependencies:**
- ML Kit Entity Extraction (for future NLP enhancements)

### Building the App

```bash
# Sync Gradle
./gradlew clean build

# Run on device/emulator
./gradlew installDebug

# Test the feature
1. Launch app
2. Complete onboarding if needed
3. Look for blue mic button on Dashboard
4. Tap and grant microphone permission
5. Say "spent 100 on ice cream"
6. Confirm the parsed expense
```

### Testing Voice Input

**Unit Test Cases** (to be added):
```kotlin
@Test
fun testAmountExtraction() {
    val result = VoiceExpenseParser.parse("spent 100 on coffee", context)
    assertEquals(100f, result?.amount)
}

@Test
fun testCategoryPrediction() {
    val result = VoiceExpenseParser.parse("paid 500 for lunch", context)
    assertEquals("Food & Dining", result?.category)
}
```

**Manual Test Scenarios:**
1. Permission flow (grant/deny)
2. Various voice formats
3. Category predictions
4. AI learning (correct a category, repeat same input)
5. Error handling (no internet, background noise)
6. UI animations (pulsing mic, status messages)

### Customization

**Add New Categories:**
Edit `VoiceExpenseParser.kt` line 40:
```kotlin
private val categoryKeywords = mapOf(
    "Your Category" to listOf("keyword1", "keyword2")
)
```

**Add Merchant Detection:**
Edit `VoiceExpenseParser.kt` line 213:
```kotlin
val merchants = listOf("starbucks", "your_brand")
```

**Change Voice Button Color:**
Edit `VoiceComposables.kt` line 31:
```kotlin
containerColor = Color(0xFF1E88E5), // Change color
```

### API Reference

**Parse Voice Input:**
```kotlin
val expense = VoiceExpenseParser.parse(
    text = "spent 100 on coffee",
    context = context
)
// Returns: VoiceExpense(amount=100, description="Coffee", 
//          category="Food & Dining", merchant=null)
```

**Train AI:**
```kotlin
VoiceExpenseParser.trainPattern(
    context = context,
    description = "ice cream",
    correctCategory = "Food & Dining"
)
```

**Check Availability:**
```kotlin
if (VoiceInputHelper.isAvailable(context)) {
    // Speech recognition available
}
```

### Architecture Diagram

```
User Voice Input
       ↓
VoiceInputHelper (Speech-to-Text)
       ↓
VoiceExpenseParser (NLP + AI)
       ↓
VoiceExpenseConfirmationDialog (User Review)
       ↓
ExpenseManager (Save to SharedPreferences)
       ↓
Dashboard (Update UI)
```

### Data Flow

```
Speech → Text → Parse → Predict → Confirm → Log → Learn
                                               ↓
                                          Update Balance
                                               ↓
                                          Refresh Dashboard
```

## Support

For issues or questions:
- Check Logcat for errors
- Review VOICE_FEATURE_DOCUMENTATION.md for details
- Test with example phrases first
- Verify permissions are granted

---
**Happy Voice Logging! 🎤💰**
