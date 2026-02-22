package com.avinya.fin_pulse_android

import android.content.Context
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

/**
 * Voice Expense Parser Unit Tests
 * 
 * To run these tests:
 * 1. Add dependencies to app/build.gradle.kts:
 *    testImplementation("org.mockito:mockito-core:5.3.1")
 *    testImplementation("org.mockito:mockito-inline:5.2.0")
 * 
 * 2. Run tests:
 *    ./gradlew test
 *    or
 *    Right-click on test class in Android Studio → Run Tests
 */
class VoiceExpenseParserTest {

    @Mock
    private lateinit var mockContext: Context

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        
        // Mock SharedPreferences
        val mockPrefs = Mockito.mock(android.content.SharedPreferences::class.java)
        val mockEditor = Mockito.mock(android.content.SharedPreferences.Editor::class.java)
        
        Mockito.`when`(mockContext.getSharedPreferences(Mockito.anyString(), Mockito.anyInt()))
            .thenReturn(mockPrefs)
        Mockito.`when`(mockPrefs.edit()).thenReturn(mockEditor)
        Mockito.`when`(mockEditor.putString(Mockito.anyString(), Mockito.anyString()))
            .thenReturn(mockEditor)
        Mockito.`when`(mockPrefs.getString(Mockito.anyString(), Mockito.isNull()))
            .thenReturn(null)
    }

    // ==================== Amount Extraction Tests ====================

    @Test
    fun testAmountExtraction_SimpleNumber() {
        val result = VoiceExpenseParser.parse("spent 100 on coffee", mockContext)
        assertNotNull(result)
        assertEquals(100f, result?.amount)
    }

    @Test
    fun testAmountExtraction_LargeNumber() {
        val result = VoiceExpenseParser.parse("spent 1500 on groceries", mockContext)
        assertNotNull(result)
        assertEquals(1500f, result?.amount)
    }

    @Test
    fun testAmountExtraction_WithRupees() {
        val result = VoiceExpenseParser.parse("50 rupees for auto", mockContext)
        assertNotNull(result)
        assertEquals(50f, result?.amount)
    }

    @Test
    fun testAmountExtraction_ShortFormat() {
        val result = VoiceExpenseParser.parse("coffee 150", mockContext)
        assertNotNull(result)
        assertEquals(150f, result?.amount)
    }

    @Test
    fun testAmountExtraction_NoAmount() {
        val result = VoiceExpenseParser.parse("spent on coffee", mockContext)
        assertNull(result) // Should return null if no amount found
    }

    // ==================== Description Extraction Tests ====================

    @Test
    fun testDescriptionExtraction_Simple() {
        val result = VoiceExpenseParser.parse("spent 100 on ice cream", mockContext)
        assertNotNull(result)
        assertEquals("Ice cream", result?.description)
    }

    @Test
    fun testDescriptionExtraction_PaidFor() {
        val result = VoiceExpenseParser.parse("paid 500 for lunch", mockContext)
        assertNotNull(result)
        assertEquals("Lunch", result?.description)
    }

    @Test
    fun testDescriptionExtraction_BoughtFor() {
        val result = VoiceExpenseParser.parse("bought coffee for 150", mockContext)
        assertNotNull(result)
        assertEquals("Coffee", result?.description)
    }

    @Test
    fun testDescriptionExtraction_ShortFormat() {
        val result = VoiceExpenseParser.parse("pizza 600", mockContext)
        assertNotNull(result)
        assertEquals("Pizza", result?.description)
    }

    @Test
    fun testDescriptionExtraction_MultipleWords() {
        val result = VoiceExpenseParser.parse("spent 800 on dinner at restaurant", mockContext)
        assertNotNull(result)
        assertTrue(result?.description?.contains("dinner") == true)
    }

    // ==================== Category Prediction Tests ====================

    @Test
    fun testCategoryPrediction_FoodDining() {
        val result = VoiceExpenseParser.parse("spent 100 on coffee", mockContext)
        assertNotNull(result)
        assertEquals("Food & Dining", result?.category)
    }

    @Test
    fun testCategoryPrediction_IceCream() {
        val result = VoiceExpenseParser.parse("spent 100 on ice cream", mockContext)
        assertNotNull(result)
        assertEquals("Food & Dining", result?.category)
    }

    @Test
    fun testCategoryPrediction_Transport_Uber() {
        val result = VoiceExpenseParser.parse("paid 200 for uber", mockContext)
        assertNotNull(result)
        assertEquals("Transport", result?.category)
    }

    @Test
    fun testCategoryPrediction_Transport_Auto() {
        val result = VoiceExpenseParser.parse("50 rupees for auto", mockContext)
        assertNotNull(result)
        assertEquals("Transport", result?.category)
    }

    @Test
    fun testCategoryPrediction_Shopping() {
        val result = VoiceExpenseParser.parse("bought shirt for 800", mockContext)
        assertNotNull(result)
        assertEquals("Shopping", result?.category)
    }

    @Test
    fun testCategoryPrediction_Entertainment() {
        val result = VoiceExpenseParser.parse("paid 300 for movie ticket", mockContext)
        assertNotNull(result)
        assertEquals("Entertainment", result?.category)
    }

    @Test
    fun testCategoryPrediction_Bills() {
        val result = VoiceExpenseParser.parse("spent 2000 on electricity bill", mockContext)
        assertNotNull(result)
        assertEquals("Bills & Utilities", result?.category)
    }

    @Test
    fun testCategoryPrediction_Health() {
        val result = VoiceExpenseParser.parse("bought medicine for 250", mockContext)
        assertNotNull(result)
        assertEquals("Health & Wellness", result?.category)
    }

    @Test
    fun testCategoryPrediction_Groceries() {
        val result = VoiceExpenseParser.parse("spent 1500 on groceries", mockContext)
        assertNotNull(result)
        assertEquals("Groceries", result?.category)
    }

    @Test
    fun testCategoryPrediction_Gift() {
        val result = VoiceExpenseParser.parse("spent 500 on birthday gift", mockContext)
        assertNotNull(result)
        assertEquals("Gifts", result?.category)
    }

    @Test
    fun testCategoryPrediction_Stationery() {
        val result = VoiceExpenseParser.parse("paid 100 for pen and notebook", mockContext)
        assertNotNull(result)
        assertEquals("Stationery", result?.category)
    }

    @Test
    fun testCategoryPrediction_Miscellaneous() {
        val result = VoiceExpenseParser.parse("spent 100 on random stuff", mockContext)
        assertNotNull(result)
        assertEquals("Miscellaneous", result?.category)
    }

    // ==================== Merchant Detection Tests ====================

    @Test
    fun testMerchantDetection_Starbucks() {
        val result = VoiceExpenseParser.parse("spent 150 at starbucks", mockContext)
        assertNotNull(result)
        assertEquals("Starbucks", result?.merchant)
    }

    @Test
    fun testMerchantDetection_Uber() {
        val result = VoiceExpenseParser.parse("paid 200 for uber ride", mockContext)
        assertNotNull(result)
        assertEquals("Uber", result?.merchant)
    }

    @Test
    fun testMerchantDetection_NoMerchant() {
        val result = VoiceExpenseParser.parse("spent 100 on coffee", mockContext)
        assertNotNull(result)
        assertNull(result?.merchant)
    }

    // ==================== Complex Input Tests ====================

    @Test
    fun testComplexInput_LunchAtRestaurant() {
        val result = VoiceExpenseParser.parse("spent 500 for lunch at restaurant", mockContext)
        assertNotNull(result)
        assertEquals(500f, result?.amount)
        assertTrue(result?.description?.contains("lunch") == true)
        assertEquals("Food & Dining", result?.category)
    }

    @Test
    fun testComplexInput_MetroRecharge() {
        val result = VoiceExpenseParser.parse("metro card recharge 500", mockContext)
        assertNotNull(result)
        assertEquals(500f, result?.amount)
        assertEquals("Transport", result?.category)
    }

    @Test
    fun testComplexInput_PetrolExpense() {
        val result = VoiceExpenseParser.parse("petrol 2000", mockContext)
        assertNotNull(result)
        assertEquals(2000f, result?.amount)
        assertEquals("Transport", result?.category)
    }

    @Test
    fun testComplexInput_OnlineOrder() {
        val result = VoiceExpenseParser.parse("ordered pizza 600", mockContext)
        assertNotNull(result)
        assertEquals(600f, result?.amount)
        assertEquals("Food & Dining", result?.category)
    }

    // ==================== Edge Cases ====================

    @Test
    fun testEdgeCase_EmptyString() {
        val result = VoiceExpenseParser.parse("", mockContext)
        assertNull(result)
    }

    @Test
    fun testEdgeCase_OnlyAmount() {
        val result = VoiceExpenseParser.parse("100", mockContext)
        assertNotNull(result)
        assertEquals(100f, result?.amount)
    }

    @Test
    fun testEdgeCase_VeryLongDescription() {
        val result = VoiceExpenseParser.parse(
            "spent 500 on a very long description with many words about food items",
            mockContext
        )
        assertNotNull(result)
        assertEquals(500f, result?.amount)
    }

    @Test
    fun testEdgeCase_SpecialCharacters() {
        val result = VoiceExpenseParser.parse("spent ₹100 on coffee!", mockContext)
        assertNotNull(result)
        assertEquals(100f, result?.amount)
    }

    // ==================== Real-World Examples ====================

    @Test
    fun testRealWorld_IceCreamExample() {
        // Original requirement: "spent 100 on ice-cream"
        val result = VoiceExpenseParser.parse("spent 100 on ice cream", mockContext)
        assertNotNull(result)
        assertEquals(100f, result?.amount)
        assertEquals("Ice cream", result?.description)
        assertEquals("Food & Dining", result?.category)
    }

    @Test
    fun testRealWorld_CasualFormat() {
        val result = VoiceExpenseParser.parse("lunch 450", mockContext)
        assertNotNull(result)
        assertEquals(450f, result?.amount)
        assertEquals("Food & Dining", result?.category)
    }

    @Test
    fun testRealWorld_WithBrandName() {
        val result = VoiceExpenseParser.parse("swiggy order 600", mockContext)
        assertNotNull(result)
        assertEquals(600f, result?.amount)
        assertEquals("Food & Dining", result?.category)
    }

    // ==================== AI Training Tests ====================

    @Test
    fun testAITraining_CanTrainPattern() {
        // This just tests that training doesn't crash
        // Actual learning verification would need integration tests
        VoiceExpenseParser.trainPattern(mockContext, "test item", "Food & Dining")
        // If we get here without exception, test passes
        assertTrue(true)
    }

    @Test
    fun testGetSupportedCategories() {
        val categories = VoiceExpenseParser.getSupportedCategories()
        assertNotNull(categories)
        assertTrue(categories.isNotEmpty())
        assertTrue(categories.contains("Food & Dining"))
        assertTrue(categories.contains("Transport"))
        assertTrue(categories.contains("Miscellaneous"))
    }
}

/**
 * Integration Tests for Voice Input Helper
 * These require an Android device/emulator to run
 */
class VoiceInputHelperIntegrationTest {
    
    // Note: These tests require instrumentation testing
    // Run with: ./gradlew connectedAndroidTest
    
    @Test
    fun testSpeechRecognitionAvailability() {
        // This would test on actual device
        // val context = InstrumentationRegistry.getInstrumentation().targetContext
        // val isAvailable = VoiceInputHelper.isAvailable(context)
        // Device-dependent, so we can't assert true/false
    }
}
