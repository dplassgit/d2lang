workspace(name = "d2lang")

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

# Get this from https://github.com/bazelbuild/rules_jvm_external/releases
RULES_JVM_EXTERNAL_TAG = "5.3"
RULES_JVM_EXTERNAL_SHA ="d31e369b854322ca5098ea12c69d7175ded971435e55c18dd9dd5f29cc5249ac"

http_archive(
    name = "rules_jvm_external",
    strip_prefix = "rules_jvm_external-%s" % RULES_JVM_EXTERNAL_TAG,
    sha256 = RULES_JVM_EXTERNAL_SHA,
    url = "https://github.com/bazelbuild/rules_jvm_external/releases/download/%s/rules_jvm_external-%s.tar.gz" % (RULES_JVM_EXTERNAL_TAG, RULES_JVM_EXTERNAL_TAG)
)

load("@rules_jvm_external//:repositories.bzl", "rules_jvm_external_deps")
rules_jvm_external_deps()

load("@rules_jvm_external//:setup.bzl", "rules_jvm_external_setup")
rules_jvm_external_setup()


load("@rules_jvm_external//:defs.bzl", "maven_install")

# These are at https://repo1.maven.org/maven2
maven_install(
    artifacts = [
        "junit:junit:4.13.2",
        "com.github.pcj:google-options:jar:1.0.0",
        "com.google.auto.value:auto-value-annotations:1.9",
        "com.google.auto.value:auto-value:1.9",
        "com.google.code.findbugs:jsr305:3.0.2",
        "com.google.guava:guava:32.1.3-jre",
        "com.google.flogger:flogger:0.8",
        "com.google.flogger:flogger-system-backend:0.8",
        "com.google.truth:truth:1.1.5",
        "com.google.truth.extensions:truth-java8-extension:1.1.5",
        "com.google.testparameterinjector:test-parameter-injector:1.14",
    ],
    fetch_javadoc = True,
    fetch_sources = True,
    repositories = [
        "https://repo1.maven.org/maven2",
    ],
)

# See https://github.com/google/bazel-common for how to update these values
http_archive(
    name = "google_bazel_common",
    sha256 = "b54410b99dd34e17dc02fc6186d478828b0d34be3876769dba338c6ccec2cea9",
    strip_prefix = "bazel-common-221ecf2922e8ebdf8e002130e9772045cfa2f464",
    urls = ["https://github.com/google/bazel-common/archive/221ecf2922e8ebdf8e002130e9772045cfa2f464.zip"],
)

load("@google_bazel_common//:workspace_defs.bzl", "google_common_workspace_rules")

google_common_workspace_rules()
