# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
-renamesourcefileattribute SourceFile

-keepattributes Signature
-keep class kotlin.coroutines.Continuation

-keep class io.github.a13e300.ro_tieba.datastore.* { *; }
-keep class tbclient.** { *; }
-keep class io.github.a13e300.ro_tieba.api.web.* { *; }
-keep class io.github.a13e300.ro_tieba.api.json.* { *; }
-keep interface io.github.a13e300.ro_tieba.api.TiebaJsonAPI { *; }
-keep interface io.github.a13e300.ro_tieba.api.TiebaWebAPI { *; }
-keep interface io.github.a13e300.ro_tieba.api.TiebaProtobufAPI { *; }
-keep, allowobfuscation class io.github.a13e300.ro_tieba.api.adapters.*
