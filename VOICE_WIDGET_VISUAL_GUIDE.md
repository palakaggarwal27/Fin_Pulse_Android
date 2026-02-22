# Voice Widget Visual Guide

## UI Layout

### Dashboard Screen with Voice Button

```
┌─────────────────────────────────┐
│  Fin-Pulse Dashboard            │
│                                 │
│  Good Evening, User    📊 👤   │
│                                 │
│  ┌───────────────────────────┐ │
│  │   Total Balance           │ │
│  │   ₹25,000                 │ │
│  └───────────────────────────┘ │
│                                 │
│  ┌──────────┐  ┌──────────┐   │
│  │ Digital  │  │  Cash    │   │
│  │ ₹20,000  │  │ ₹5,000   │   │
│  └──────────┘  └──────────┘   │
│                                 │
│  Recent Activity                │
│  ┌───────────────────────────┐ │
│  │ Coffee       -₹150        │ │
│  │ Food & Dining  2:30 PM    │ │
│  └───────────────────────────┘ │
│                                 │
│                                 │
│                          🎤     │ ← Voice Button (Blue)
│                          ⊕     │ ← Add Button (Green)
└─────────────────────────────────┘
```

### Voice Input Dialog

```
┌─────────────────────────────────┐
│  Voice Expense Logger           │
│                                 │
│         ┌───────────┐          │
│         │           │          │
│         │    🎤     │          │ ← Animated Mic Button
│         │           │          │   (Pulsing when listening)
│         └───────────┘          │
│                                 │
│   Listening... Speak now!       │
│                                 │
│   Try saying:                   │
│   • "Spent 100 on ice cream"    │
│   • "Paid 500 for lunch"        │
│   • "Bought coffee for 150"     │
│                                 │
│                    [Cancel]     │
└─────────────────────────────────┘
```

### Voice Confirmation Dialog

```
┌─────────────────────────────────┐
│  Confirm Expense                │
│                                 │
│  ┌───────────────────────────┐ │
│  │ Amount:                   │ │
│  │ 100                       │ │ ← Editable
│  └───────────────────────────┘ │
│                                 │
│  ┌───────────────────────────┐ │
│  │ Description:              │ │
│  │ Ice cream                 │ │ ← Editable
│  └───────────────────────────┘ │
│                                 │
│  ┌───────────────────────────┐ │
│  │ Food & Dining        ▼    │ │ ← Dropdown
│  └───────────────────────────┘ │
│                                 │
│  AI suggested: Food & Dining    │
│                                 │
│  [Cancel]      [Log Expense]    │
└─────────────────────────────────┘
```

## Button States

### Mic Button (Not Active)
```
    ┌─────┐
    │     │
    │ 🎤  │  Blue (#1E88E5)
    │     │  56dp x 56dp
    └─────┘
```

### Mic Button (Listening)
```
    ╱─────╲
   ╱       ╲
  │   🎤    │  Red (#FF4D4D)
   ╲       ╱  80dp x 80dp
    ╲─────╱   Pulsing animation
    (scale: 1.0 → 1.15)
```

### Add Button (Regular)
```
    ┌─────┐
    │     │
    │  ⊕  │  Green (#00FF85)
    │     │  64dp x 64dp
    └─────┘
```

## Color Scheme

### Primary Colors
- **Voice Button**: #1E88E5 (Material Blue)
- **Voice Button Active**: #FF4D4D (Red)
- **Add Button**: #00FF85 (FinPulse Emerald)
- **Background**: #0B0E11 (FinPulse Background)
- **Surface**: #161B22 (FinPulse Surface)

### Text Colors
- **Primary Text**: #FFFFFF (White)
- **Secondary Text**: rgba(255, 255, 255, 0.7)
- **Hint Text**: rgba(255, 255, 255, 0.4)
- **Success**: #00FF85 (FinPulse Emerald)
- **Error**: #FF4D4D (Red)

## Animation Details

### Pulsing Animation (When Listening)
```
Duration: 1000ms
Easing: FastOutSlowInEasing
Scale: 1.0 ↔ 1.15
Repeat: Infinite
Mode: Reverse
```

### Status Messages Flow
```
1. "Tap to speak..."
   ↓ (User taps mic)
2. "Ready for speech"
   ↓ (Permission granted)
3. "Listening... Speak now!"
   ↓ (User speaks)
4. "Got it! Processing..."
   ↓ (Speech ends)
5. "Processing your input..."
   ↓ (Parsing complete)
6. Shows confirmation dialog
```

## Permission Flow

```
First Time Use:
┌─────────────────────────────────┐
│  Allow Fin-Pulse to record      │
│  audio?                         │
│                                 │
│  🎤                             │
│                                 │
│  [Deny]        [Allow]          │
└─────────────────────────────────┘

If Denied:
┌─────────────────────────────────┐
│  Microphone Permission Required │
│                                 │
│  To log expenses by voice, we   │
│  need access to your microphone.│
│  Please grant the permission in │
│  app settings.                  │
│                                 │
│                      [OK]        │
└─────────────────────────────────┘
```

## User Flow Diagram

```
Dashboard
    │
    ├─ Tap Mic Button (🎤)
    │
    ├─ Voice Input Dialog Opens
    │   │
    │   ├─ Check Permission
    │   │   │
    │   │   ├─ Granted → Start Listening
    │   │   │
    │   │   └─ Denied → Request Permission
    │   │       │
    │   │       ├─ Granted → Start Listening
    │   │       │
    │   │       └─ Denied → Show Rationale
    │   │
    │   ├─ User Speaks
    │   │   "spent 100 on ice cream"
    │   │
    │   ├─ Speech to Text
    │   │   "spent 100 on ice cream"
    │   │
    │   └─ Parse with AI
    │       │
    │       ├─ Success → Close Dialog
    │       │
    │       └─ Failed → Show Error Toast
    │
    ├─ Confirmation Dialog Opens
    │   │
    │   ├─ Shows: ₹100, "Ice cream", "Food & Dining"
    │   │
    │   ├─ User Can Edit:
    │   │   - Amount
    │   │   - Description
    │   │   - Category
    │   │
    │   └─ User Taps "Log Expense"
    │       │
    │       ├─ If Category Changed
    │       │   → Train AI
    │       │
    │       └─ Add Expense
    │
    └─ Back to Dashboard
        │
        └─ Shows Success Toast
            "Expense logged successfully!"
```

## Voice Input Examples with Visual Output

### Example 1: Coffee Purchase
```
Voice: "spent 150 on coffee"

Confirmation Dialog:
┌─────────────────────────────────┐
│  Amount: 150                    │
│  Description: Coffee            │
│  Category: Food & Dining ✓      │
└─────────────────────────────────┘

Dashboard Entry:
┌─────────────────────────────────┐
│  Coffee              -₹150      │
│  Food & Dining    3:45 PM       │
└─────────────────────────────────┘
```

### Example 2: Uber Ride
```
Voice: "paid 200 for uber"

Confirmation Dialog:
┌─────────────────────────────────┐
│  Amount: 200                    │
│  Description: Uber              │
│  Category: Transport ✓          │
│  Merchant: Uber                 │
└─────────────────────────────────┘

Dashboard Entry:
┌─────────────────────────────────┐
│  Uber                -₹200      │
│  Transport        3:46 PM       │
└─────────────────────────────────┘
```

### Example 3: Grocery Shopping
```
Voice: "spent 1500 on groceries"

Confirmation Dialog:
┌─────────────────────────────────┐
│  Amount: 1500                   │
│  Description: Groceries         │
│  Category: Groceries ✓          │
└─────────────────────────────────┘

Dashboard Entry:
┌─────────────────────────────────┐
│  Groceries          -₹1,500     │
│  Groceries        3:47 PM       │
└─────────────────────────────────┘
```

## Accessibility Features

- **High Contrast**: White text on dark background
- **Large Touch Targets**: 56dp minimum (mic button)
- **Clear Status Messages**: Always shows what's happening
- **Error Handling**: Friendly error messages
- **Haptic Feedback**: Button taps provide feedback
- **Voice Feedback**: Could be added via TTS

## Responsive Design

### Phone (Portrait)
```
Buttons positioned bottom-right:
- Voice button: 16dp from right, 88dp from bottom
- Add button: 16dp from right, 16dp from bottom
- Spacing between: 16dp vertical gap
```

### Phone (Landscape)
```
Same positioning, scrollable dashboard content
```

### Tablet
```
Larger buttons (80dp for Add, 64dp for Voice)
More spacing (24dp gaps)
```

## Dark Theme Integration

All colors already follow dark theme:
- ✓ Dark background (#0B0E11)
- ✓ Dark surfaces (#161B22)
- ✓ Light text (White)
- ✓ Accent colors (Emerald, Blue)

## Future Enhancement Ideas

### Visual Improvements
1. **Waveform Animation**
   - Show audio waveform while listening
   - Real-time amplitude visualization

2. **Category Icons**
   - 🍔 Food & Dining
   - 🚗 Transport
   - 🛍️ Shopping
   - Add icons to confirmation dialog

3. **Voice History**
   - Show last 3 voice inputs
   - Quick retry option

4. **Confidence Indicator**
   - Show AI confidence level
   - Visual: ⭐⭐⭐⭐⭐ (5 stars)

### Interaction Improvements
1. **Long Press Voice Button**
   - Hold to record, release to process
   - Push-to-talk style

2. **Swipe to Edit**
   - Swipe right on entry to edit via voice
   - "Change coffee to lunch"

3. **Voice Feedback**
   - Text-to-speech confirmation
   - "Logged 150 rupees for coffee"

---

**This guide helps visualize the voice widget feature before building/running the app.**
