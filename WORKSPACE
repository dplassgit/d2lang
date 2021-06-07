workspace(name = "d2lang")
load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

# In Bazel 2.0, Maven rules provided via new rules_jvm_external.
RULES_JVM_EXTERNAL_TAG = "4.1"
RULES_JVM_EXTERNAL_SHA = "f36441aa876c4f6427bfb2d1f2d723b48e9d930b62662bf723ddfb8fc80f0140"

http_archive(
    name = "rules_jvm_external",
    strip_prefix = "rules_jvm_external-%s" % RULES_JVM_EXTERNAL_TAG,
    sha256 = RULES_JVM_EXTERNAL_SHA,
    url = "https://github.com/bazelbuild/rules_jvm_external/archive/%s.zip" % RULES_JVM_EXTERNAL_TAG,
)
load("@rules_jvm_external//:defs.bzl", "maven_install")

maven_install(
    artifacts = [
        "junit:junit:4.13.2",
        "com.google.guava:guava:30.1.1-jre",
        "com.google.flogger:flogger:0.6",
        "com.google.flogger:flogger-system-backend:0.6",
        "com.google.truth:truth:1.1.3",
        "com.github.pcj:google-options:jar:1.0.0",
        "com.google.truth.extensions:truth-java8-extension:1.1.3",
    ],
    repositories = [
        "https://maven.google.com",
        "https://repo1.maven.org/maven2",
        "https://jcenter.bintray.com",
    ],
    fetch_sources=True,
    fetch_javadoc=True,
)

load("@bazel_tools//tools/build_defs/repo:git.bzl", "git_repository")

git_repository(
    name = "google_bazel_common",
    commit = "9d1beb9294151cb1b28cd4b4dc842fd7559f9147",
    remote = "git://github.com/google/bazel-common.git",
)

