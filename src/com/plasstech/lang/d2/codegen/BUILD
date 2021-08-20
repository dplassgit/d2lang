LOCATION_SRCS = [
  "ConstantOperand.java",
  "FieldSetAddress.java",
  "Location.java",
  "MemoryAddress.java",
  "Operand.java",
  "RegisterLocation.java",
  "StackLocation.java",
  "TempLocation.java",
]

java_library(
    name="codegen",
    srcs=glob(["*.java"], exclude=LOCATION_SRCS),
    deps=[
        ":location",
        "//src/com/plasstech/lang/d2/codegen/il",
        "//src/com/plasstech/lang/d2/common",
        "//src/com/plasstech/lang/d2/parse/node",
        "//src/com/plasstech/lang/d2/phase",
        "//src/com/plasstech/lang/d2/phase:state",
        "//src/com/plasstech/lang/d2/type",
        "//src/com/plasstech/lang/d2/type:storage",
        "//src/com/plasstech/lang/d2/type:symbols",
        "//src/com/plasstech/lang/d2/type:var_types",
        "@maven//:com_google_flogger_flogger",
        "@maven//:com_google_guava_guava",
    ],
    visibility=["//visibility:public"],
)

java_library(
    name="location",
    srcs=LOCATION_SRCS,
    visibility=["//visibility:public"],
    deps=[
        ":registers",
        "//src/com/plasstech/lang/d2/type:storage",
        "//src/com/plasstech/lang/d2/type:var_types",
        "@maven//:com_google_guava_guava",
    ],
)

java_library(
    name="registers",
    srcs=["Registers.java", "Register.java"],
    visibility=["//visibility:public"],
    deps=[
        "@maven//:com_google_guava_guava",
    ],
)
