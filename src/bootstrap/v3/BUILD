genrule(
    name = "v3src",
    srcs = [
        "lexer.d",
        "stringlist.d",
        "symtab.d",
        "parser.d",
    ],
    outs = ["v3.d"],
    cmd = "cat $(SRCS) > $@",
)

genrule(
    name = "v3asm",
    srcs = ["v3.d"],
    outs = ["v3.asm"],
    cmd = "(echo 'Using v2fromv2' && $(location //src/bootstrap/v2:v2fromv2.exe) < $(SRCS) > $@) || (tail $@ && exit -1)",
    tools = ["//src/bootstrap/v2:v2fromv2.exe"],
)

genrule(
    name = "v3obj",
    srcs = ["v3.asm"],
    outs = ["v3.obj"],
    cmd = "nasm -fwin64 $(SRCS)",
)

genrule(
    name = "v3exe",
    srcs = ["v3.obj"],
    outs = ["v3.exe"],
    cmd = "gcc $(SRCS) -o $@",
    executable = True,
)

genrule(
    name = "v3fromv3asm",
    srcs = ["v3.d"],
    outs = ["v3fromv3.asm"],
    # TODO: diff the two .asm files
    cmd = "(echo 'Using v3' && $(location :v3exe) < $(SRCS) > $@) || (tail $@ && exit -1)",
    tools = [":v3exe"],
)

genrule(
    name = "v3fromv3obj",
    srcs = ["v3fromv3.asm"],
    outs = ["v3fromv3.obj"],
    cmd = "nasm -fwin64 $(SRCS)",
)

genrule(
    name = "v3fromv3exe",
    srcs = ["v3fromv3.obj"],
    outs = ["v3fromv3.exe"],
    cmd = "gcc $(SRCS) -o $@",
    executable = True,
    visibility = ["//visibility:public"],
)
