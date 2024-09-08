java_library(
    name = "testing",
    testonly = 1,
    srcs = glob(
        ["*.java"],
    ),
    visibility = ["//visibility:public"],
    deps = [
        "//src/com/plasstech/lang/d2/parse/node",
        "//src/com/plasstech/lang/d2/phase:phase_name",
        "//src/com/plasstech/lang/d2/phase/testing",
        "@maven//:com_google_truth_truth",
        "@maven//:junit_junit",
    ],
)
