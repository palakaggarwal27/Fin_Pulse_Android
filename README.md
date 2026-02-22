# Fin-Pulse: Luxury Finance Tracking

Fin-Pulse is a minimalist, high-performance finance manager built with Jetpack Compose. It aims to provide a zero-friction experience for tracking expenses and income using automation and AI.

## 🚀 Key Features

- **Luxury Dark Theme:** An emerald-on-black interface designed for high-end usability.
- **Smart Notification Listener:** Automatically detects transactions from payment apps like PhonePe, GPay, and Paytm.
- **AI-Powered Bubble:** When a transaction is detected, a non-intrusive bubble appears for 1-tap confirmation.
- **AI Training Lab:** A dedicated space where you can teach the app how to read your specific notifications and map UPI IDs to names.
- **Voice Logging:** Long-press the add button or use the Mic to log expenses via voice commands.
- **Persistent AI Brain:** Ships with pre-trained patterns to recognize common Indian banking and UPI messages out of the box.
- **Monitored Apps Control:** Users can choose exactly which apps Fin-Pulse should listen to (e.g., enable GPay, disable WhatsApp).

## ⚠️ Current Status & Limitations

**Note:** This application is currently in **Active Debug/Development** phase.

- **Permissions Required:** The app requires SMS, Notification Access, and Overlay permissions to function. 
- **Parsing Accuracy:** While getting better, the parser may occasionally fail on highly non-standard message formats.
- **Device Persistence:** Currently, profile images are stored as URIs. On some Android versions, these URIs may lose permission after a device reboot (Fix in progress).
- **Manual Corrections:** If the AI guesses wrong (e.g., Spent vs Received), use the AI Training Lab to correct it.

## 🛠 Future Scope

- **Cloud Sync:** Synchronize transactions across multiple devices securely.
- **Advanced Analytics:** Detailed monthly reports with budget forecasting and savings goals.
- **Bank Statement Import:** Support for PDF/CSV statement imports for historical data.
- **Investment Tracking:** Add support for tracking Mutual Funds, Stocks, and Gold.
- **Recurring Transactions:** Automatic logging for monthly rent, subscriptions, and SIPs.

## 🧪 AI Lab Usage

To improve the app for yourself and others:
1. Paste a sample message into the **AI Training Lab**.
2. If it detects incorrectly, use the **Train AI** button to fix the "True/False" status or the "Income/Spend" direction.
3. The app will remember your choice for all future messages of that format.

---
*Created by Avinya - Intelligent Finance for the Modern World.*
