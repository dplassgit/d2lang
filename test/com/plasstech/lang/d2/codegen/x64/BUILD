load("@google_bazel_common//testing:test_defs.bzl", "gen_java_tests")

gen_java_tests(name="AllTests",
    srcs=glob(["*.java"],
        exclude=[
            "GoldensTest.java",
        ]),
    prefix_path="test/",
    deps=[
        "//src/com/plasstech/lang/d2/codegen",
        "//src/com/plasstech/lang/d2/codegen/il",
        "//src/com/plasstech/lang/d2/codegen:location",
        "//src/com/plasstech/lang/d2/codegen:locations",
        "//src/com/plasstech/lang/d2/codegen/testing", # for EmitterSubject
        "//src/com/plasstech/lang/d2/codegen/x64",
        "//src/com/plasstech/lang/d2/codegen/x64:registers",
        "//src/com/plasstech/lang/d2/common",
        "//src/com/plasstech/lang/d2:executor",
        "//src/com/plasstech/lang/d2/interpreter",
        "//src/com/plasstech/lang/d2/lex",
        "//src/com/plasstech/lang/d2/optimize",
        "//src/com/plasstech/lang/d2/parse",
        "//src/com/plasstech/lang/d2/parse/node",
        "//src/com/plasstech/lang/d2/phase:state",
        "//src/com/plasstech/lang/d2/type",
        "//src/com/plasstech/lang/d2/type:result",
        "//src/com/plasstech/lang/d2/type:symbols",
        "//src/com/plasstech/lang/d2/type:var_types",
        "//test/com/plasstech/lang/d2/testing:test_utils",
        "@maven//:com_google_guava_guava",
        "@maven//:com_google_testparameterinjector_test_parameter_injector",
        "@maven//:com_google_truth_truth",
        "@maven//:junit_junit",
    ],
    runtime_deps=[
        "@maven//:com_google_flogger_flogger_system_backend",
    ],
)
