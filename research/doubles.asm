; To execute:
; nasm -fwin64 -Ox printdouble.asm && gcc printdoubleobj -o d2out.exe

global main

extern exit
extern printf

section .data
  PRINTF_NUMBER_FMT: db "%f", 10, 0
  _pi: dq 3.14159
  _two: dq 2.0
  _x: dq 0  ; just a dq - will be double - does it need to be initialized to 0.0?

section .text
main:

  ; Loading a constant
  movsd xmm0, [_pi]
  movq [_x], xmm0 ; movq is required because it's a quadword

  mov RCX, PRINTF_NUMBER_FMT  ; First argument is address of pattern
  ; get the double into xmm0 (or really, any xmm register)
  movsd xmm0, [_x]
  ; Transfer to rdx, because printf is varargs: 
  ; "Floating-point values are only placed in the integer registers RCX, RDX, R8, and R9 when there are varargs arguments."
  ; need movq because why?
  movq rdx, xmm0
  sub RSP, 0x20
  call printf
  add RSP, 0x20

  ; let's multiply pi by 2
  movsd xmm0, [_pi]
  movsd xmm1, [_two]
  mulsd xmm0, xmm1  ; xmm0 *= xmm1

  mov RCX, PRINTF_NUMBER_FMT  ; First argument is address of pattern
  movq rdx, xmm0
  sub RSP, 0x20
  call printf
  add RSP, 0x20

  mov RCX, 0
  call exit
