# Regras do NewPipeExtractor (Rhino)
-keep class org.mozilla.javascript.** { *; }
-keep class org.mozilla.classfile.ClassFileWriter
-dontwarn org.mozilla.javascript.tools.**

# Etapa 3: Media3 já traz as próprias regras de consumidor; nada extra necessário.
