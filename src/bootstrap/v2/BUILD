genrule(
    name = "v2src",
    srcs = [
        "lexer.d",
        "stringlist.d",
        "symtab.d",
        "parser.d",
    ],
    outs = ["v2.d"],
    cmd = "cat $(SRCS) > $@",
)

genrule(
    name = "v2asm",
    srcs = ["v2.d"],
    outs = ["v2.asm"],
    # NOTE: does not use the bazel target. Uses the executable as checked into git
    cmd = "(echo 'Using v1fromv1' && src/bootstrap/v1/v1fromv1.exe < $(SRCS) > $@) || (tail $@ && exit -1)",
)

genrule(
    name = "v2obj",
    srcs = ["v2.asm"],
    outs = ["v2.obj"],
    cmd = "nasm -fwin64 $(SRCS)",
)

genrule(
    name = "v2exe",
    srcs = ["v2.obj"],
    outs = ["v2.exe"],
    cmd = "gcc $(SRCS) -o $@",
    executable = True,
)

genrule(
    name = "v2fromv2asm",
    srcs = ["v2.d"],
    outs = ["v2fromv2.asm"],
    # TODO: diff the two .asm files
    cmd = "(echo 'Using v2' && $(location :v2exe) < $(SRCS) > $@) || (tail $@ && exit -1)",
    tools = [":v2exe"],
)

genrule(
    name = "v2fromv2obj",
    srcs = ["v2fromv2.asm"],
    outs = ["v2fromv2.obj"],
    cmd = "nasm -fwin64 $(SRCS)",
)

genrule(
    name = "v2fromv2exe",
    srcs = ["v2fromv2.obj"],
    outs = ["v2fromv2.exe"],
    cmd = "gcc $(SRCS) -o $@",
    executable = True,
    visibility = ["//visibility:public"],
)
