# =============================================================================
# proguard-rules.pro
#
# R8/ProGuard keep-rules for the RELEASE build type. These rules only take
# effect when isMinifyEnabled = true (currently false in build.gradle.kts, so
# this file is unused at the moment — it exists so the release block is wired
# correctly and so students see WHERE shrinking rules would live).
#
# When you enable minification for a real release, add rules here to keep any
# classes/members that R8 cannot see are used (e.g. reflection, JNI, serialized
# model classes). Example:
#
#   -keep class com.example.appreleasebasics.model.** { *; }
#
# Add project-specific keep rules below this line.
# =============================================================================
