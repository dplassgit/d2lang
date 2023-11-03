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
    name="common",
    srcs=glob(["*.java"]),
    visibility=["//visibility:public"],
    deps=[
        ":auto",
        "//src/com/plasstech/lang/d2/phase:phase_name",
        "@maven//:com_github_pcj_google_options",
        "@maven//:com_google_code_findbugs_jsr305",
        "@maven//:com_google_guava_guava",
        "@maven//:com_google_auto_value_auto_value",
        "@maven//:com_google_auto_value_auto_value_annotations",
    ],
)
