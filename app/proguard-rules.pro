# OkHttp and org.json are handled by their published consumer rules.
# Keep token and Google credential classes out of reflection-sensitive transformations.
-keep class com.google.android.libraries.identity.googleid.** { *; }
