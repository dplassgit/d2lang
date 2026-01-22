java_library(
    name = "testing",
    testonly = 1,
    srcs = ["OptimizerSubject.java"],
    visibility = ["//visibility:public"],
    deps = [
        "//src/com/plasstech/lang/d2/optimize",
        "@maven//:com_google_truth_truth",
        "@maven//:com_google_guava_guava",
    ],
)

java_library(
    name = "optimizer",
    testonly = 1,
    srcs = ["OptimizerWithNop.java"],
    visibility = ["//visibility:public"],
    deps = [
        "//src/com/plasstech/lang/d2/codegen/il",
        "//src/com/plasstech/lang/d2/optimize",
        "//src/com/plasstech/lang/d2/type:symbols",
        "@maven//:com_google_guava_guava",
    ],
)
