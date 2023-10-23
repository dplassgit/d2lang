load("@google_bazel_common//testing:test_defs.bzl", "gen_java_tests")

gen_java_tests(name="AllTests",
    srcs=glob(["*.java"],),
    prefix_path="test/",
    deps=[
        "//src/com/plasstech/lang/d2/codegen",
        "//src/com/plasstech/lang/d2/codegen/il",
        "//src/com/plasstech/lang/d2/codegen:locations",
        "//src/com/plasstech/lang/d2/common",
        "//src/com/plasstech/lang/d2:executor",
        "//src/com/plasstech/lang/d2/interpreter",
        "//src/com/plasstech/lang/d2/lex",
        "//src/com/plasstech/lang/d2/parse",
        "//src/com/plasstech/lang/d2/parse/node",
        "//src/com/plasstech/lang/d2/phase:state",
        "//src/com/plasstech/lang/d2/type",
        "//src/com/plasstech/lang/d2/type:result",
        "//src/com/plasstech/lang/d2/type:var_types",
        "@maven//:com_google_guava_guava",
        "@maven//:com_google_truth_truth",
        "@maven//:junit_junit",
    ],
    runtime_deps=[
        "@maven//:com_google_flogger_flogger_system_backend",
    ],
)
