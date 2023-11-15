java_plugin(
    name = "auto_plugin",
    processor_class = "com.google.auto.value.processor.AutoValueProcessor",
    deps = ["@maven//:com_google_auto_value_auto_value"],
)

java_library(
    name = "auto",
    exported_plugins = [":auto_plugin"],
    exports = ["@maven//:com_google_auto_value_auto_value"],
)

java_library(
    name = "state",
    srcs = [
        "State.java",
    ],
    visibility = ["//visibility:public"],
    deps = [
        ":auto",
        ":errors",
        "//src/com/plasstech/lang/d2/codegen/il",
        "//src/com/plasstech/lang/d2/common",
        "//src/com/plasstech/lang/d2/parse/node",
        "//src/com/plasstech/lang/d2/type:result",
        "//src/com/plasstech/lang/d2/type:symbols",
        "@maven//:com_google_auto_value_auto_value",
        "@maven//:com_google_auto_value_auto_value_annotations",
        "@maven//:com_google_code_findbugs_jsr305",
        "@maven//:com_google_guava_guava",
    ],
)

java_library(
    name = "phase",
    srcs = ["Phase.java"],
    visibility = ["//visibility:public"],
    deps = [
        ":state",
    ],
)

java_library(
    name = "phase_name",
    srcs = ["PhaseName.java"],
    visibility = ["//visibility:public"],
)

java_library(
    name = "errors",
    srcs = [
        "Errors.java",
    ],
    visibility = ["//visibility:public"],
    deps = [
        "//src/com/plasstech/lang/d2/common",
        "@maven//:com_google_guava_guava",
    ],
)
