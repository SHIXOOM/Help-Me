# Help Me  

**Help Me** is a mini-project designed to detect and prevent access to nudity or explicit content on your device.  

## Overview  
This Android application leverages a custom image classifier built with **PyTorch**. The classifier uses **MobileNet V3** as its backbone, optimizing for both **power efficiency** and **real-time inference** on portable devices.  

Since I am not highly experienced in Kotlin or Android development, I utilized **GitHub Copilot** (GPT-o3) to assist in building the Android app.  

## How It Works  
1. The app takes a **screenshot** of your device every **10–20 seconds**.  
2. The screenshot is **processed through the classifier** to check for explicit content.  
3. If explicit content is detected, the app **automatically returns your device to the home screen**.  
4. The process **repeats continuously**.  

## Current Status  
🚀 **Early-stage development:**  
- **Classifier Performance:** Achieves **96% accuracy** on the test set, but detection accuracy on mobile screens may initially be lower.  
- **App Performance:** Optimizations are ongoing for **efficiency and accuracy**.  

🔒 **Privacy & Security:**  
- The app operates **entirely offline**, ensuring **complete privacy**.  
- No data is transmitted or stored externally—your activity remains **100% private**.  

---  
Please let me know if you would like to contribute to this app in any way and stay tuned for updates and improvements!
