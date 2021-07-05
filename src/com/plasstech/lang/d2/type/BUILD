VAR_TYPE_FILES = [
    "ArrayType.java",
    "RecordReferenceType.java",
    "SimpleType.java",
    "VarType.java",
]

STORAGE_FILES = [
    "SymbolStorage.java",
]

java_library(name="type",
             srcs=glob(["*.java"], exclude=VAR_TYPE_FILES + STORAGE_FILES),
             deps=[
                 ":storage",
                 ":var_types",
                 "//src/com/plasstech/lang/d2/common",
                 "//src/com/plasstech/lang/d2/lex",
                 "//src/com/plasstech/lang/d2/parse/node",
                 "@maven//:com_google_guava_guava",
             ],
             visibility=["//visibility:public"],
             )

java_library(name="var_types",
             srcs=VAR_TYPE_FILES,
             visibility=["//visibility:public"],
             )

java_library(name="storage",
             srcs=STORAGE_FILES,
             visibility=["//visibility:public"],
             )
