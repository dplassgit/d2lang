REGISTERS_SRCS = [
  "IntRegister.java",
  "X64Registers.java",
  #"X64RegisterVisitor.java",
  "XmmRegister.java",
]

java_library(
    name="registers",
    srcs=REGISTERS_SRCS,
    visibility=["//visibility:public"],
    deps=[
        "//src/com/plasstech/lang/d2/common",
        "//src/com/plasstech/lang/d2/codegen:registers",
        "//src/com/plasstech/lang/d2/type:var_types",
        "@maven//:com_google_guava_guava",
    ],
)

