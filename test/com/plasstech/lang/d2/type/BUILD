load("@google_bazel_common//testing:test_defs.bzl", "gen_java_tests")

gen_java_tests(
    name = "SmallTests",
    srcs = glob(["*.java"]),
    prefix_path = "test/",
    deps = [
        "//src/com/plasstech/lang/d2:yet_another_compiler",
        "//src/com/plasstech/lang/d2/common",
        "//src/com/plasstech/lang/d2/parse/node",
        "//src/com/plasstech/lang/d2/phase:phase_name",
        "//src/com/plasstech/lang/d2/phase:state",
        "//src/com/plasstech/lang/d2/type:symbols",
        "//src/com/plasstech/lang/d2/type:var_types",
        "//test/com/plasstech/lang/d2/testing",
        "@maven//:com_google_testparameterinjector_test_parameter_injector",
        "@maven//:com_google_truth_truth",
        "@maven//:junit_junit",
    ],
)
