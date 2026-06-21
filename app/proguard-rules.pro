# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# DataStore / protobuf-lite: 防止 R8 移除 protobuf 生成的 value_ 字段
-keep class androidx.datastore.preferences.protobuf.** { *; }
-keepclassmembers class androidx.datastore.preferences.protobuf.** { *; }
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.MessageLite { *; }
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.MessageLiteOrBuilder { *; }
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite { *; }
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite$* { *; }

# 显式保留 protobuf 生成类中常用的反射字段名
-keepclassmembers class * {
    java.lang.Object value_;
    int value_;
    java.lang.Object unknownFields;
    int memoizedSerializedSize;
    int memoizedHashCode;
}

# Google protobuf（media3 等库可能依赖）
-keep class com.google.protobuf.** { *; }
-keepclassmembers class com.google.protobuf.** { *; }
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite { *; }
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite$* { *; }

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile