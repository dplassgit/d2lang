VAR_TYPES = [
    "ArrayType.java",
    "SimpleType.java",
    "VarType.java",
]

java_library(name="type",
             srcs=glob(["*.java"], exclude=VAR_TYPES),
             deps=[
                 ":var_types",
                 "//src/com/plasstech/lang/d2/common",
                 "//src/com/plasstech/lang/d2/lex",
                 "//src/com/plasstech/lang/d2/parse/node",
                 "@maven//:com_google_guava_guava",
             ],
             visibility=["//visibility:public"],
             )

java_library(name="var_types",
             srcs=VAR_TYPES,
             visibility=["//visibility:public"],
             )
