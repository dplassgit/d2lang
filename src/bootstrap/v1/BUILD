genrule(name="v1src",
        srcs=["lexer.d", "symtab.d", "parser.d",],
        outs=["v1.d"],
        cmd="cat $(SRCS) > $@"
)

genrule(name="v1asm",
        srcs=["v1.d"],
        outs=["v1.asm"],
        cmd="src/bootstrap/v0/v0fromv0.exe < $(SRCS) > $@"
)

genrule(name="v1obj",
        srcs=["v1.asm"],
        outs=["v1.obj"],
        cmd="nasm -fwin64 $(SRCS)",
)

genrule(name="v1exe",
        srcs=["v1.obj"],
        outs=["v1.exe"],
        cmd="gcc $(SRCS) -o $@"
)
