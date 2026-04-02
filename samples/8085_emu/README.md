# cpu.d

This is an 8085 emulator written in d2lang.

It is based on the C++ version in https://github.com/FanisDeligiannis/8085_emulator

See also [ATTRIBUTION.md](ATTRIBUTION.md)

## Usage

First, assemble the 8085 file using https://github.com/dplassgit/8085-Assembler-trs80-100
(my fork of https://github.com/sobkas/8085-Assembler-tsr80-100, with bugs and typos fixed).

```
python $PATH_TO_ASSEMBLER/assembly.py input.as -s -o input.8085
```

Then, run it through the executable:

```
cpu.exe < input.8085
```

