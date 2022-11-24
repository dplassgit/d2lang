; To execute:
; nasm -fwin64 C:/Users/dplas/dev/d2lang/samples/sqrt.d.asm && gcc C:/Users/dplas/dev/d2lang/samples/sqrt.d.obj -o C:/Users/dplas/dev/d2lang/samples/sqrt.d && ./C:/Users/dplas/dev/d2lang/samples/sqrt.d

global main

extern __main
extern printf
extern _flushall
extern exit

section .data
  PRINTF_COUNT: db "argc count is %d", 10, 0
  PRINTF_ARG: db "argv[%d] is %s", 10, 0

section .text
main:

	;push	rbp
	;mov	rbp, rsp

  ; this is required to convert the stack to the thingie
	mov	DWORD [rsp+16], ecx
	mov	QWORD [rsp+24], rdx
	call	__main

  mov rcx, PRINTF_COUNT
  ; this works!
  mov edx, [rsp+16]
  sub RSP, 0x20
  call printf
  add RSP, 0x20

  sub RSP, 0x20
  call _flushall
  add RSP, 0x20

  mov rcx, PRINTF_ARG
  ; get the nth value
  mov edx, [rsp+16]
  dec edx
  ; 8 bytes per pointer
  imul edx, 8
  ; this works!!!
  ; get the start of the array
	mov	r8, QWORD [rsp+24]
  ; dereference the nth entry
  add r8, rdx
	mov	r8, QWORD [r8]

  mov edx, [rsp+16]

  sub RSP, 0x20
  call printf
  add RSP, 0x20

  sub RSP, 0x20
  call _flushall
  add RSP, 0x20

  ; mov RCX, 0
  ; call exit
  mov rax, 0
	;pop	rbp
	ret
