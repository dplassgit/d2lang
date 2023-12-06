extern strstr
extern calloc
extern strncpy

global ifind
global substr

section .text

; Find a string within another stirng, and return its index,
; or -1 if not found.
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
