java_library(
    name = "testing",
    srcs = glob(
        ["*.java"],
    ),
    testonly=1,
    visibility = ["//visibility:public"],
    deps = [
        "//src/com/plasstech/lang/d2:executor",
        "//src/com/plasstech/lang/d2:yet_another_compiler",
        "//src/com/plasstech/lang/d2/codegen/il",  # for IL op
        "//src/com/plasstech/lang/d2/codegen/x64",
        "//src/com/plasstech/lang/d2/codegen/x64:x64assembler",
        "//src/com/plasstech/lang/d2/codegen/x64/optimize",
        "//src/com/plasstech/lang/d2/common",
        "//src/com/plasstech/lang/d2/interpreter",
        "//src/com/plasstech/lang/d2/parse/node",
        "//src/com/plasstech/lang/d2/phase:phase_name",
        "//src/com/plasstech/lang/d2/phase:state",
        "@maven//:com_google_guava_guava",
        "@maven//:com_google_truth_truth",
    ],
)
