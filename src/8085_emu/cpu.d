btoi: extern proc(b: byte): int
itob: extern proc(i: int): byte
itos: extern proc(i: int): string
ifind: extern proc(haystack: string, needle: string): int

// This has to be here so the rest of the global references to cpu: CPU work.
STACK_START = 65534 // why not 65535?
cpu = newCpu()

// 8-bit register
Register: record {
  v: byte
}

Increment: proc(r: Register) { r.v = r.v + 0y01 }
Decrement: proc(r: Register) { r.v = r.v - 0y01 }
SetValueI: proc(dest: Register, v: int) { dest.v = itob(v) }
SetValue: proc(dest: Register, v: byte) { dest.v = v }

SetBit: proc(r: Register, bit: byte, v: bool) {
  if v {
    r.v = r.v | (0y01 << bit)
  } else {
    ClearBit(r, bit)
  }
}

// Returning an int so we can do math
GetBit: proc(r: Register, bit: byte): int {
  return btoi((r.v >> bit) & 0y01)
}

// Clear bit number bit of register r
ClearBit: proc(r: Register, bit: byte) {
  r.v = r.v & (!(0y01 << bit))
}

// Stack (separate from memory, which is not realistic,
// shrug.)
Stack: record {
  SP: int
  stack_data: byte[65535]
}

newStack: proc: Stack {
  s = new Stack
  s.SP = STACK_START 
  return s
}

Push: proc(stack: Stack, value: byte) {
  stack.SP = stack.SP - 1

  // Issue #55: support advanced l-values
  data = stack.stack_data
  data[stack.SP] = value
}

Pop: proc(cpu: CPU): byte {
  stack = cpu.stack

  ret = stack.stack_data[stack.SP]

  // Clear the stop of the stack.
  data = stack.stack_data
  data[stack.SP] = 0y00

  stack.SP = stack.SP + 1

  if stack.SP > STACK_START {
    exit "ERROR: POP on empty stack at PC " + itos(cpu.PC)
  }

  return ret
}

// Represents state of the CPU and memory
CPU: record {
  A: Register
  B: Register
  C: Register
  D: Register
  E: Register
  H: Register
  L: Register
  Flags: Register

  org: int  // origin
  PC: int  // really, a 16-bit short

  running: bool

  memory: byte[65536]

  stack: Stack

  debug: bool
}


newCpu: proc: CPU {
  cpu = new CPU
  cpu.A = new Register
  cpu.B = new Register
  cpu.C = new Register
  cpu.D = new Register
  cpu.E = new Register
  cpu.H = new Register
  cpu.L = new Register
  cpu.Flags = new Register

  cpu.PC = 0
  cpu.stack = newStack()

  debugFlag = false
  i = 0 while i < length(args) do i++ {
    debugFlag = debugFlag or (args[i] == '-d')
  }
  cpu.debug = debugFlag 

  return cpu
}


//////////////////////////////////////////////////////
//
// MEMORY
//
//////////////////////////////////////////////////////

SetMemory: proc(cpu: CPU, addr: int, value: byte) {
  m = cpu.memory
  m[addr] = value
}

// Get the byte in memory pointed to by the M (HL) register pair.
GetM: proc(cpu: CPU): byte {
  return cpu.memory[GetHLUnsigned()]
}

// Get the next byte past the PC in memory
NextPC: proc(cpu: CPU): byte {
  cpu.PC = cpu.PC + 1
  ret = cpu.memory[cpu.PC]
  return ret
}

// Get the next 2 bytes in memory, using PC
GetNextPC16: proc(): int {
  low = btoi(NextPC(cpu)) & 255 // unsigned
  high = btoi(NextPC(cpu)) & 255 // unsigned
  word = (high << 8) | low
  return word
}


//////////////////////////////////////////////////////
//
// FLAGS
//
//////////////////////////////////////////////////////
SIGN_FLAG=0y07
ZERO_FLAG=0y06
AUX_CARRY_FLAG=0y04
PARITY_FLAG=0y02
CARRY_FLAG=0y00

SetFlags: proc(cpu: CPU, sign: bool, zero: bool, carry: bool) {
  SetBit(cpu.Flags, SIGN_FLAG, sign)
  SetBit(cpu.Flags, ZERO_FLAG, zero)
  SetBit(cpu.Flags, CARRY_FLAG, carry)
}

// Set sign & zero flags, depending on the given number.
SetFlagsBasedOn: proc(v: byte): void {
  SetBit(cpu.Flags, SIGN_FLAG, (v & 0y80) != 0y00)
  SetBit(cpu.Flags, ZERO_FLAG, v == 0y00)
}

// Set sign & zero flags, depending on the given number.
SetFlagsBasedOnI: proc(v: int): void {
  v = v & 255
  SetBit(cpu.Flags, SIGN_FLAG, (v & 128) != 0)
  SetBit(cpu.Flags, ZERO_FLAG, v == 0)
}


//////////////////////////////////////////////////////
//
// REGISTER PAIRS
//
//////////////////////////////////////////////////////

// Get register pair BC unsigned int
GetBCUnsigned: proc(): int {
  B = btoi(cpu.B.v) & 255
  C = btoi(cpu.C.v) & 255
  BC = (B << 8) | C
  return BC
}

// Get register pair DE unsigned int
GetDEUnsigned: proc(): int {
  D = btoi(cpu.D.v) & 255
  E = btoi(cpu.D.v) & 255
  DE = (D << 8) | E
  return DE
}

// Get register pair HL unsigned int
GetHLUnsigned: proc(): int {
  H = btoi(cpu.H.v) & 255
  L = btoi(cpu.L.v) & 255
  HL = (H << 8) | L
  return HL
}

// Get register pair BC signed int16_t
GetBCSigned: proc(): int {
  B = btoi(cpu.B.v)
  C = btoi(cpu.C.v)
  BC = (B << 8) | C
  return BC
}

// Get register pair HL signed int
GetHLSigned: proc(): int {
  H = btoi(cpu.H.v)
  L = btoi(cpu.L.v)
  HL = (H << 8) | L
  return HL
}


//////////////////////////////////////////////////////
//
// UTILITIES for opcodes
//
//////////////////////////////////////////////////////

// Compare Register A with another number. Set the flags accordingly.
Compare: proc(other: byte): void {
  A = btoi(cpu.A.v) & 255
  otherI = btoi(other) & 255

  // Must be unsigned comparison
  res = A - otherI

  SetBit(cpu.Flags, SIGN_FLAG, (res & 128) != 0)
  SetBit(cpu.Flags, ZERO_FLAG, A == otherI)
  SetBit(cpu.Flags, CARRY_FLAG, A < otherI)
}

// Add signed number to Register A
ADDx: proc(other: byte): void {
  // Convert to unsigned ints
  rA = btoi(cpu.A.v) & 255
  rOther = btoi(other) & 255

  result = rA + rOther

  SetFlags(cpu,
    (result & 128) != 0, // sign
    (result & 255) == 0, // zero
    (result & 256) > 0 // carry
  )

  // Will convert back to a byte
  SetValueI(cpu.A, result)
}

// Add signed number to Register A with carry
ADCx: proc(other: byte): void {
  // Convert to unsigned ints
  rA = btoi(cpu.A.v) & 255
  carryIn = GetBit(cpu.Flags, CARRY_FLAG)
  rOther = btoi(other) & 255

  result = rA + rOther + carryIn

  SetFlags(cpu,
    (result & 128) != 0, // sign
    (result & 255) == 0, // zero
    (result & 256) > 0 // carry out
  )

  // Will convert back to a byte
  SetValueI(cpu.A, result)
}

//////////////////////////////////////////////////////
//
// OPCODES
//
//////////////////////////////////////////////////////

ACI: proc() { ADCx(NextPC(cpu)) }
ADCA: proc() { ADCx(cpu.A.v) }
ADCB: proc() { ADCx(cpu.B.v) }
ADCC: proc() { ADCx(cpu.C.v) }
ADCD: proc() { ADCx(cpu.D.v) }
ADCE: proc() { ADCx(cpu.E.v) }
ADCH: proc() { ADCx(cpu.H.v) }
ADCL: proc() { ADCx(cpu.L.v) }
ADCM: proc() { ADCx(GetM(cpu)) }

ADDA: proc() { ADDx(cpu.A.v) }
ADDB: proc() { ADDx(cpu.B.v) }
ADDC: proc() { ADDx(cpu.C.v) }
ADDD: proc() { ADDx(cpu.D.v) }
ADDE: proc() { ADDx(cpu.E.v) }
ADDH: proc() { ADDx(cpu.H.v) }
ADDL: proc() { ADDx(cpu.L.v) }
ADDM: proc() { ADDx(GetM(cpu)) }
ADI: proc() { ADDx(NextPC(cpu)) }

// Bitwise And Register A with another number.
ANDx: proc(other: byte): void {
  result = cpu.A.v & other
  SetFlags(cpu,
          (result & 0y80) != 0y00, // sign
          result == 0y00, // zero
          false // clear carry
      )
  SetValue(cpu.A, result)
}

ANAA: proc() { ANDx(cpu.A.v) }
ANAB: proc() { ANDx(cpu.B.v) }
ANAC: proc() { ANDx(cpu.C.v) }
ANAD: proc() { ANDx(cpu.D.v) }
ANAE: proc() { ANDx(cpu.E.v) }
ANAH: proc() { ANDx(cpu.H.v) }
ANAL: proc() { ANDx(cpu.L.v) }
ANAM: proc() { ANDx(GetM(cpu)) }
ANI:  proc() { ANDx(NextPC(cpu)) }

CALL: proc() {
  addr = GetNextPC16()

  // If this is a ROM call, emulate it.
  if addr == 1282 { // 0x0502
    // Drop into basic
    cpu.running = false
    return
  } elif addr == 16930 { // 0x4222
    // crlf
    println ""
    return
  } elif addr == 4514 { // 0x11a2
    // send the buffer pointed by HL to the screen
    start = GetHLUnsigned()
    while cpu.memory[start] != 0y00 do start++ {
      c = cpu.memory[start]
      print chr(btoi(c))
    }
    return
  } elif addr == 32 { // 0x0020 
    // print A
    print chr(btoi(cpu.A.v) & 255)
    return
  } elif addr == 14804 { // 0x39D4
    // print the number in HL
    print GetHLUnsigned()
    return
  }

  // Otherwise, do the rest of this method

  // Get the return address, one after here.
  PC = cpu.PC + 1

  high = itob(PC >> 8)
  low = itob(PC & 255)

  Push(cpu.stack, high)
  Push(cpu.stack, low)

  cpu.PC = addr - 1
}

// Call conditionally based on the given flag
CallCond: proc(flag: byte, skip_if: int) {

  // TODO: flip the skip_if parameter
  if (skip_if == GetBit(cpu.Flags, flag)) {
    // Skip past the address
    cpu.PC = cpu.PC + 2
    return
  }

  addr = GetNextPC16()

  PC = cpu.PC + 1

  high = itob(PC >> 8)
  low = itob(PC & 255)

  Push(cpu.stack, high)
  Push(cpu.stack, low)

  cpu.PC = addr - 1
}

CC: proc() {
  CallCond(CARRY_FLAG, 0)
}

CM: proc() {
  CallCond(SIGN_FLAG, 0)
}

CMA: proc() {
  SetValue(cpu.A, !(cpu.A.v))
}

CMC: proc() {
  carry = GetBit(cpu.Flags, CARRY_FLAG) == 1
  SetBit(cpu.Flags, CARRY_FLAG, not carry)
}

CMPA: proc() {
  // compare a with a
  ClearBit(cpu.Flags, SIGN_FLAG)
  SetBit(cpu.Flags, ZERO_FLAG, true)
  ClearBit(cpu.Flags, CARRY_FLAG)
}

CMPB: proc() { Compare(cpu.B.v) }
CMPC: proc() { Compare(cpu.C.v) }
CMPD: proc() { Compare(cpu.D.v) }
CMPE: proc() { Compare(cpu.E.v) }
CMPH: proc() { Compare(cpu.H.v) }
CMPL: proc() { Compare(cpu.L.v) }
CMPM: proc() { Compare(GetM(cpu)) }

CNC: proc() {
  CallCond(CARRY_FLAG, 1)
}

CNZ: proc() {
  CallCond(ZERO_FLAG, 1)
}

CP: proc() {
  CallCond(SIGN_FLAG, 1)
}

CPE: proc() {
  exit "CPE not implemented"
}

CPI: proc() {
  dir = NextPC(cpu)
  Compare(dir)
}

CPO: proc() {
  exit "CPO not implemented"
}

CZ: proc() {
  CallCond(ZERO_FLAG, 0)
}

DAA: proc() {
  exit "DAA not implemented"
}

DADB: proc() {
  exit "DADB not implemented"

  // Original C++:
  //int BC = GetBCSigned()
  //int HL = GetHLSigned()

  //int32_t res = BC + HL

  //int newHL = res & (0xffff)

  //if ((newHL & 0xffff0000) > 1)
  //{
    //SetBit(cpu, Flags, CARRY_FLAG, 1)
  //}
  //else
  //{
    //ClearBit(cpu, Flags, CARRY_FLAG)
  //}

  //uint8_t high = (newHL >> 8) & 255
  //uint8_t low = newHL & 255

  //cpu->H->SetSigned(high)
  //cpu->L->SetSigned(low)
}

DADD: proc() {
  exit "DADD not implemented"
}

DADH: proc() {
  exit "DADH not implemented"
}

DADSP: proc() {
  exit "DADSP not implemented"
}

DCRx: proc(r: Register) {
  Decrement(r)
  SetFlagsBasedOn(r.v)
}

DCRA: proc() { DCRx(cpu.A) }
DCRB: proc() { DCRx(cpu.B) }
DCRC: proc() { DCRx(cpu.C) }
DCRD: proc() { DCRx(cpu.D) }
DCRE: proc() { DCRx(cpu.E) }
DCRH: proc() { DCRx(cpu.H) }
DCRL: proc() { DCRx(cpu.L) }

DCRM: proc() {
  M = GetM(cpu)
  M--

  addr = GetHLUnsigned()
  SetMemory(cpu, addr, M)
  SetFlagsBasedOn(M)
}

DCXB: proc() {
  BC = GetBCUnsigned()
  BC--

  high = (BC >> 8) & 255
  low = BC & 255

  SetValueI(cpu.B, high)
  SetValueI(cpu.C, low)
}

DCXD: proc() {
  DE = GetDEUnsigned()
  DE--

  high = (DE >> 8) & 255
  low = DE & 255

  SetValueI(cpu.D, high)
  SetValueI(cpu.E, low)
}

DCXH: proc() {
  HL = GetHLUnsigned()
  HL--

  high = (HL >> 8) & 255
  low = HL & 255

  SetValueI(cpu.H, high)
  SetValueI(cpu.L, low)
}

DCXSP: proc() {
  stack = cpu.stack
  stack.SP = stack.SP - 1
}

HLT: proc() {
  cpu.running = false
}

INRx: proc(r: Register) {
  Increment(r)
  SetFlagsBasedOn(r.v)
}

INRA: proc() { INRx(cpu.A) }
INRB: proc() { INRx(cpu.B) }
INRC: proc() { INRx(cpu.C) }
INRD: proc() { INRx(cpu.D) }
INRE: proc() { INRx(cpu.E) }
INRH: proc() { INRx(cpu.H) }
INRL: proc() { INRx(cpu.L) }

INRM: proc() {
  M = GetM(cpu)
  M++

  addr = GetHLUnsigned()
  SetMemory(cpu, addr, M)
  SetFlagsBasedOn(M)
}

INXB: proc() {
  BC = GetBCUnsigned()
  BC++

  high = (BC >> 8) & 255
  low = BC & 255

  SetValueI(cpu.B, high)
  SetValueI(cpu.C, low)
}

INXD: proc() {
  DE = GetDEUnsigned()
  DE++

  high = (DE >> 8) & 255
  low = DE & 255

  SetValueI(cpu.D, high)
  SetValueI(cpu.E, low)
}

INXH: proc() {
  HL = GetHLUnsigned()
  HL++

  high = (HL >> 8) & 255
  low = HL & 255

  SetValueI(cpu.H, high)
  SetValueI(cpu.L, low)
}

INXSP: proc() {
  stack = cpu.stack
  stack.SP = stack.SP + 1
}

// Conditional jump. Does not jump if flag is "skip_if"
JumpCond: proc(flag: byte, skip_if: int) {
  if (skip_if == GetBit(cpu.Flags, flag)) {
    cpu.PC = cpu.PC + 2
    return
  }

  addr = GetNextPC16()

  cpu.PC = addr - 1
}

JC: proc() {
  JumpCond(CARRY_FLAG, 0)
}

JM: proc() {
  JumpCond(SIGN_FLAG, 0)
}

JMP: proc() {
  addr = GetNextPC16()
  cpu.PC = addr - 1
}

JNC: proc() {
  JumpCond(CARRY_FLAG, 1)
}

JNZ: proc() {
  JumpCond(ZERO_FLAG, 1)
}

JP: proc() {
  JumpCond(SIGN_FLAG, 1)
}

JPE: proc() {
  exit "JPE not implemented"
}

JPO: proc() {
  exit "JPO not implemented"
}

JZ: proc() {
  JumpCond(ZERO_FLAG, 0)
}

LDA: proc() {
  addr = GetNextPC16()
  SetValue(cpu.A, cpu.memory[addr])
}

LDAXB: proc() {
  addr = GetBCUnsigned()
  SetValue(cpu.A, cpu.memory[addr])
}

LDAXD: proc() {
  addr = GetDEUnsigned()
  SetValue(cpu.A, cpu.memory[addr])
}

LHLD: proc() {
  addr = GetNextPC16()
  SetValue(cpu.L, cpu.memory[addr])
  SetValue(cpu.H, cpu.memory[addr + 1])
}

LXIB: proc() {
  val = GetNextPC16()

  high = (val >> 8) & 255
  low = val & 255

  SetValueI(cpu.B, high)
  SetValueI(cpu.C, low)
}

LXID: proc() {
  val = GetNextPC16()

  high = (val >> 8) & 255
  low = val & 255

  SetValueI(cpu.D, high)
  SetValueI(cpu.E, low)
}

LXIH: proc() {
  val = GetNextPC16()

  high = (val >> 8) & 255
  low = val & 255

  SetValueI(cpu.H, high)
  SetValueI(cpu.L, low)
}

LXISP: proc() {
  exit "LXISP not implemented"
  //val = GetNextPC16()

  //cpu->SP->Set(val)
}

// Move from 'from' to 'to'
MOV: proc(to: Register, from: Register) {
  SetValue(to, from.v)
}

MOVAA: proc() { }
MOVAB: proc() { MOV(cpu.A, cpu.B) }
MOVAC: proc() { MOV(cpu.A, cpu.C) }
MOVAD: proc() { MOV(cpu.A, cpu.D) }
MOVAE: proc() { MOV(cpu.A, cpu.E) }
MOVAH: proc() { MOV(cpu.A, cpu.H) }
MOVAL: proc() { MOV(cpu.A, cpu.L) }
MOVAM: proc() { SetValue(cpu.A, GetM(cpu)) }

MOVBA: proc() { MOV(cpu.B, cpu.A) }
MOVBB: proc() { }
MOVBC: proc() { MOV(cpu.B, cpu.C) }
MOVBD: proc() { MOV(cpu.B, cpu.D) }
MOVBE: proc() { MOV(cpu.B, cpu.E) }
MOVBH: proc() { MOV(cpu.B, cpu.H) }
MOVBL: proc() { MOV(cpu.B, cpu.L) }
MOVBM: proc() { SetValue(cpu.B, GetM(cpu)) }

MOVCA: proc() { MOV(cpu.C, cpu.A) }
MOVCB: proc() { MOV(cpu.C, cpu.B) }
MOVCC: proc() { }
MOVCD: proc() { MOV(cpu.C, cpu.D) }
MOVCE: proc() { MOV(cpu.C, cpu.E) }
MOVCH: proc() { MOV(cpu.C, cpu.H) }
MOVCL: proc() { MOV(cpu.C, cpu.L) }
MOVCM: proc() { SetValue(cpu.C, GetM(cpu)) }

MOVDA: proc() { MOV(cpu.D, cpu.A) }
MOVDB: proc() { MOV(cpu.D, cpu.B) }
MOVDC: proc() { MOV(cpu.D, cpu.C) }
MOVDD: proc() { }
MOVDE: proc() { MOV(cpu.D, cpu.E) }
MOVDH: proc() { MOV(cpu.D, cpu.H) }
MOVDL: proc() { MOV(cpu.D, cpu.L) }
MOVDM: proc() { SetValue(cpu.D, GetM(cpu)) }

MOVEA: proc() { MOV(cpu.E, cpu.A) }
MOVEB: proc() { MOV(cpu.E, cpu.B) }
MOVEC: proc() { MOV(cpu.E, cpu.C) }
MOVED: proc() { MOV(cpu.E, cpu.D) }
MOVEE: proc() { }
MOVEH: proc() { MOV(cpu.E, cpu.H) }
MOVEL: proc() { MOV(cpu.E, cpu.L) }
MOVEM: proc() { SetValue(cpu.E, GetM(cpu)) }

MOVHA: proc() { MOV(cpu.H, cpu.A) }
MOVHB: proc() { MOV(cpu.H, cpu.B) }
MOVHC: proc() { MOV(cpu.H, cpu.C) }
MOVHD: proc() { MOV(cpu.H, cpu.D) }
MOVHE: proc() { MOV(cpu.H, cpu.E) }
MOVHH: proc() { }
MOVHL: proc() { MOV(cpu.H, cpu.L) }
MOVHM: proc() { SetValue(cpu.H, GetM(cpu)) }

MOVLA: proc() { MOV(cpu.L, cpu.A) }
MOVLB: proc() { MOV(cpu.L, cpu.B) }
MOVLC: proc() { MOV(cpu.L, cpu.C) }
MOVLD: proc() { MOV(cpu.L, cpu.D) }
MOVLE: proc() { MOV(cpu.L, cpu.E) }
MOVLH: proc() { MOV(cpu.L, cpu.H) }
MOVLL: proc() { }
MOVLM: proc() { SetValue(cpu.L, GetM(cpu)) }

MOVMA: proc() { SetMemory(cpu, GetHLUnsigned(), cpu.A.v) }
MOVMB: proc() { SetMemory(cpu, GetHLUnsigned(), cpu.B.v) }
MOVMC: proc() { SetMemory(cpu, GetHLUnsigned(), cpu.C.v) }
MOVMD: proc() { SetMemory(cpu, GetHLUnsigned(), cpu.D.v) }
MOVME: proc() { SetMemory(cpu, GetHLUnsigned(), cpu.E.v) }
MOVMH: proc() { SetMemory(cpu, GetHLUnsigned(), cpu.H.v) }
MOVML: proc() { SetMemory(cpu, GetHLUnsigned(), cpu.L.v) }

MVIA: proc() { SetValue(cpu.A, NextPC(cpu)) }
MVIB: proc() { SetValue(cpu.B, NextPC(cpu)) }
MVIC: proc() { SetValue(cpu.C, NextPC(cpu)) }
MVID: proc() { SetValue(cpu.D, NextPC(cpu)) }
MVIE: proc() { SetValue(cpu.E, NextPC(cpu)) }
MVIH: proc() { SetValue(cpu.H, NextPC(cpu)) }
MVIL: proc() { SetValue(cpu.L, NextPC(cpu)) }
MVIM: proc() { SetMemory(cpu, GetHLUnsigned(), NextPC(cpu)) }

NOP: proc() { }

ORAx: proc(other: byte) {
  result = cpu.A.v | other
  SetValue(cpu.A, result)
  SetFlags(cpu,
    (result & 0y80) != 0y00,  // sign
    result == 0y00,           // zero
    false                     // always clear carry
  )
}

ORAA: proc() { ORAx(cpu.A.v) }
ORAB: proc() { ORAx(cpu.B.v) }
ORAC: proc() { ORAx(cpu.C.v) }
ORAD: proc() { ORAx(cpu.D.v) }
ORAE: proc() { ORAx(cpu.E.v) }
ORAH: proc() { ORAx(cpu.H.v) }
ORAL: proc() { ORAx(cpu.L.v) }
ORAM: proc() { ORAx(GetM(cpu)) }
ORI: proc() { ORAx(NextPC(cpu)) }

PCHL: proc() {
  addr = GetHLUnsigned()
  cpu.PC = addr - 1
}

POPB: proc() {
  SetValue(cpu.C, Pop(cpu))
  SetValue(cpu.B, Pop(cpu))
}

POPD: proc() {
  SetValue(cpu.E, Pop(cpu))
  SetValue(cpu.D, Pop(cpu))
}

POPH: proc() {
  SetValue(cpu.L, Pop(cpu))
  SetValue(cpu.H, Pop(cpu))
}

POPPSW: proc() {
  SetValue(cpu.Flags, Pop(cpu))
  SetValue(cpu.A, Pop(cpu))
}

PUSHB: proc() {
  Push(cpu.stack, cpu.B.v)
  Push(cpu.stack, cpu.C.v)
}

PUSHD: proc() {
  Push(cpu.stack, cpu.D.v)
  Push(cpu.stack, cpu.E.v)
}

PUSHH: proc() {
  Push(cpu.stack, cpu.H.v)
  Push(cpu.stack, cpu.L.v)
}

PUSHPSW: proc() {
  Push(cpu.stack, cpu.A.v)
  Push(cpu.stack, cpu.Flags.v)
}

RAL: proc() {
  A = btoi(cpu.A.v)

  carryOut = (A & 128) > 0

  A = A << 1

  SetValue(cpu.A, itob(A))
  SetBit(cpu.A, 0y00, 1 == GetBit(cpu.Flags, CARRY_FLAG))
  SetBit(cpu.Flags, CARRY_FLAG, carryOut)
}

RAR: proc() {
  A = btoi(cpu.A.v)

  carryOut = (A & 1) > 0

  A = A >> 1

  SetValue(cpu.A, itob(A))
  SetBit(cpu.A, 0y07, 1 == GetBit(cpu.Flags, CARRY_FLAG))
  SetBit(cpu.Flags, CARRY_FLAG, carryOut)
}

// Conditional return, if flag != skip_if
ReturnCond: proc(flag: byte, skip_if: int) {
  if (skip_if == GetBit(cpu.Flags, flag)) {
    return
  }

  low = btoi(Pop(cpu)) & 255
  high = btoi(Pop(cpu)) & 255

  addr = (high << 8) | low

  cpu.PC = addr - 1
}

RC: proc() {
  ReturnCond(CARRY_FLAG, 0)
}

RET: proc() {
  low = btoi(Pop(cpu)) & 255
  high = btoi(Pop(cpu)) & 255

  addr = (high << 8) | low

  cpu.PC = addr - 1
}

RLC: proc() {
  A = btoi(cpu.A.v)

  carryOut = (A & 128) > 0

  A = A << 1
  SetValueI(cpu.A, A)

  SetBit(cpu.Flags, CARRY_FLAG, carryOut)
  SetBit(cpu.A, 0y00, carryOut)
}

// Undocumented opcode: HL=HL-BC
DSUB: proc() {
  exit "DSUB not implemented"
}

RM: proc() {
  ReturnCond(SIGN_FLAG, 0)
}

RNC: proc() {
  ReturnCond(CARRY_FLAG, 1)
}

RNZ: proc() {
  ReturnCond(ZERO_FLAG, 1)
}

RP: proc() {
  ReturnCond(SIGN_FLAG, 1)
}

RPE: proc() {
  exit "RPE not implemented"
}

RPO: proc() {
  exit "RPO not implemented"
}

RRC: proc() {
  A = cpu.A.v

  carryOut = (A & 0y01) > 0y00

  A = A >> 0y01
  SetValue(cpu.A, A)

  SetBit(cpu.Flags, CARRY_FLAG, carryOut)
  SetBit(cpu.A, 0y07, carryOut)
}


RZ: proc() {
  ReturnCond(ZERO_FLAG, 0)
}

// Returns the twos-complement of the input,
// as an int, but it is NOT sign-extended
TwosComp: proc(b: byte): int {
  return btoi(-b)
}

SBBx: proc(otherRaw: byte) {
  A = btoi(cpu.A.v) & 255
  otherI = btoi(otherRaw) & 255

  carryIn = GetBit(cpu.Flags, CARRY_FLAG)
  carryInB = itob(carryIn)
  carryOut = A < (otherI + carryIn)

  // This seems oddly long. Why not res = A - other?
  res = A + TwosComp(otherRaw) + TwosComp(carryInB)

  SetValueI(cpu.A, res)
  SetFlagsBasedOnI(res)
  SetBit(cpu.Flags, CARRY_FLAG, carryOut)
}

// This was not tested
SBBA: proc() {
  A = btoi(cpu.A.v)

  carryIn = GetBit(cpu.Flags, CARRY_FLAG)

  other = (!(A+carryIn)) + 1

  res = A + other

  SetValueI(cpu.A, res)
  SetFlagsBasedOnI(res)
  SetCarry(res)
}

SBBB: proc() { SBBx(cpu.B.v) }
SBBC: proc() { SBBx(cpu.C.v) }
SBBD: proc() { SBBx(cpu.D.v) }
SBBE: proc() { SBBx(cpu.E.v) }
SBBH: proc() { SBBx(cpu.H.v) }
SBBL: proc() { SBBx(cpu.L.v) }
SBBM: proc() { SBBx(GetM(cpu)) }
SBI: proc() { SBBx(NextPC(cpu)) }

SHLD: proc() {
  addr = GetNextPC16()

  SetMemory(cpu, addr, cpu.L.v)
  SetMemory(cpu, addr + 1, cpu.H.v)
}

SPHL: proc() {
  stack = cpu.stack
  stack.SP = GetHLUnsigned()
}

STA: proc() {
  addr = GetNextPC16()
  SetMemory(cpu, addr, cpu.A.v)
}

STAXB: proc() {
  addr = GetBCUnsigned()
  SetMemory(cpu, addr, cpu.A.v)
}

STAXD: proc() {
  addr = GetDEUnsigned()
  SetMemory(cpu, addr, cpu.A.v)
}

STC: proc() {
  SetBit(cpu.Flags, CARRY_FLAG, true)
}

SetCarry: proc(res: int) {
  SetBit(cpu.Flags, CARRY_FLAG, (res & 256) == 256)
}

// TODO: this may be wrong, but no test code uses it yet...
SUBx: proc(otherRaw: byte) {
  A = btoi(cpu.A.v) & 255

  other = (!(btoi(otherRaw))) + 1
  res = A + other

  SetValueI(cpu.A, res)
  SetFlagsBasedOnI(res)
  SetCarry(res)

}

SUBA: proc() {
  A = btoi(cpu.A.v)

  other = A

  // This may be wrong, based on how SBB is implemented.
  res = A - other

  SetValueI(cpu.A, res)
  SetFlagsBasedOnI(A)
  SetCarry(res)
}

SUBB: proc() { SUBx(cpu.B.v) }
SUBC: proc() { SUBx(cpu.C.v) }
SUBD: proc() { SUBx(cpu.D.v) }
SUBE: proc() { SUBx(cpu.E.v) }
SUBH: proc() { SUBx(cpu.H.v) }
SUBL: proc() { SUBx(cpu.L.v) }
SUBM: proc() { SUBx(GetM(cpu)) }
SUI: proc() { SUBx(NextPC(cpu)) }

XCHG: proc() {
  H = cpu.H.v
  L = cpu.L.v

  D = cpu.D.v
  E = cpu.E.v

  SetValue(cpu.H, D)
  SetValue(cpu.L, E)
  SetValue(cpu.D, H)
  SetValue(cpu.E, L)
}

// XOR A with the given other
XRAx: proc(other: byte) {
  // TODO: set flags. 
  SetValue(cpu.A, cpu.A.v ^ other)
}

// I think none of these are tested...
XRAA: proc() { SetValue(cpu.A, 0y00) }
XRAB: proc() { XRAx(cpu.B.v) }
XRAC: proc() { XRAx(cpu.C.v) }
XRAD: proc() { XRAx(cpu.D.v) }
XRAE: proc() { XRAx(cpu.E.v) }
XRAH: proc() { XRAx(cpu.H.v) }
XRAL: proc() { XRAx(cpu.L.v) }
XRAM: proc() { XRAx(GetM(cpu)) }
XRI: proc() { XRAx(NextPC(cpu)) }

XTHL: proc() {
  exit "XTHL not implemented"
  // this can't be right...? throws away the values?
  //Pop(cpu)
  //Pop(cpu)

  //Push(cpu.stack, cpu.H.v)
  //Push(cpu.stack, cpu.L.v)
}

OPCODES = [
  "NOP", // 0x0
  "LXI B", // 0x1
  "STAX B", // 0x2
  "INX B", // 0x3
  "INR B", // 0x4
  "DCR B", // 0x5
  "MVI B", // 0x6
  "RLC", // 0x7
  "DSUB", // 0x8
  "DAD B", // 0x9
  "LDAX B", // 0xa
  "DCX B", // 0xb
  "INR C", // 0xc
  "DCR C", // 0xd
  "MVI C", // 0xe
  "RRC", // 0xf
  "",
  "LXI D", // 0x11
  "STAX D", // 0x12
  "INX D", // 0x13
  "INR D", // 0x14
  "DCR D", // 0x15
  "MVI D", // 0x16
  "RAL", // 0x17
  "",
  "DAD D", // 0x19
  "LDAX D", // 0x1a
  "DCX D", // 0x1b
  "INR E", // 0x1c
  "DCR E", // 0x1d
  "MVI E", // 0x1e
  "RAR", // 0x1f
  "RIM", // 0x20
  "LXI H", // 0x21
  "SHLD", // 0x22
  "INX H", // 0x23
  "INR H", // 0x24
  "DCR H", // 0x25
  "MVI H", // 0x26
  "DAA", // 0x27
  "",
  "DAD H", // 0x29
  "LHLD", // 0x2a
  "DCX H", // 0x2b
  "INR L", // 0x2c
  "DCR L", // 0x2d
  "MVI L", // 0x2e
  "CMA", // 0x2f
  "SIM", // 0x30
  "LXI SP", // 0x31
  "STA", // 0x32
  "INX SP", // 0x33
  "INR M", // 0x34
  "DCR M", // 0x35
  "MVI M", // 0x36
  "STC", // 0x37
  "",
  "DAD SP", // 0x39
  "LDA", // 0x3a
  "DCX SP", // 0x3b
  "INR A", // 0x3c
  "DCR A", // 0x3d
  "MVI A", // 0x3e
  "CMC", // 0x3f
  "MOV B, B", // 0x40
  "MOV B, C", // 0x41
  "MOV B, D", // 0x42
  "MOV B, E", // 0x43
  "MOV B, H", // 0x44
  "MOV B, L", // 0x45
  "MOV B, M", // 0x46
  "MOV B, A", // 0x47
  "MOV C, B", // 0x48
  "MOV C, C", // 0x49
  "MOV C, D", // 0x4a
  "MOV C, E", // 0x4b
  "MOV C, H", // 0x4c
  "MOV C, L", // 0x4d
  "MOV C, M", // 0x4e
  "MOV C, A", // 0x4f
  "MOV D, B", // 0x50
  "MOV D, C", // 0x51
  "MOV D, D", // 0x52
  "MOV D, E", // 0x53
  "MOV D, H", // 0x54
  "MOV D, L", // 0x55
  "MOV D, M", // 0x56
  "MOV D, A", // 0x57
  "MOV E, B", // 0x58
  "MOV E, C", // 0x59
  "MOV E, D", // 0x5a
  "MOV E, E", // 0x5b
  "MOV E, H", // 0x5c
  "MOV E, L", // 0x5d
  "MOV E, M", // 0x5e
  "MOV E, A", // 0x5f
  "MOV H, B", // 0x60
  "MOV H, C", // 0x61
  "MOV H, D", // 0x62
  "MOV H, E", // 0x63
  "MOV H, H", // 0x64
  "MOV H, L", // 0x65
  "MOV H, M", // 0x66
  "MOV H, A", // 0x67
  "MOV L, B", // 0x68
  "MOV L, C", // 0x69
  "MOV L, D", // 0x6a
  "MOV L, E", // 0x6b
  "MOV L, H", // 0x6c
  "MOV L, L", // 0x6d
  "MOV L, M", // 0x6e
  "MOV L, A", // 0x6f
  "MOV M, B", // 0x70
  "MOV M, C", // 0x71
  "MOV M, D", // 0x72
  "MOV M, E", // 0x73
  "MOV M, H", // 0x74
  "MOV M, L", // 0x75
  "HLT", // 0x76
  "MOV M, A", // 0x77
  "MOV A, B", // 0x78
  "MOV A, C", // 0x79
  "MOV A, D", // 0x7a
  "MOV A, E", // 0x7b
  "MOV A, H", // 0x7c
  "MOV A, L", // 0x7d
  "MOV A, M", // 0x7e
  "MOV A, A", // 0x7f
  "ADD B", // 0x80
  "ADD C", // 0x81
  "ADD D", // 0x82
  "ADD E", // 0x83
  "ADD H", // 0x84
  "ADD L", // 0x85
  "ADD M", // 0x86
  "ADD A", // 0x87
  "ADC B", // 0x88
  "ADC C", // 0x89
  "ADC D", // 0x8a
  "ADC E", // 0x8b
  "ADC H", // 0x8c
  "ADC L", // 0x8d
  "ADC M", // 0x8e
  "ADC A", // 0x8f
  "SUB B", // 0x90
  "SUB C", // 0x91
  "SUB D", // 0x92
  "SUB E", // 0x93
  "SUB H", // 0x94
  "SUB L", // 0x95
  "SUB M", // 0x96
  "SUB A", // 0x97
  "SBB B", // 0x98
  "SBB C", // 0x99
  "SBB D", // 0x9a
  "SBB E", // 0x9b
  "SBB H", // 0x9c
  "SBB L", // 0x9d
  "SBB M", // 0x9e
  "SBB A", // 0x9f
  "ANA B", // 0xa0
  "ANA C", // 0xa1
  "ANA D", // 0xa2
  "ANA E", // 0xa3
  "ANA H", // 0xa4
  "ANA L", // 0xa5
  "ANA M", // 0xa6
  "ANA A", // 0xa7
  "XRA B", // 0xa8
  "XRA C", // 0xa9
  "XRA D", // 0xaa
  "XRA E", // 0xab
  "XRA H", // 0xac
  "XRA L", // 0xad
  "XRA M", // 0xae
  "XRA A", // 0xaf
  "ORA B", // 0xb0
  "ORA C", // 0xb1
  "ORA D", // 0xb2
  "ORA E", // 0xb3
  "ORA H", // 0xb4
  "ORA L", // 0xb5
  "ORA M", // 0xb6
  "ORA A", // 0xb7
  "CMP B", // 0xb8
  "CMP C", // 0xb9
  "CMP D", // 0xba
  "CMP E", // 0xbb
  "CMP H", // 0xbc
  "CMP L", // 0xbd
  "CMP M", // 0xbe
  "CMP A", // 0xbf
  "RNZ", // 0xc0
  "POP B", // 0xc1
  "JNZ", // 0xc2
  "JMP", // 0xc3
  "CNZ", // 0xc4
  "PUSH B", // 0xc5
  "ADI", // 0xc6
  "RST", // 0xc7}
  "RZ", // 0xc8
  "RET", // 0xc9
  "JZ", // 0xca
  "",
  "CZ", // 0xcc
  "CALL", // 0xcd
  "ACI", // 0xce
  "RST", // 0xcf}
  "RNC", // 0xd0
  "POP D", // 0xd1
  "JNC", // 0xd2
  "OUT", // 0xd3
  "CNC", // 0xd4
  "PUSH D", // 0xd5
  "SUI", // 0xd6
  "RST", // 0xd7}
  "RC", // 0xd8
  "",
  "JC", // 0xda
  "IN", // 0xdb
  "CC", // 0xdc
  "",
  "SBI", // 0xde
  "RST", // 0xdf}
  "RPO", // 0xe0
  "POP H", // 0xe1
  "JPO", // 0xe2
  "XTHL", // 0xe3
  "CPO", // 0xe4
  "PUSH H", // 0xe5
  "ANI", // 0xe6
  "RST", // 0xe7}
  "RPE", // 0xe8
  "PCHL", // 0xe9
  "JPE", // 0xea
  "XCHG", // 0xeb
  "CPE", // 0xec
  "",
  "XRI", // 0xee
  "RST", // 0xef}
  "RP", // 0xf0
  "POP PSW", // 0xf1
  "JP", // 0xf2
  "DI", // 0xf3
  "CP", // 0xf4
  "PUSH PSW", // 0xf5
  "ORI", // 0xf6
  "RST", // 0xf7}
  "RM", // 0xf8
  "SPHL", // 0xf9
  "JM", // 0xfa
  "EI", // 0xfb
  "CM", // 0xfc
  "",
  "CPI", // 0xfe
  "RST" // 0xff
]

printOp: proc(cpu: CPU) {
  op = cpu.memory[cpu.PC]
  posop = btoi(op) & 255
  print "\t" print OPCODES[posop] print ": " print op
  if cpu.PC < 65534 {
    print " " print cpu.memory[cpu.PC + 1]
    if cpu.PC < 65533 {
      print " " print cpu.memory[cpu.PC + 2]
    }
  }
  println ""
}

executeCurrentOp: proc(cpu: CPU) {
  op = cpu.memory[cpu.PC]
  if cpu.debug { printOp(cpu) }
  if op == 0y00 { NOP() }
  elif op == 0y01 { LXIB() }
  elif op == 0y02 { STAXB() }
  elif op == 0y03 { INXB() }
  elif op == 0y04 { INRB() }
  elif op == 0y05 { DCRB() }
  elif op == 0y06 { MVIB() }
  elif op == 0y07 { RLC() }
  elif op == 0y08 { DSUB() }
  elif op == 0y09 { DADB() }
  elif op == 0y0a { LDAXB() }
  elif op == 0y0b { DCXB() }
  elif op == 0y0c { INRC() }
  elif op == 0y0d { DCRC() }
  elif op == 0y0e { MVIC() }
  elif op == 0y0f { RRC() }
  elif op == 0y11 { LXID() }
  elif op == 0y12 { STAXD() }
  elif op == 0y13 { INXD() }
  elif op == 0y14 { INRD() }
  elif op == 0y15 { DCRD() }
  elif op == 0y16 { MVID() }
  elif op == 0y17 { RAL() }
  elif op == 0y19 { DADD() }
  elif op == 0y1a { LDAXD() }
  elif op == 0y1b { DCXD() }
  elif op == 0y1c { INRE() }
  elif op == 0y1d { DCRE() }
  elif op == 0y1e { MVIE() }
  elif op == 0y1f { RAR() }
  elif op == 0y21 { LXIH() }
  elif op == 0y22 { SHLD() }
  elif op == 0y23 { INXH() }
  elif op == 0y24 { INRH() }
  elif op == 0y25 { DCRH() }
  elif op == 0y26 { MVIH() }
  elif op == 0y27 { DAA() }
  elif op == 0y29 { DADH() }
  elif op == 0y2a { LHLD() }
  elif op == 0y2b { DCXH() }
  elif op == 0y2c { INRL() }
  elif op == 0y2d { DCRL() }
  elif op == 0y2e { MVIL() }
  elif op == 0y2f { CMA() }
  elif op == 0y31 { LXISP() }
  elif op == 0y32 { STA() }
  elif op == 0y33 { INXSP() }
  elif op == 0y34 { INRM() }
  elif op == 0y35 { DCRM() }
  elif op == 0y36 { MVIM() }
  elif op == 0y37 { STC() }
  elif op == 0y39 { DADSP() }
  elif op == 0y3a { LDA() }
  elif op == 0y3b { DCXSP() }
  elif op == 0y3c { INRA() }
  elif op == 0y3d { DCRA() }
  elif op == 0y3e { MVIA() }
  elif op == 0y3f { CMC() }
  elif op == 0y40 { MOVBB() }
  elif op == 0y41 { MOVBC() }
  elif op == 0y42 { MOVBD() }
  elif op == 0y43 { MOVBE() }
  elif op == 0y44 { MOVBH() }
  elif op == 0y45 { MOVBL() }
  elif op == 0y46 { MOVBM() }
  elif op == 0y47 { MOVBA() }
  elif op == 0y48 { MOVCB() }
  elif op == 0y49 { MOVCC() }
  elif op == 0y4a { MOVCD() }
  elif op == 0y4b { MOVCE() }
  elif op == 0y4c { MOVCH() }
  elif op == 0y4d { MOVCL() }
  elif op == 0y4e { MOVCM() }
  elif op == 0y4f { MOVCA() }
  elif op == 0y50 { MOVDB() }
  elif op == 0y51 { MOVDC() }
  elif op == 0y52 { MOVDD() }
  elif op == 0y53 { MOVDE() }
  elif op == 0y54 { MOVDH() }
  elif op == 0y55 { MOVDL() }
  elif op == 0y56 { MOVDM() }
  elif op == 0y57 { MOVDA() }
  elif op == 0y58 { MOVEB() }
  elif op == 0y59 { MOVEC() }
  elif op == 0y5a { MOVED() }
  elif op == 0y5b { MOVEE() }
  elif op == 0y5c { MOVEH() }
  elif op == 0y5d { MOVEL() }
  elif op == 0y5e { MOVEM() }
  elif op == 0y5f { MOVEA() }
  elif op == 0y60 { MOVHB() }
  elif op == 0y61 { MOVHC() }
  elif op == 0y62 { MOVHD() }
  elif op == 0y63 { MOVHE() }
  elif op == 0y64 { MOVHH() }
  elif op == 0y65 { MOVHL() }
  elif op == 0y66 { MOVHM() }
  elif op == 0y67 { MOVHA() }
  elif op == 0y68 { MOVLB() }
  elif op == 0y69 { MOVLC() }
  elif op == 0y6a { MOVLD() }
  elif op == 0y6b { MOVLE() }
  elif op == 0y6c { MOVLH() }
  elif op == 0y6d { MOVLL() }
  elif op == 0y6e { MOVLM() }
  elif op == 0y6f { MOVLA() }
  elif op == 0y70 { MOVMB() }
  elif op == 0y71 { MOVMC() }
  elif op == 0y72 { MOVMD() }
  elif op == 0y73 { MOVME() }
  elif op == 0y74 { MOVMH() }
  elif op == 0y75 { MOVML() }
  elif op == 0y76 { HLT() }
  elif op == 0y77 { MOVMA() }
  elif op == 0y78 { MOVAB() }
  elif op == 0y79 { MOVAC() }
  elif op == 0y7a { MOVAD() }
  elif op == 0y7b { MOVAE() }
  elif op == 0y7c { MOVAH() }
  elif op == 0y7d { MOVAL() }
  elif op == 0y7e { MOVAM() }
  elif op == 0y7f { MOVAA() }
  elif op == 0y80 { ADDB() }
  elif op == 0y81 { ADDC() }
  elif op == 0y82 { ADDD() }
  elif op == 0y83 { ADDE() }
  elif op == 0y84 { ADDH() }
  elif op == 0y85 { ADDL() }
  elif op == 0y86 { ADDM() }
  elif op == 0y87 { ADDA() }
  elif op == 0y88 { ADCB() }
  elif op == 0y89 { ADCC() }
  elif op == 0y8a { ADCD() }
  elif op == 0y8b { ADCE() }
  elif op == 0y8c { ADCH() }
  elif op == 0y8d { ADCL() }
  elif op == 0y8e { ADCM() }
  elif op == 0y8f { ADCA() }
  elif op == 0y90 { SUBB() }
  elif op == 0y91 { SUBC() }
  elif op == 0y92 { SUBD() }
  elif op == 0y93 { SUBE() }
  elif op == 0y94 { SUBH() }
  elif op == 0y95 { SUBL() }
  elif op == 0y96 { SUBM() }
  elif op == 0y97 { SUBA() }
  elif op == 0y98 { SBBB() }
  elif op == 0y99 { SBBC() }
  elif op == 0y9a { SBBD() }
  elif op == 0y9b { SBBE() }
  elif op == 0y9c { SBBH() }
  elif op == 0y9d { SBBL() }
  elif op == 0y9e { SBBM() }
  elif op == 0y9f { SBBA() }
  elif op == 0ya0 { ANAB() }
  elif op == 0ya1 { ANAC() }
  elif op == 0ya2 { ANAD() }
  elif op == 0ya3 { ANAE() }
  elif op == 0ya4 { ANAH() }
  elif op == 0ya5 { ANAL() }
  elif op == 0ya6 { ANAM() }
  elif op == 0ya7 { ANAA() }
  elif op == 0ya8 { XRAB() }
  elif op == 0ya9 { XRAC() }
  elif op == 0yaa { XRAD() }
  elif op == 0yab { XRAE() }
  elif op == 0yac { XRAH() }
  elif op == 0yad { XRAL() }
  elif op == 0yae { XRAM() }
  elif op == 0yaf { XRAA() }
  elif op == 0yb0 { ORAB() }
  elif op == 0yb1 { ORAC() }
  elif op == 0yb2 { ORAD() }
  elif op == 0yb3 { ORAE() }
  elif op == 0yb4 { ORAH() }
  elif op == 0yb5 { ORAL() }
  elif op == 0yb6 { ORAM() }
  elif op == 0yb7 { ORAA() }
  elif op == 0yb8 { CMPB() }
  elif op == 0yb9 { CMPC() }
  elif op == 0yba { CMPD() }
  elif op == 0ybb { CMPE() }
  elif op == 0ybc { CMPH() }
  elif op == 0ybd { CMPL() }
  elif op == 0ybe { CMPM() }
  elif op == 0ybf { CMPA() }
  elif op == 0yc0 { RNZ() }
  elif op == 0yc1 { POPB() }
  elif op == 0yc2 { JNZ() }
  elif op == 0yc3 { JMP() }
  elif op == 0yc4 { CNZ() }
  elif op == 0yc5 { PUSHB() }
  elif op == 0yc6 { ADI() }
  elif op == 0yc8 { RZ() }
  elif op == 0yc9 { RET() }
  elif op == 0yca { JZ() }
  elif op == 0ycc { CZ() }
  elif op == 0ycd { CALL() }
  elif op == 0yce { ACI() }
  elif op == 0yd0 { RNC() }
  elif op == 0yd1 { POPD() }
  elif op == 0yd2 { JNC() }
  elif op == 0yd4 { CNC() }
  elif op == 0yd5 { PUSHD() }
  elif op == 0yd6 { SUI() }
  elif op == 0yd8 { RC() }
  elif op == 0yda { JC() }
  elif op == 0ydc { CC() }
  elif op == 0yde { SBI() }
  elif op == 0ye0 { RPO() }
  elif op == 0ye1 { POPH() }
  elif op == 0ye2 { JPO() }
  elif op == 0ye3 { XTHL() }
  elif op == 0ye4 { CPO() }
  elif op == 0ye5 { PUSHH() }
  elif op == 0ye6 { ANI() }
  elif op == 0ye8 { RPE() }
  elif op == 0ye9 { PCHL() }
  elif op == 0yea { JPE() }
  elif op == 0yeb { XCHG() }
  elif op == 0yec { CPE() }
  elif op == 0yee { XRI() }
  elif op == 0yf0 { RP() }
  elif op == 0yf1 { POPPSW() }
  elif op == 0yf2 { JP() }
  elif op == 0yf4 { CP() }
  elif op == 0yf5 { PUSHPSW() }
  elif op == 0yf6 { ORI() }
  elif op == 0yf8 { RM() }
  elif op == 0yf9 { SPHL() }
  elif op == 0yfa { JM() }
  elif op == 0yfc { CM() }
  elif op == 0yfe { CPI() }
  else {
    print "Unknown op " println op
    exit 
  }
}


//////////////////////////////////////////////////////
//
// RUN!
//
//////////////////////////////////////////////////////
run: proc(cpu: CPU) {
  if cpu.org == -1 {
    exit "No origin set!"
  }

  while cpu.running {
    debug(cpu)

    executeCurrentOp(cpu)
    cpu.PC = cpu.PC + 1

    // TODO: change this to something like:
    // oldPc = cpu.PC
    // cpu.PC = cpu.PC + 1
    // runOneOp(cpu, oldPc)
    // But then many of the cpu.PC = cpu.PC - 1 need to be changed
  }

  if cpu.debug {
    printCpuState(cpu)
  }
}


//////////////////////////////////////////////////////
//
// DEBUG
//
//////////////////////////////////////////////////////

debug: proc(cpu: CPU) {
  if cpu.debug {
    printCpuState(cpu)
  }
}

printCpuState: proc(cpu: CPU) {
  println "\t-------------------------------------------------------"
  // A: 0y00 B: 0y00 C: 0y00 D: 0y00 E: 0y00 H: 0y00 L: 0y00
  // PC: FFFF  S:0 Z:0 C:0
  print "\tA: " print cpu.A.v
  print " B: " print cpu.B.v
  print " C: " print cpu.C.v
  print " D: " print cpu.D.v
  print " E: " print cpu.E.v
  print " H: " print cpu.H.v
  print " L: " println cpu.L.v
  print "\tPC: " printHex(cpu.PC)
  print "  S:" print GetBit(cpu.Flags, SIGN_FLAG)
  print " Z:" print GetBit(cpu.Flags, ZERO_FLAG)
  print " C:" println GetBit(cpu.Flags, CARRY_FLAG)
}

DIGITS="0123456789ABCDEF"
printHex: proc(num: int) {
   factor = 4096
   num = num & 65535
   i = 0 while i < 4 do i++ {
     digit = num / factor // int division
     print DIGITS[digit]
     num = num - digit * factor
     factor /= 16
   }
}


//////////////////////////////////////////////////////
//
// LOAD
//
//////////////////////////////////////////////////////
global_data: string

next_line_loc = 0
read_input: proc {
  global_data = input
}

// Get the next line. Returns null at EOF.
// NOTE: LAST LINE MUST END WITH \n
next_line: proc: String {
  line = ''
  len = length(global_data)
  while next_line_loc < len {
    ch = global_data[next_line_loc]
    next_line_loc = next_line_loc + 1
    if asc(ch) != asc('\n') {
      line = line + ch
    } else {
      return line
    }
  }
  // got to eof
  return null
}

hexToInt: proc(s: string, i: int, j: int, k: int, m:int): int {
  a = nibbleToByte(s[i])
  b = nibbleToByte(s[j])
  c = nibbleToByte(s[k])
  d = nibbleToByte(s[m])
  return (a << 12) + (b << 8) + (c << 4) + d
}

hexToByte: proc(s: string, i: int, j:int): byte {
  leftB = nibbleToByte(s[i])
  rightB = nibbleToByte(s[j])
  return itob((leftB << 4) + rightB)
}

nibbleToByte: proc(s: string): int {
  c = s[0]
  if c >= '0' and c <= '9' {
    return asc(c) - asc('0')
  }
  if c >= 'A' and c <= 'F' {
    return 10 + (asc(c) - asc('A'))
  }
  // lower case
  return 10 + (asc(c) - asc('a'))
}

// Read input, parse each line and write into memory
loadData: proc(cpu: CPU) {
  read_input()

  cpu.org = -1
  line = next_line()
  mem = cpu.memory
  while line != null {
    loc = ifind(line, "0x")
    if loc == 0 {
      addr = hexToInt(line, 2, 3, 4, 5)

      if cpu.org == -1 {
        // Set PC to first line of input
        cpu.PC = addr
        cpu.org = addr
      }
      // Parse input: write into memory
      mem[addr] = hexToByte(line, 12, 13)
    }
    line = next_line()
  }
}


//////////////////////////////////////////////////////
//
// MAIN
//
//////////////////////////////////////////////////////
main: proc {
  loadData(cpu)

  // run
  cpu.running = true
  run(cpu)
  cpu.running = false
}

main()

//i = 3 while i <= 18 do i++ {
  //printHex(cpu.org + i) print ": " println cpu.memory[cpu.org + i]
//}
