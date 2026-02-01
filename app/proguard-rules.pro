# ProGuard rules for app module

# Keep SMS receivers and services
-keep class com.example.messaging.app.receiver.** { *; }
-keep class com.example.messaging.app.service.** { *; }
