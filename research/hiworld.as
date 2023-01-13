; To assemble: python [path]/assembler.py dcode -t
org 0x09000
jmp __START
  CONST_helloworld_0: db "hello world", 0
__START:

  ; SOURCE: printf("%s", \"hello world\")
  lxi H, CONST_helloworld_0
  call 0x11A2
