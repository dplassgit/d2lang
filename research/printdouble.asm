; To execute:
; nasm -fwin64 -Ox printDouble_opt_false.asm && gcc printDouble_opt_false.obj -o printDouble_opt_false && ./printDouble_opt_false

global main

extern exit
extern printf

section .data
  DOUBLE_3_4_0: dq 3.141
  PRINTF_DOUBLE_FMT: db "%.5f", 10, 0
  _a: dq 0
  _b: dq 0

section .text
main:

  ; START a = 3.4;
  movsd XMM0, [DOUBLE_3_4_0]
  movq [_a], XMM0
  ; END a = 3.4;

  ; START b = a;
  movsd XMM0, [_a]
  movq [_b], XMM0
  ; END b = a;

  ; START printf("%s", b);
  ; movq RDX, [_b] ;; this is wrong.
  mov RDX, [_b] ;; this is right.
  mov RCX, PRINTF_DOUBLE_FMT  ; First argument is address of pattern
  sub RSP, 0x20
  call printf
  add RSP, 0x20
  ; END printf("%s", b);

  ; START printf("%s", a);
  ;movq RDX, [_a] ;; this is wrong.
  mov RDX, [_a] ;; this is right.
  mov RCX, PRINTF_DOUBLE_FMT  ; First argument is address of pattern
  sub RSP, 0x20
  call printf
  add RSP, 0x20
  ; END printf("%s", a);

  ; START exit(0);
  mov RCX, 0
  call exit
  ; END exit(0);
