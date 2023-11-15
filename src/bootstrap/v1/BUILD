genrule(
    name = "v1src",
    srcs = [
        "lexer.d",
        "symtab.d",
        "parser.d",
    ],
    outs = ["v1.d"],
    cmd = "cat $(SRCS) > $@",
)

genrule(
    name = "v1asm",
    srcs = ["v1.d"],
    outs = ["v1.asm"],
    cmd = "src/bootstrap/v0/v0fromv0.exe < $(SRCS) > $@ || tail $@",
)

genrule(
    name = "v1obj",
    srcs = ["v1.asm"],
    outs = ["v1.obj"],
    cmd = "nasm -fwin64 $(SRCS)",
)

genrule(
    name = "v1exe",
    srcs = ["v1.obj"],
    outs = ["v1.exe"],
    cmd = "gcc $(SRCS) -o $@",
    executable = True,
)

genrule(
    name = "v1fromv1asm",
    srcs = ["v1.d"],
    outs = ["v1fromv1.asm"],
    cmd = "$(location :v1exe) < $(SRCS) > $@ || tail $@",
    tools = [":v1exe"],
)

genrule(
    name = "v1fromv1obj",
    srcs = ["v1fromv1.asm"],
    outs = ["v1fromv1.obj"],
    cmd = "nasm -fwin64 $(SRCS)",
)

genrule(
    name = "v1fromv1exe",
    srcs = ["v1fromv1.obj"],
    outs = ["v1fromv1.exe"],
    cmd = "gcc $(SRCS) -o $@",
    executable = True,
    visibility = ["//visibility:public"],
)
