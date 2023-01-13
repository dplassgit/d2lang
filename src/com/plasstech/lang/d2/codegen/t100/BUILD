java_library(
    name = "t100",
    srcs = glob(["*.java"]),
    visibility = ["//visibility:public"],
    deps = [
        ":auto",
        "//src/com/plasstech/lang/d2/codegen",  # for emitter, etc.
        "//src/com/plasstech/lang/d2/codegen:location",  # for locaiton, etc.
        "//src/com/plasstech/lang/d2/codegen:locations",  # for locaiton, etc.
        "//src/com/plasstech/lang/d2/codegen/il",  # for IL ops
        "//src/com/plasstech/lang/d2/common",
        "//src/com/plasstech/lang/d2/parse/node",
        "//src/com/plasstech/lang/d2/phase",
        "//src/com/plasstech/lang/d2/phase:state",
        "//src/com/plasstech/lang/d2/type",
        "//src/com/plasstech/lang/d2/type:storage",
        "//src/com/plasstech/lang/d2/type:symbols",
        "//src/com/plasstech/lang/d2/type:var_types",
        "@maven//:com_google_auto_value_auto_value",
        "@maven//:com_google_auto_value_auto_value_annotations",
        "@maven//:com_google_code_findbugs_jsr305",
        "@maven//:com_google_flogger_flogger",
        "@maven//:com_google_guava_guava",
    ],
)

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
