  ; xvi=16-bit code
  ; To build:
  ; nasm -fbin xvi.asm -o xvi.com

  org 0x100

  ; write a character.
	;mov ah, 0x0e
  ;mov al, 'a'
	;int 0x10

  ; write a string, terminated by $ (?)
  mov dx, CONST_1
  mov ah, 9
  int 0x21 

  int 0x20   ; back to o/s

section .data
  CONST_1: db "Hello, world", 10, "$", 0

