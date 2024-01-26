genrule(name="emusrc",
        srcs=[
          "cpu.d",
        ],
        outs=["emu.d"],
        cmd="cat $(SRCS) > $@"
)

# TODO: now that source is concatenated, build the compiler and then compile the concatenated source.
