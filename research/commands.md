# How to run

```
rm -f bug147.exe; nasm -fwin64 bug147.asm && gcc bug147.obj -o bug147.exe && ./bug147.exe

rm -f sqrt.exe; gcc sqrt.c -O0 --save-temps -masm=intel -o sqrt.exe && ./sqrt.exe

nasm -fbin xvi.asm -o xvi.com 
```

In dosbox use `ctrl-F4` to reload the mounted disk.
