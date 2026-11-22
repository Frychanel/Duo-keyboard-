# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class com.frychanel.duokeyboard.data.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class com.frychanel.duokeyboard.ime.DuoKeyboardService
