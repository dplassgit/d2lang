load("@google_bazel_common//testing:test_defs.bzl", "gen_java_tests")

gen_java_tests(
    name = "AllTests",
    srcs = glob(
        ["*.java"],
    ),
    prefix_path = "test/",
    runtime_deps = [
        "@maven//:com_google_flogger_flogger_system_backend",
    ],
    deps = [
        "//src/com/plasstech/lang/d2/codegen/x64/optimize",
        "//src/com/plasstech/lang/d2/phase",
        "//src/com/plasstech/lang/d2/phase:state",
        "@maven//:com_google_guava_guava",
        "@maven//:com_google_testparameterinjector_test_parameter_injector",
        "@maven//:com_google_truth_truth",
        "@maven//:junit_junit",
    ],
)
