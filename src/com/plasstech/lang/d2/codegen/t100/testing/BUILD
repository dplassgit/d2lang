java_library(
    name = "testing",
    srcs = ["TestUtils.java"],
    testonly = True,
    visibility = ["//visibility:public"],
    deps = [
        "//src/com/plasstech/lang/d2:executor",
        "//src/com/plasstech/lang/d2/codegen/t100",
        "//src/com/plasstech/lang/d2/codegen/testing",
        "//src/com/plasstech/lang/d2/common",
        "//src/com/plasstech/lang/d2/interpreter",
        "//src/com/plasstech/lang/d2/phase",
        "//src/com/plasstech/lang/d2/phase:phase_name",
        "//src/com/plasstech/lang/d2/phase:state",
        "@maven//:com_google_guava_guava",
        "@maven//:com_google_truth_truth",
    ],
)
