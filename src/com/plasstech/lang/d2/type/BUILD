java_library(
    name = "type",
    srcs = [
        "ProcGatherer.java",
        "RecordGatherer.java",
        "RecordTypeAssigner.java",
        "StaticChecker.java",
    ],
    visibility = ["//visibility:public"],
    deps = [
        ":exception",
        ":result",
        ":storage",
        ":symbols",
        ":var_types",
        "//src/com/plasstech/lang/d2/common",
        "//src/com/plasstech/lang/d2/parse/node",
        "//src/com/plasstech/lang/d2/phase",
        "//src/com/plasstech/lang/d2/phase:errors",
        "//src/com/plasstech/lang/d2/phase:state",
        "@maven//:com_google_guava_guava",
    ],
)

java_library(
    name = "result",
    srcs = [
        "TypeCheckResult.java",
    ],
    visibility = ["//visibility:public"],
    deps = [
        ":symbols",
        "//src/com/plasstech/lang/d2/common",
        "//src/com/plasstech/lang/d2/phase:errors",
        "@maven//:com_google_guava_guava",
    ],
)

java_library(
    name = "symbols",
    srcs = glob(["*Symbol.java"]) + [
        "SymTab.java",
        "SymbolTable.java",
    ],
    visibility = ["//visibility:public"],
    deps = [
        ":exception",
        ":storage",
        ":var_types",
        "//src/com/plasstech/lang/d2/common",
        "//src/com/plasstech/lang/d2/parse/node",
        "@maven//:com_google_guava_guava",
    ],
)

java_library(
    name = "var_types",
    srcs = [
        "ArrayType.java",
        "DefaultVarType.java",
        "NullType.java",
        "PointerType.java",
        "RecordReferenceType.java",
        "SimpleType.java",
        "StringType.java",
        "UnknownType.java",
        "VarType.java",
        "VarTypeRegistry.java",
    ],
    visibility = ["//visibility:public"],
    deps = [
        "//src/com/plasstech/lang/d2/common",
        "@maven//:com_google_guava_guava",
    ],
)

java_library(
    name = "print_formats",
    srcs = [
        "PrintFormats.java",
    ],
    visibility = ["//visibility:public"],
    deps = [
       ":var_types",
        "@maven//:com_google_guava_guava",
    ],
)

java_library(
    name = "storage",
    srcs = ["SymbolStorage.java"],
    visibility = ["//visibility:public"],
)

java_library(
    name = "exception",
    srcs = ["TypeException.java"],
    visibility = ["//visibility:public"],
    deps = [
        "//src/com/plasstech/lang/d2/common",
    ],
)
