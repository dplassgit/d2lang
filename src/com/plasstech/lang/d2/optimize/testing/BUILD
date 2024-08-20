java_library(
    name = "testing",
    testonly = 1,
    srcs = glob(
        ["*.java"],
    ),
    visibility = ["//visibility:public"],
    deps = [
        "//src/com/plasstech/lang/d2:executor",
        "//src/com/plasstech/lang/d2/codegen/il",
        "//src/com/plasstech/lang/d2/interpreter",
        "//src/com/plasstech/lang/d2/optimize",
        "//src/com/plasstech/lang/d2/parse/node",
        "//src/com/plasstech/lang/d2/phase:phase_name",
        "//src/com/plasstech/lang/d2/phase:state",
        "@maven//:com_google_guava_guava",
        "@maven//:com_google_truth_truth",
    ],
)
