extern calloc
extern sprintf
extern strncpy
extern strstr

global btoi
global btos
global ifind
global itod
global itos
global ltod
global ltos
global substr

section .text

; Find a string within another string, and return its index,
; or -1 if not found.
; ifind: extern proc(haystack: string, needle: string): int
ifind:
  ; first string in rcx
  ; second string in rdx
  push rcx
  sub RSP, 0x20
  call strstr
  add RSP, 0x20
  pop rcx
  ; result in rax
  cmp rax, 0
  je ifind_notfound
  ; rax = rax - rcx
  sub rax, rcx
  ret

ifind_notfound:
  mov rax, -1
  ret


; Substring: rcx=source, rdx=start, r8=end
; substr: extern proc(source: string, start: int, end: int): string
substr:
  ; 1. get new length = end-start+1
  sub r8, rdx
  inc r8

  ; 2. allocate: calloc(rcx, rdx)
  push rcx
  push rdx
  mov rcx, r8
  mov rdx, 1
  sub RSP, 0x20
  call calloc
  add RSP, 0x20
  ; rax has new string
  pop rdx
  pop rcx

  ; 3. make starting location:rcx+start
  add rcx, rdx

  ; 4. strncpy(dest/rcx, source/rdx, size/r8)
  mov rdx, rcx   ; source into rdx
  mov rcx, rax   ; dest from rax to rcx
  mov r8, 1      ; size
  sub RSP, 0x20
  call strncpy   ; destination is returned.
  add RSP, 0x20

  ret


; btoi: extern proc(b: byte): int
btoi:
  xor rax, rax
  ; sign-extend 
  movsx eax, cl
  ret

; itod: extern proc(i: int): double
itod: 
	cvtsi2sd xmm0, ecx
	ret

; ltod: extern proc(el: long): double
ltod: 
	cvtsi2sd xmm0, rcx
	ret


; btos: extern proc(b: byte): string
btos:
  ;sprintf(result, "0y%02x", input)
  and rcx, 0x000000ff
  mov rdx, SPRINTF_BYTE
  ; allocate 5 bytes: "0yxx" + 1
  mov r8, 5
  jmp xtos ; tail recursion


; itos: extern proc(i: int): string
itos:
  ;sprintf(result, "%d", input)
  mov rdx, SPRINTF_INT
  ; allocate 12 bytes: -2,147,483,648 is the MNN (11 plus 1 for null)
  mov r8, 12
  jmp xtos ; tail recursion


; ltos: extern proc(ell: long): string
ltos:
  ;sprintf(result, "%lld", input)
  mov rdx, SPRINTF_LONG
  mov r8, 21
  jmp xtos ; tail recursion

; parameters:
; rcx: value
; rdx: sprintf
; r8: size
xtos:
  push rcx ; save the input (value)
  push rdx ; save the input (format)

  ; allocate r8 bytes
  mov rcx, r8
  mov rdx, 1
  sub RSP, 0x20
  call calloc
  add RSP, 0x20
  ; rax has new string

  mov rcx, rax
  pop rdx
  pop r8  ; input was in RCX, now in R8
  push rax ; save result
  sub RSP, 0x20
  call sprintf
  add RSP, 0x20
  pop rax ; return it
  ret


section .data
  SPRINTF_BYTE: db "0y%02x", 0
  SPRINTF_INT: db "%d", 0
  SPRINTF_LONG: db "%lld", 0 ; note, no trailing L, which is ironic.
