LOCATION_IMPL_SRCS = [
    "ConstantOperand.java",
    "MemoryAddress.java",
    "Location.java",
    "Operand.java",
    "ParamLocation.java",
    "StackLocation.java",
    "TempLocation.java",
    "VariableLocation.java",
]

LOCATION_SRCS = [
    "Location.java",
    "Operand.java",
]

java_library(
    name = "codegen",
    srcs = glob(
        ["*.java"],
        exclude = LOCATION_SRCS + LOCATION_IMPL_SRCS,
    ),
    visibility = ["//visibility:public"],
    deps = [
        ":auto",
        ":location",
        ":locations",
        "//src/com/plasstech/lang/d2/codegen/il",
        "//src/com/plasstech/lang/d2/common",
        "//src/com/plasstech/lang/d2/parse/node",
        "//src/com/plasstech/lang/d2/phase",
        "//src/com/plasstech/lang/d2/phase:state",
        "//src/com/plasstech/lang/d2/type",
        "//src/com/plasstech/lang/d2/type:exception",
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

java_library(
    name = "location",
    srcs = LOCATION_SRCS,
    visibility = ["//visibility:public"],
    deps = [
        "//src/com/plasstech/lang/d2/type:storage",
        "//src/com/plasstech/lang/d2/type:var_types",
        "@maven//:com_google_guava_guava",
    ],
)

java_library(
    name = "locations",
    srcs = LOCATION_IMPL_SRCS,
    visibility = ["//visibility:public"],
    deps = [
        ":location",
        "//src/com/plasstech/lang/d2/type:storage",
        "//src/com/plasstech/lang/d2/type:symbols",
        "//src/com/plasstech/lang/d2/type:var_types",
        "@maven//:com_google_guava_guava",
    ],
)

java_library(
    name = "trimmers",
    srcs = ["Trimmers.java"],
    visibility = ["//visibility:public"],
    deps = [
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
