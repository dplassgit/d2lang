REGISTERS_SRCS = [
    "IntRegister.java",
    "Registers.java",
    "Register.java",
    "RegistersInterface.java",
    "RegisterVisitor.java",
    "XmmRegister.java",
    "RegisterLocation.java",
]

java_library(
    name = "x64",
    srcs = glob(
        ["*.java"],
        exclude = REGISTERS_SRCS,
    ),
    visibility = ["//visibility:public"],
    deps = [
        ":auto",
        ":registers",
        "//src/com/plasstech/lang/d2/codegen",  # for emitter, etc.
        "//src/com/plasstech/lang/d2/codegen:location",  # for locaiton, operand
        "//src/com/plasstech/lang/d2/codegen:locations",  # for ConstantOperand, ParamLocatoin, etc.
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

java_library(
    name = "registers",
    srcs = REGISTERS_SRCS,
    visibility = ["//visibility:public"],
    deps = [
        "//src/com/plasstech/lang/d2/codegen:location",
        "//src/com/plasstech/lang/d2/common",
        "//src/com/plasstech/lang/d2/type:storage",
        "//src/com/plasstech/lang/d2/type:var_types",
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
