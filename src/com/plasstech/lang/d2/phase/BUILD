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
    srcs = ["State.java"],
    deps = [
        ":auto", 
        "//src/com/plasstech/lang/d2/codegen/il",
        "//src/com/plasstech/lang/d2/common",
        "//src/com/plasstech/lang/d2/parse/node",
        "//src/com/plasstech/lang/d2/type",
        "//src/com/plasstech/lang/d2/type:symbols",
        "@maven//:com_google_code_findbugs_jsr305",
        "@maven//:com_google_guava_guava",
        "@maven//:com_google_auto_value_auto_value",
        "@maven//:com_google_auto_value_auto_value_annotations",
    ],
    visibility=["//visibility:public"],
)

java_library(
    name = "phase",
    srcs = ["Phase.java"],
    deps = [
        ":state", 
    ],
    visibility=["//visibility:public"],
)
