; To execute:
; nasm -fwin64 C:/Users/dplas/dev/d2lang/samples/sqrt.d.asm && gcc C:/Users/dplas/dev/d2lang/samples/sqrt.d.obj -o C:/Users/dplas/dev/d2lang/samples/sqrt.d && ./C:/Users/dplas/dev/d2lang/samples/sqrt.d

global main

extern exit
extern printf
extern sqrt
extern ceil
extern sin
extern tan
extern fabs
extern log
extern log2
extern log10
extern floor
extern round
extern trunc

section .data
  DOUBLE_1_0E5_2: dq 0.000010
  DOUBLE_23423_0_4: dq 23.423000000
  DOUBLE_2_0_3: dq 2.000000
  DOUBLE_4_0_1: dq 4.000000
  DOUBLE_9_0_5: dq 9.100000
  DOUBLE_01: dq 0.100000
  DOUBLE_10: dq 10.000000
  PRINTF_DOUBLE_FMT: db "should be %f is %f", 10, 0
  ZERO_DBL: dq 0.000000

section .text
main:

  movsd XMM6, [DOUBLE_23423_0_4]
  ;movsd XMM6, [DOUBLE_9_0_5]
  ;movsd XMM6, [DOUBLE_01]
  ;movsd xMM6, [DOUBLE_10]
  movsd xmm0, xmm6

  sub RSP, 0x28     ;; AHA THIS WAS THE PROBLEM!
  call sqrt 
  ;call sin 
  ;call ceil
  ;call fabs
  ;call log 
  ;call log10
  ;call floor
  ;call round
  ;call trunc
  ;call log2
  ;call tan

  add RSP, 0x28

  movq rdx, xmm6 ; input
  movq R8, XMM0 ; output
  mov RCX, PRINTF_DOUBLE_FMT  ; First argument is address of pattern
  sub RSP, 0x20
  call printf
  add RSP, 0x20

  mov RCX, 0
  call exit
