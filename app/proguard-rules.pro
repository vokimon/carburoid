# Keep all symbol names for the stacktrace to be meaningful
-keep class * { *; }

# Suppress warning about javax.lang.model.element.Modifier.
# See https://github.com/google/error-prone/issues/2122
-dontwarn javax.lang.model.element.Modifier
