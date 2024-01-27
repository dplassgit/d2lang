load("@google_bazel_common//testing:test_defs.bzl", "gen_java_tests")

gen_java_tests(
    name = "AllTests",
    srcs = glob(["*.java"]),
    prefix_path = "test/",
    deps = [
        "//src/com/plasstech/lang/d2/codegen/t100/testing",
        "@maven//:junit_junit",
    ],
)
