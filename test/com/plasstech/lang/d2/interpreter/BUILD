load("@google_bazel_common//testing:test_defs.bzl", "gen_java_tests")

gen_java_tests(
    name = "SmallTests",
    srcs = glob(["*.java"]),
    prefix_path = "test/",
    runtime_deps = [
        "@maven//:com_google_flogger_flogger_system_backend",
    ],
    deps = [
        "//src/com/plasstech/lang/d2:executor",
        "//src/com/plasstech/lang/d2:yet_another_compiler",
        "//src/com/plasstech/lang/d2/codegen/il",
        "//src/com/plasstech/lang/d2/common",
        "//src/com/plasstech/lang/d2/interpreter",
        "//src/com/plasstech/lang/d2/parse/node",
        "@maven//:com_google_guava_guava",
        "@maven//:com_google_testparameterinjector_test_parameter_injector",
        "@maven//:com_google_truth_truth",
        "@maven//:junit_junit",
    ],
)
