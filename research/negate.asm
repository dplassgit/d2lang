; To execute:
; nasm -fwin64 -Ox printdouble.asm && gcc printdoubleobj -o d2out.exe

global main

extern exit
extern printf

section .data
  PRINTF_NUMBER_FMT: db "%f", 10, 0
  _pi: dq 3.14159
  n1d: dq 0
  NEGATIVE_ONE_DOUBLE: dq  0x8000000000000000
  _x: dq 0

section .text
main:

  ; Loading a constant
  movsd xmm0, [_pi]
  movq [_x], xmm0 ; movq is required because it's a quadword

  ; negate: xmm0 = -1
  movsd xmm0, [_x]
  movsd xmm1, [NEGATIVE_ONE_DOUBLE]
  ; xmm0 = -pi
  ; xorpd uses 128 bits only, so can't use [_x] because it's only 64 bits
  xorpd xmm0, xmm1 

  mov RCX, PRINTF_NUMBER_FMT  ; First argument is address of pattern
  movq rdx, xmm0 ; second arg
  sub RSP, 0x20
  call printf
  add RSP, 0x20

  xor RCX, RCX
  call exit
