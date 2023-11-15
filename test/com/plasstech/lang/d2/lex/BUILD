load("@google_bazel_common//testing:test_defs.bzl", "gen_java_tests")

gen_java_tests(
    name = "SmallTests",
    srcs = glob(["*.java"]),
    prefix_path = "test/",
    deps = [
        "//src/com/plasstech/lang/d2/common",
        "//src/com/plasstech/lang/d2/lex",
        "@maven//:com_google_truth_truth",
        "@maven//:junit_junit",
    ],
)
