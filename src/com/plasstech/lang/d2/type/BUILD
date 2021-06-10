java_library(name="type",
             srcs=glob(["*.java"], exclude=["ArrayType.java", "SimpleType.java", "VarType.java"]),
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
             srcs=["ArrayType.java", "SimpleType.java", "VarType.java"],
             visibility=["//visibility:public"],
             )
