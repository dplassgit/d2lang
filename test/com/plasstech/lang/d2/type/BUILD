load("@google_bazel_common//testing:test_defs.bzl", "gen_java_tests")

gen_java_tests(name="SmallTests",
    srcs=glob(["*.java"],),
    prefix_path="test/",
	  deps=[
        "//src/com/plasstech/lang/d2/common",
        "//src/com/plasstech/lang/d2/lex",
        "//src/com/plasstech/lang/d2/parse",
        "//src/com/plasstech/lang/d2/parse/node",
        "//src/com/plasstech/lang/d2/phase:state",
        "//src/com/plasstech/lang/d2/type",
        "//src/com/plasstech/lang/d2/type:symbols",
        "//src/com/plasstech/lang/d2/type:var_types",
        "@maven//:com_google_testparameterinjector_test_parameter_injector",
        "@maven//:com_google_truth_truth",
        "@maven//:junit_junit",
    ],
)
