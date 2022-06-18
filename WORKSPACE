workspace(name = "d2lang")
load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

# In Bazel 2.0, Maven rules provided via new rules_jvm_external.

# Get this from https://github.com/bazelbuild/rules_jvm_external/releases
RULES_JVM_EXTERNAL_TAG = "4.2"
RULES_JVM_EXTERNAL_SHA = "cd1a77b7b02e8e008439ca76fd34f5b07aecb8c752961f9640dea15e9e5ba1ca"

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
        "com.github.pcj:google-options:jar:1.0.0",
        "com.google.auto.value:auto-value-annotations:1.8.1",
        "com.google.auto.value:auto-value:1.9",
        "com.google.code.findbugs:jsr305:3.0.2",
        "com.google.guava:guava:31.1-jre",
        "com.google.flogger:flogger:0.7.4",
        "com.google.flogger:flogger-system-backend:0.7.4",
        "com.google.truth:truth:1.1.3",
        "com.google.truth.extensions:truth-java8-extension:1.1.3",
        "com.google.testparameterinjector:test-parameter-injector:1.0",
    ],
    repositories = [
        "https://maven.google.com",
        "https://repo1.maven.org/maven2",
        "https://jcenter.bintray.com",
    ],
    fetch_sources=True,
    fetch_javadoc=True,
)


load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

# See https://github.com/google/bazel-common for how to update these values
http_archive(
  name = "google_bazel_common",
  sha256 = "b54410b99dd34e17dc02fc6186d478828b0d34be3876769dba338c6ccec2cea9",
  strip_prefix = "bazel-common-221ecf2922e8ebdf8e002130e9772045cfa2f464",
  urls = ["https://github.com/google/bazel-common/archive/221ecf2922e8ebdf8e002130e9772045cfa2f464.zip"],
)


load("@google_bazel_common//:workspace_defs.bzl", "google_common_workspace_rules")

google_common_workspace_rules()
