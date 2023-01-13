s/call 0/; \0/
s/ _/ d_/g
s/^_/d_/
s/:$/: nop/
s/0x\([0-9a-f]\{4\}\)/0\1h/g
s/0x\([0-9a-f]\{2\}\)/0\1h/g
s/ org/;org/
s/ + /+/g
