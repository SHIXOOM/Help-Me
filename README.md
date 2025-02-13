# Help Me
A mini project to make an app that can block nudity and explicit content from your device. Not blocking them directly but more of detecting them on your screen and deciding the best course of action.  

I built an image classifier using PyTorch. I used MobileNet V3 as a backbone for my classifier to utilize its power and time efficiency for inference on portable devices and smartphones.  

I then used GitHub Copilot to get GPT-o3 to help me on building the Android app, as I am not skilled enough in using Kotlin or in Android development.

This application is still in early stages, whether the classifier performance (It has 96% accuracy on test set, but detection from mobile screens will be a bit worse initially), or the application performance itself.
This app works completely offline, so your data is completely safe and private, no one has any access to what you do on your device.
