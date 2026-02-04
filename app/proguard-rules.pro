# ProGuard rules for app module

# Keep SMS receivers and services
-keep class com.technicallyrural.junction.app.receiver.** { *; }
-keep class com.technicallyrural.junction.app.service.** { *; }
