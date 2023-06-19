genrule(name="v5src",
        srcs=[
          "lexer.d",
          "stringlist.d",
          "emitter.d",
          "symtab.d",
          "registers.d", # needs vartype
          "parser.d",
        ],
        outs=["v5.d"],
        cmd="cat $(SRCS) > $@"
)

genrule(name="v5fromv4asm",
        srcs=["v5.d"],
        outs=["v5fromv4.asm"],
        tools=["//src/bootstrap/v4:v4fromv4.exe"],
        cmd="(echo 'Using v4fromv4' && $(location //src/bootstrap/v4:v4fromv4.exe) < $(SRCS) > $@) || (tail $@ && exit -1)"
)

genrule(name="v5fromv4obj",
        srcs=["v5fromv4.asm"],
        outs=["v5fromv4.obj"],
        cmd="nasm -fwin64 $(SRCS)",
)

genrule(name="v5exe",
        executable=True,
        srcs=["v5fromv4.obj"],
        outs=["v5.exe"],
        cmd="gcc $(SRCS) -o $@"
)

genrule(name="v5fromv5asm",
        srcs=["v5.d"],
        outs=["v5fromv5.asm"],
        tools=[":v5exe"],
        cmd="(echo 'Using v5' && $(location :v5exe) < $(SRCS) > $@) || (tail $@ && exit -1)"
)

genrule(name="v5fromv5obj",
        srcs=["v5fromv5.asm"],
        outs=["v5fromv5.obj"],
        cmd="nasm -fwin64 $(SRCS)",
)

genrule(name="v5fromv5exe",
        srcs=["v5fromv5.obj"],
        outs=["v5fromv5.exe"],
        executable=True,
        visibility=["//visibility:public"],
        cmd="gcc $(SRCS) -o $@"
)
