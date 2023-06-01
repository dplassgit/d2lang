global main
extern exit
extern printf
extern _flushall

section .text
main:
  mov RAX, 1
  push RAX
  mov RAX, 2
  push RAX
  mov RAX, 3
  push RAX
  mov RAX, 99
  push RAX

  mov RAX, [RSP]
  xchg rax, [RSP+24]
  mov [RSP], RAX

  mov RAX, [RSP+8]
  xchg rax, [RSP+16]
  mov [RSP+8], RAX

  mov RCX, CONST_0
  mov RDX, [RSP]  ; top of stack
  sub RSP, 0x20
  call printf
  add RSP, 0x20

  mov RCX, CONST_0
  mov RDX, [RSP+8]  ; next
  sub RSP, 0x20
  call printf
  add RSP, 0x20

  mov RCX, CONST_0
  mov RDX, [RSP+16]
  sub RSP, 0x20
  call printf
  add RSP, 0x20

  mov RCX, CONST_0
  mov RDX, [RSP+24]
  sub RSP, 0x20
  call printf
  add RSP, 0x20

  call exit

section .data
  CONST_0: db "", 37, "d", 10, 0
