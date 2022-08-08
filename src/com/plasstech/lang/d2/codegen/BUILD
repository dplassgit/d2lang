LOCATION_SRCS = [
  "ConstantOperand.java",
  "Location.java",
  "MemoryAddress.java",
  "Operand.java",
  "ParamLocation.java",
  "RegisterLocation.java",
  "StackLocation.java",
  "TempLocation.java",
]

REGISTERS_SRCS = [
  "IntRegister.java",
  "MmxRegister.java",
  "Registers.java",
  "Register.java",
  "RegistersInterface.java",
  "RegisterVisitor.java",
]

java_library(
    name="codegen",
    srcs=glob(["*.java"], exclude=LOCATION_SRCS + REGISTERS_SRCS),
    deps=[
        ":location",
        ":registers",
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
    srcs=REGISTERS_SRCS,
    visibility=["//visibility:public"],
    deps=[
        "//src/com/plasstech/lang/d2/common",
        "//src/com/plasstech/lang/d2/type:var_types",
        "@maven//:com_google_guava_guava",
    ],
)
