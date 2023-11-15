genrule(
    name = "v4src",
    srcs = [
        "lexer.d",
        "stringlist.d",
        "emitter.d",
        "symtab.d",
        "registers.d",  # needs vartype
        "parser.d",
    ],
    outs = ["v4.d"],
    cmd = "cat $(SRCS) > $@",
)

genrule(
    name = "v4fromv3asm",
    srcs = ["v4.d"],
    outs = ["v4fromv3.asm"],
    cmd = "(echo 'Using v3fromv3' && $(location //src/bootstrap/v3:v3fromv3.exe) < $(SRCS) > $@) || (tail $@ && exit -1)",
    tools = ["//src/bootstrap/v3:v3fromv3.exe"],
)

genrule(
    name = "v4fromv3obj",
    srcs = ["v4fromv3.asm"],
    outs = ["v4fromv3.obj"],
    cmd = "nasm -fwin64 $(SRCS)",
)

genrule(
    name = "v4exe",
    srcs = ["v4fromv3.obj"],
    outs = ["v4.exe"],
    cmd = "gcc $(SRCS) -o $@",
    executable = True,
)

genrule(
    name = "v4fromv4asm",
    srcs = ["v4.d"],
    outs = ["v4fromv4.asm"],
    cmd = "(echo 'Using v4' && $(location :v4exe) < $(SRCS) > $@) || (tail $@ && exit -1)",
    tools = [":v4exe"],
)

genrule(
    name = "v4fromv4obj",
    srcs = ["v4fromv4.asm"],
    outs = ["v4fromv4.obj"],
    cmd = "nasm -fwin64 $(SRCS)",
)

genrule(
    name = "v4fromv4exe",
    srcs = ["v4fromv4.obj"],
    outs = ["v4fromv4.exe"],
    cmd = "gcc $(SRCS) -o $@",
    executable = True,
    visibility = ["//visibility:public"],
)
