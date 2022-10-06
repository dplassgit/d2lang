# Double manipulation

Welcome to the wonderful world of MMX (Floating point.)

## Registers

XMM0 through XMM15 and are 128-bits each. Most opcodes will clear the upper
64 bits if not used.

YMM registers are 256 bits long. XMM registers represent the lower 128 bits of
the YMM registers. The YMM and XMM registers are overlapping and XMM are
contained in YMM.

## Constants

`_pi: dq 3.14159`

Note, that doubles cannot be used as immediate arguments.

## Loading constants

`movsd xmm0, [_pi]`

The `sd` means "scalar double"

## Args

When passing a double argument, instead of RCX, RDX, R8, R9, substitute XMM0,
XMM1, XMM2, XMM3.

E.g., to pass two ints and two doubles, use RCX, RDX, XMM2, XMM3.

Double int double int: XMM0, RDX, XMM2, R9

**EXCEPT FOR VARARGS**, like `printf`: put **ALL** args into integer registers.
This may require double transfers, e.g., load a constant into an MMX register,
then move to an int register. Example:

```
section .data
  PRINTF_NUMBER_FMT: db "%f", 10, 0
  _pi: dq 3.14159

section .text
  ...
  mov RCX, PRINTF_NUMBER_FMT  ; First argument is address of pattern
  movsd XMM0, [_pi]  ;; load double constant
  movq RDX, XMM0  ;; transfer to RDX for second argument
  sub RSP, 0x20
  call printf
  add RSP, 0x20
```

NOTE use of `movq`, which is required because it moves between an MMX
(floating point) register and an integer register.

## Arithmetic

Same as integer registers, the format is:

`OP R1, R2` means `R1 = R1 OP R2`

But R2 cannot be an immediate argument. 

Multiplying pi times two;

```
  movsd xmm0, [_pi]
  movsd xmm1, [_two]
  mulsd xmm0, xmm1  ; xmm0 *= xmm1
```

## Volatile registers

XMM0-XMM5 are volatile. Consider volatile registers destroyed on function
calls.

XMM6-XMM15 are nonvolatile. They must be saved and restored by a function
that uses them.


## References

https://www.felixcloutier.com/x86/index.html

https://docs.microsoft.com/en-us/cpp/build/x64-software-conventions?view=msvc-170#x64-register-usage

https://docs.microsoft.com/en-us/cpp/build/x64-calling-convention?view=msvc-170#parameter-passing

