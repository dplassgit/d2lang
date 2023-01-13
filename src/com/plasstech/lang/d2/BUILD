java_binary(
    name = "D2Compiler",
    srcs = ["D2Compiler.java"],
    runtime_deps = [
        "@maven//:com_google_flogger_flogger_system_backend",
    ],
    deps = [
        ":yet_another_compiler",
        "//src/com/plasstech/lang/d2/codegen/t100",
        "//src/com/plasstech/lang/d2/codegen/x64",
        "//src/com/plasstech/lang/d2/codegen/x64/optimize",
        "//src/com/plasstech/lang/d2/common",
        "//src/com/plasstech/lang/d2/phase:state",
        "@maven//:com_github_pcj_google_options",
        "@maven//:com_google_guava_guava",
    ],
)

java_binary(
    name = "LexerDriver",
    srcs = ["LexerDriver.java"],
    deps = [
        "//src/com/plasstech/lang/d2/common",
        "//src/com/plasstech/lang/d2/lex",
    ],
)

java_binary(
    name = "ParserDriver",
    srcs = ["ParserDriver.java"],
    runtime_deps = [
        "@maven//:com_google_flogger_flogger_system_backend",
    ],
    deps = [
        ":yet_another_compiler",
        "//src/com/plasstech/lang/d2/common",
        "//src/com/plasstech/lang/d2/parse/node",
        "//src/com/plasstech/lang/d2/phase:phase_name",
        "//src/com/plasstech/lang/d2/phase:state",
    ],
)

java_binary(
    name = "CheckerDriver",
    srcs = ["CheckerDriver.java"],
    runtime_deps = [
        "@maven//:com_google_flogger_flogger_system_backend",
    ],
    deps = [
        ":yet_another_compiler",
        "//src/com/plasstech/lang/d2/common",
        "//src/com/plasstech/lang/d2/parse/node",
        "//src/com/plasstech/lang/d2/phase:phase_name",
        "//src/com/plasstech/lang/d2/phase:state",
    ],
)

java_binary(
    name = "CodeGenDriver",
    srcs = ["CodeGenDriver.java"],
    runtime_deps = [
        "@maven//:com_google_flogger_flogger_system_backend",
    ],
    deps = [
        ":yet_another_compiler",
        "//src/com/plasstech/lang/d2/codegen/il",
        "//src/com/plasstech/lang/d2/common",
        "//src/com/plasstech/lang/d2/phase:phase_name",
        "//src/com/plasstech/lang/d2/phase:state",
        "@maven//:com_google_guava_guava",
    ],
)

java_binary(
    name = "ILOptimizerDriver",
    srcs = ["ILOptimizerDriver.java"],
    runtime_deps = [
        "@maven//:com_google_flogger_flogger_system_backend",
    ],
    deps = [
        ":yet_another_compiler",
        "//src/com/plasstech/lang/d2/common",
        "//src/com/plasstech/lang/d2/phase:phase_name",
        "//src/com/plasstech/lang/d2/phase:state",
        "@maven//:com_google_guava_guava",
    ],
)

java_binary(
    name = "InterpreterDriver",
    srcs = ["InterpreterDriver.java"],
    runtime_deps = [
        "@maven//:com_google_flogger_flogger_system_backend",
    ],
    deps = [
        ":executor",
        "//src/com/plasstech/lang/d2/common",
        "//src/com/plasstech/lang/d2/interpreter",
        "//src/com/plasstech/lang/d2/phase:state",
        "@maven//:com_github_pcj_google_options",
        "@maven//:com_google_guava_guava",
    ],
)

java_library(
    name = "executor",
    srcs = ["InterpreterExecutor.java"],
    visibility = ["//visibility:public"],
    deps = [
        ":yet_another_compiler",
        "//src/com/plasstech/lang/d2/common",
        "//src/com/plasstech/lang/d2/interpreter",
        "//src/com/plasstech/lang/d2/phase:state",
        "@maven//:com_github_pcj_google_options",
        "@maven//:com_google_guava_guava",
    ],
)

java_library(
    name = "yet_another_compiler",
    srcs = ["YetAnotherCompiler.java"],
    visibility = ["//visibility:public"],
    deps = [
        "//src/com/plasstech/lang/d2/codegen",
        "//src/com/plasstech/lang/d2/codegen/il",
        "//src/com/plasstech/lang/d2/common",
        "//src/com/plasstech/lang/d2/lex",
        "//src/com/plasstech/lang/d2/optimize",
        "//src/com/plasstech/lang/d2/parse",
        "//src/com/plasstech/lang/d2/phase:phase_name",
        "//src/com/plasstech/lang/d2/phase:state",
        "//src/com/plasstech/lang/d2/type",
        "@maven//:com_google_guava_guava",
    ],
)
