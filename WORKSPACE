load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

RULES_JVM_EXTERNAL_TAG = "3.0"
RULES_JVM_EXTERNAL_SHA = "62133c125bf4109dfd9d2af64830208356ce4ef8b165a6ef15bbff7460b35c3a"

http_archive(
    name = "rules_jvm_external",
    strip_prefix = "rules_jvm_external-%s" % RULES_JVM_EXTERNAL_TAG,
    sha256 = RULES_JVM_EXTERNAL_SHA,
    url = "https://github.com/bazelbuild/rules_jvm_external/archive/%s.zip" % RULES_JVM_EXTERNAL_TAG,
)

load("@rules_jvm_external//:defs.bzl", "maven_install")

maven_install(
   artifacts = [
    "com.google.flogger:flogger:0.3.1",
    "com.google.guava:guava:27.1-jre",
    "com.google.truth:truth:0.45",
    "com.googlecode.java-diff-utils:diffutils:1.3.0",
    "commons-codec:commons-codec:jar:1.11",
    "junit:junit:4.13",
   ],
   repositories = [
        "https://maven.google.com",
        "https://repo1.maven.org/maven2",
    ],
)
