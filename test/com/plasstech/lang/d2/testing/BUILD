java_library(
    name = "test_utils",
    srcs = ["TestUtils.java"],
    visibility = ["//visibility:public"],
    deps = [
        "//src/com/plasstech/lang/d2:executor",
        "//src/com/plasstech/lang/d2:yet_another_compiler",
        "//src/com/plasstech/lang/d2/codegen/il",
        "//src/com/plasstech/lang/d2/common",
        "//src/com/plasstech/lang/d2/interpreter",
        "//src/com/plasstech/lang/d2/optimize",
        "//src/com/plasstech/lang/d2/parse/node",
        "//src/com/plasstech/lang/d2/phase:state",
        "@maven//:com_google_guava_guava",
        "@maven//:com_google_truth_truth",
        "@maven//:junit_junit",
    ],
)

java_library(
    name = "testing",
    srcs = ["VarTypeSubject.java"],
    visibility = ["//visibility:public"],
    deps = [
        "//src/com/plasstech/lang/d2/type:var_types",
        "@maven//:com_google_code_findbugs_jsr305",
        "@maven//:com_google_truth_truth",
    ],
)
