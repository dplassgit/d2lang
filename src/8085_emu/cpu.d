btoi: extern proc(b: byte): int
itob: extern proc(i: int): byte
itos: extern proc(i: int): string

// 8-bit register
Register8: record {
  v: byte
}

Increment: proc(r: Register8) { r.v = r.v + 0y01 }
Decrement: proc(r: Register8) { r.v = r.v - 0y01 }
SetValueI: proc(r: Register8, v: int) { r.v = itob(v) }
SetValue: proc(r: Register8, v: byte) { r.v = v }

// set bit number b to value v (low bit)
SetBit: proc(r: Register8, b: byte, v: byte) {
  // TODO: DO THIS
  exit("SetBit not done")
}

SetBitB: proc(r: Register8, b: byte, v: bool) {
  if v {
    SetBit(r, b, 0y01)
  } else {
    SetBit(r, b, 0y00)
  }
}

// Returning an int so we can do math
GetBit: proc(r: Register8, b: byte): int {
  // TODO: DO THIS
  exit("GetBit not done")
  return 0
}

// clear bit number b
ClearBit: proc(r: Register8, b: byte) {
  SetBit(r, b, 0y00)
}

STACK_START = 65534 // why not 65535?
Stack: record {
  SP: int
  stack_data: byte[65535]
}

newStack: proc: Stack {
  s = new Stack
  s.SP = STACK_START 
  return s
}

Push: proc(s: Stack, v: byte) {
  s.SP = s.SP - 1

  // Issue #55: support advanced l-values
  d = s.stack_data
  d[s.SP] = v
}

Pop: proc(cpu: CPU): byte {
  s = cpu.stack

  ret = s.stack_data[s.SP]
  d = s.stack_data
  d[s.SP] = 0y00
  s.SP = s.SP + 1

  if s.SP > STACK_START {
    exit "ERROR: POP on empty stack at PC " + itos(cpu.PC)
  }

  return ret
}

// Represents state of the CPU and memory
CPU: record {
  A: Register8
  B: Register8
  C: Register8
  D: Register8
  E: Register8
  H: Register8
  L: Register8
  Flags: Register8

  // really short
  PC: int

  running: bool
  memory: byte[65536]
  stack: Stack
}


newCpu: proc: CPU {
  cpu = new CPU
  cpu.A = new Register8
  cpu.B = new Register8
  cpu.C = new Register8
  cpu.D = new Register8
  cpu.E = new Register8
  cpu.H = new Register8
  cpu.L = new Register8

  cpu.PC = 0
  cpu.stack = newStack()
  return cpu
}

cpu = newCpu()


SetMemory: proc(cpu: CPU, addr: int, val: byte) {
  m = cpu.memory
  m[addr] = val
}


SIGN_FLAG=0y07
ZERO_FLAG=0y06
AUX_CARRY_FLAG=0y04
PARITY_FLAG=0y02
CARRY_FLAG=0y00

SetFlags: proc(cpu: CPU, sign: bool, zero: bool, carry: bool) {
  SetBitB(cpu.Flags, SIGN_FLAG, sign)
  SetBitB(cpu.Flags, ZERO_FLAG, zero)
  SetBitB(cpu.Flags, CARRY_FLAG, carry)
}

NextPC: proc(cpu: CPU): byte {
  cpu.PC = cpu.PC + 1
  ret = cpu.memory[cpu.PC]
  return ret
}

GetM: proc(cpu: CPU): byte {
  return cpu.memory[GetHLUnsigned()]
}


Step: proc(cpu: CPU) {
  if cpu.running {
    op = cpu.memory[cpu.PC]
    runOneOp(op)
    cpu.PC = cpu.PC + 1
  }
}



// Set sign, zero and parity flag, depending on a certain number.
// Not enough info to know carry.
SetFlagsBasedOn: proc(v: byte): void {
  SetBitB(cpu.Flags, SIGN_FLAG, v < 0y00)
  SetBitB(cpu.Flags, ZERO_FLAG, v == 0y00)
}

SetFlagsBasedOnI: proc(v: int): void {
  SetBitB(cpu.Flags, SIGN_FLAG, v < 0)
  SetBitB(cpu.Flags, ZERO_FLAG, v == 0)
}

//Get double register BC unsigned int
GetBCUnsigned: proc(): int {
  B = btoi(cpu.B.v) & 255
  C = btoi(cpu.C.v) & 255
  BC = (B << 8) | C
  return BC
}

//Get double register DE unsigned int
GetDEUnsigned: proc(): int {
  D = btoi(cpu.D.v) & 255
  E = btoi(cpu.D.v) & 255
  DE = (D << 8) | E
  return DE
}

//Get double register HL unsigned int
GetHLUnsigned: proc(): int {
  H = btoi(cpu.H.v) & 255
  L = btoi(cpu.L.v) & 255
  HL = (H << 8) | L
  return HL
}

//Get double register BC signed int16_t
GetBCSigned: proc(): int {
  B = btoi(cpu.B.v)
  C = btoi(cpu.C.v)
  BC = (B << 8) | C
  return BC
}

//Get double register DE signed int
GetDESigned: proc(): int {
  D = btoi(cpu.D.v)
  E = btoi(cpu.E.v)
  DE = (D << 8) | E
  return DE
}

//Get double register HL signed int
GetHLSigned: proc(): int {
  H = btoi(cpu.H.v)
  L = btoi(cpu.L.v)
  HL = (H << 8) | L
  return HL
}


//Get the next 2 bytes in memory, using PC
GetNextPC16: proc(): int {
  low = btoi(NextPC(cpu)) & 255 // unsigned
  high = btoi(NextPC(cpu)) & 255 // unsigned
  word = (high << 8) | low
  return word
}

//Compare Register A with another number. Set the flags accordingly.
Compare: proc(other: byte): void {
  A = cpu.A.v

  // signed (?)
  if (A < other) {
    SetBit(cpu.Flags, CARRY_FLAG, 0y01)
    ClearBit(cpu.Flags, ZERO_FLAG)
  } elif (A == other) {
    ClearBit(cpu.Flags, CARRY_FLAG)
    SetBit(cpu.Flags, ZERO_FLAG, 0y01)
  } elif (A > other) {
    ClearBit(cpu.Flags, CARRY_FLAG)
    ClearBit(cpu.Flags, ZERO_FLAG)
  }
}

//Add signed number to Register A
AddSigned: proc(data: byte): void {
  rA = btoi(cpu.A.v)

  // uint16_t result16 = (uint8_t)rA + (uint8_t)data;
  // int8_t result = rA + data;
  result = rA + btoi(data)

  //cpu->SetFlags(
    //result < 0, sign
    //result == 0, zero
    //(result4 & 0xf0) > 0, aux_c
    //!(bits_in(*(uint8_t*)&result) % 2), parity
    //(result16 & 0b100000000) > 0 carry
  //);
  SetFlags(cpu,
    result < 0,
    result == 0,
    (result & 128) > 0
  )

  SetValue(cpu.A, itob(result))
}

//Add signed number to Register A with carry
AddSignedWithCarry: proc(data: byte): void {
  rA = btoi(cpu.A.v)
  rC = GetBit(cpu.Flags, CARRY_FLAG)

  result = rA + btoi(data) + rC

  //    int8_t rA = cpu->A->GetSigned();
  //    int8_t rC = cpu->Flags->GetBit(CARRY_FLAG);

  //    uint16_t result16 = (uint8_t)rA + (uint8_t)data + (uint8_t)rC;
  //    int8_t result = rA + data + rC;

  //    cpu->SetFlags(
  //        result < 0, // sign
  //        result == 0, // zero
  //        (result4 & 0xf0) > 0, // aux_c
  //        !(bits_in(*(uint8_t*)&result) % 2), // parity
  //        (result16 & 0b100000000) > 0 // carry
  //    );
  SetFlags(cpu,
    result < 0,
    result == 0,
    (result & 128) > 0 // carry
  )

  SetValueI(cpu.A, result)
}

//Bitwise And Register A with another number.
BitAnd: proc(data: byte): void {
  rA = cpu.A.v

  result = rA & data

  //    cpu->SetFlags(
  //        (result & 0b10000000) > 0, // sign
  //        result == 0, // zero
  //        0, // aux_c
  //        !(bits_in(*(uint8_t*)&result) % 2), // parity
  //        0 // carry (!)
  //    );
  SetFlags(cpu,
          (result & 0yff) > 0y00, // sign
          result == 0y00, // zero
          false // carry
      )

  SetValue(cpu.A, result)
}

//-------------------Instructions--------------------

ACI: proc() {
  data = NextPC(cpu)
  AddSignedWithCarry(data)
}

ADCA: proc() {
  AddSignedWithCarry(cpu.A.v)
}

ADCB: proc() {
  AddSignedWithCarry(cpu.B.v)
}

ADCC: proc() {
  AddSignedWithCarry(cpu.C.v)
}

ADCD: proc() {
  AddSignedWithCarry(cpu.D.v)
}

ADCE: proc() {
  AddSignedWithCarry(cpu.E.v)
}

ADCH: proc() {
  AddSignedWithCarry(cpu.H.v)
}

ADCL: proc() {
  AddSignedWithCarry(cpu.L.v)
}

ADCM: proc() {
  AddSignedWithCarry(GetM(cpu))
}

ADDA: proc() {
  AddSigned(cpu.A.v)
}

ADDB: proc() {
  AddSigned(cpu.B.v)
}

ADDC: proc() {
  AddSigned(cpu.C.v)
}

ADDD: proc() {
  AddSigned(cpu.D.v)
}

ADDE: proc() {
  AddSigned(cpu.E.v)
}

ADDH: proc() {
  AddSigned(cpu.H.v)
}

ADDL: proc() {
  AddSigned(cpu.L.v)
}

ADDM: proc() {
  AddSigned(GetM(cpu))
}

ADI: proc() {
  AddSigned(NextPC(cpu))
}

ANAA: proc() {
  BitAnd(cpu.A.v)
}

ANAB: proc() {
  BitAnd(cpu.B.v)
}

ANAC: proc() {
  BitAnd(cpu.C.v)
}

ANAD: proc() {
  BitAnd(cpu.D.v)
}

ANAE: proc() {
  BitAnd(cpu.E.v)
}

ANAH: proc() {
  BitAnd(cpu.H.v)
}

ANAL: proc() {
  BitAnd(cpu.L.v)
}

ANAM: proc() {
  BitAnd(GetM(cpu))
}

ANI: proc() {
  BitAnd(NextPC(cpu))
}

CALL: proc() {
  addr = GetNextPC16()

  cpu.PC = cpu.PC + 1

  HIGH = itob(cpu.PC >> 8)
  LOW = itob(cpu.PC & 255)

  Push(cpu.stack, HIGH)
  Push(cpu.stack, LOW)

  cpu.PC = addr
}

CC: proc() {
  if (0==GetBit(cpu.Flags, CARRY_FLAG)) {
    cpu.PC = cpu.PC + 1
    cpu.PC = cpu.PC + 1
    return
  }

  addr = GetNextPC16()

  cpu.PC = cpu.PC + 1

  HIGH = itob(cpu.PC >> 8)
  LOW = itob(cpu.PC & 255)

  Push(cpu.stack, HIGH)
  Push(cpu.stack, LOW)

  cpu.PC = addr
}

CM: proc() {
  if (0 == GetBit(cpu.Flags, SIGN_FLAG)) {
    cpu.PC = cpu.PC + 1
    cpu.PC = cpu.PC + 1
    return
  }

  addr = GetNextPC16()

  cpu.PC = cpu.PC + 1

  HIGH = itob(cpu.PC >> 8)
  LOW = itob(cpu.PC & 255)

  Push(cpu.stack, HIGH)
  Push(cpu.stack, LOW)

  cpu.PC = addr
}

CMA: proc() {
  SetValue(cpu.A, !(cpu.A.v))
}

CMC: proc() {
  carry = GetBit(cpu.Flags, CARRY_FLAG) == 1
  SetBitB(cpu.Flags, CARRY_FLAG, not carry)
}

CMPA: proc() {
  // compare a with a
  ClearBit(cpu.Flags, CARRY_FLAG)
  SetBit(cpu.Flags, ZERO_FLAG, 0y01)
}

CMPB: proc() {
  B = cpu.B.v
  Compare(B)
}

CMPC: proc() {
  C = cpu.C.v
  Compare(C)
}

CMPD: proc() {
  D = cpu.D.v
  Compare(D)
}

CMPE: proc() {
  E = cpu.E.v
  Compare(E)
}

CMPH: proc() {
  H = cpu.H.v
  Compare(H)
}

CMPL: proc() {
  L = cpu.L.v
  Compare(L)
}

CMPM: proc() {
  M = GetM(cpu)
  Compare(M)
}

CNC: proc() {
  if (1==GetBit(cpu.Flags, CARRY_FLAG)) {
    cpu.PC = cpu.PC + 1
    cpu.PC = cpu.PC + 1
    return
  }

  addr = GetNextPC16()

  cpu.PC = cpu.PC + 1

  HIGH = itob(cpu.PC >> 8)
  LOW = itob(cpu.PC & 255)

  Push(cpu.stack, HIGH)
  Push(cpu.stack, LOW)

  cpu.PC = addr
}

CNZ: proc() {
  if (1==GetBit(cpu.Flags, ZERO_FLAG)) {
    cpu.PC = cpu.PC + 1
    cpu.PC = cpu.PC + 1
    return
  }

  addr = GetNextPC16()

  cpu.PC = cpu.PC + 1

  HIGH = itob(cpu.PC >> 8)
  LOW = itob(cpu.PC & 255)

  Push(cpu.stack, HIGH)
  Push(cpu.stack, LOW)

  cpu.PC = addr
}

CP: proc() {
  if (1==GetBit(cpu.Flags, SIGN_FLAG)) {
    cpu.PC = cpu.PC + 1
    cpu.PC = cpu.PC + 1
    return
  }

  addr = GetNextPC16()

  cpu.PC = cpu.PC + 1

  HIGH = itob(cpu.PC >> 8)
  LOW = itob(cpu.PC & 255)

  Push(cpu.stack, HIGH)
  Push(cpu.stack, LOW)

  cpu.PC = addr
}

CPE: proc() {
  if (0 == GetBit(cpu.Flags, PARITY_FLAG)) {
    cpu.PC = cpu.PC + 1
    cpu.PC = cpu.PC + 1
    return
  }

  addr = GetNextPC16()

  cpu.PC = cpu.PC + 1

  HIGH = itob(cpu.PC >> 8)
  LOW = itob(cpu.PC & 255)

  Push(cpu.stack, HIGH)
  Push(cpu.stack, LOW)

  cpu.PC = addr
}

CPI: proc() {
  L = NextPC(cpu)
  Compare(L)
}

CPO: proc() {
  if (1==GetBit(cpu.Flags, PARITY_FLAG)) {
    cpu.PC = cpu.PC + 1
    cpu.PC = cpu.PC + 1
    return
  }

  addr = GetNextPC16()

  cpu.PC = cpu.PC + 1

  HIGH = itob(cpu.PC >> 8)
  LOW = itob(cpu.PC & 255)

  Push(cpu.stack, HIGH)
  Push(cpu.stack, LOW)

  cpu.PC = addr
}

CZ: proc() {
  if (0 == GetBit(cpu.Flags, ZERO_FLAG)) {
    cpu.PC = cpu.PC + 1
    cpu.PC = cpu.PC + 1
    return
  }

  addr = GetNextPC16()

  cpu.PC = cpu.PC + 1

  HIGH = itob(cpu.PC >> 8)
  LOW = itob(cpu.PC & 255)

  Push(cpu.stack, HIGH)
  Push(cpu.stack, LOW)

  cpu.PC = addr
}

DAA: proc() {
  exit "DAA not implemented"
}

DADB: proc() {
  exit "DADB not implemented"
  //int BC = GetBCSigned()
  //int HL = GetHLSigned()

  //int32_t res = BC + HL
//
  //int newHL = res & (255ff)

  //if ((newHL & 255ff0000) > 1)
  //{
    //SetBit(cpu, Flags, CARRY_FLAG, 1)
  //}
  //else
  //{
    //ClearBit(cpu, Flags, CARRY_FLAG)
  //}
//
  //uint8_t high = (newHL >> 8) & 255
  //uint8_t low = newHL & 255
//
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

DCRA: proc() {
  Decrement(cpu.A)
  SetFlagsBasedOn(cpu.A.v)
}

DCRB: proc() {
  Decrement(cpu.B)
  SetFlagsBasedOn(cpu.B.v)
}

DCRC: proc() {
  Decrement(cpu.C)
  SetFlagsBasedOn(cpu.C.v)
}

DCRD: proc() {
  Decrement(cpu.D)
  SetFlagsBasedOn(cpu.D.v)
}

DCRE: proc() {
  Decrement(cpu.E)
  SetFlagsBasedOn(cpu.E.v)
}

DCRH: proc() {
  Decrement(cpu.H)
  SetFlagsBasedOn(cpu.H.v)
}

DCRL: proc() {
  Decrement(cpu.L)
  SetFlagsBasedOn(cpu.L.v)
}

DCRM: proc() {
  M = GetM(cpu)
  M--

  addr = GetHLUnsigned()
  SetMemory(cpu, addr, M)

  SetFlagsBasedOn(M)
}

DCXB: proc() {
  BC = GetBCSigned()
  BC--

  HIGH = (BC >> 8) & 255
  LOW = (BC & 255)

  SetValueI(cpu.B, HIGH)
  SetValueI(cpu.C, LOW)
}

DCXD: proc() {
  DE = GetDESigned()
  DE--

  HIGH = ((DE >> 8) & 255)
  LOW = (DE & 255)

  SetValueI(cpu.D, HIGH)
  SetValueI(cpu.E, LOW)
}

DCXH: proc() {
  HL = GetHLSigned()
  HL--

  HIGH = ((HL >> 8) & 255)
  LOW = (HL & 255)

  SetValueI(cpu.H, HIGH)
  SetValueI(cpu.L, LOW)
}

DCXSP: proc() {
  exit("DCXSP not implemented")
  //cpu->SP->Decrement()
}

DI: proc() {} // INTERRUPTS 

HLT: proc() {
  cpu.running = false
}

INRA: proc() {
  Increment(cpu.A)
  SetFlagsBasedOn(cpu.A.v)
}

INRB: proc() {
  Increment(cpu.B)
  SetFlagsBasedOn(cpu.B.v)
}

INRC: proc() {
  Increment(cpu.C)
  SetFlagsBasedOn(cpu.C.v)
}

INRD: proc() {
  Increment(cpu.D)
  SetFlagsBasedOn(cpu.D.v)
}

INRE: proc() {
  Increment(cpu.E)
  SetFlagsBasedOn(cpu.E.v)
}

INRH: proc() {
  Increment(cpu.H)
  SetFlagsBasedOn(cpu.H.v)
}

INRL: proc() {
  Increment(cpu.L)
  SetFlagsBasedOn(cpu.L.v)
}

INRM: proc() {
  M = GetM(cpu)
  M++

  addr = GetHLUnsigned()
  SetMemory(cpu, addr, M)
  SetFlagsBasedOn(M)
}

INXB: proc() {
  BC = GetBCSigned()
  BC++

  HIGH = (BC >> 8) & 255
  LOW = (BC & 255)

  SetValueI(cpu.B, HIGH)
  SetValueI(cpu.C, LOW)
}

INXD: proc() {
  DE = GetDESigned()
  DE++

  HIGH = (DE >> 8) & 255
  LOW = DE & 255

  SetValueI(cpu.D, HIGH)
  SetValueI(cpu.E, LOW)
}

INXH: proc() {
  HL = GetHLSigned()
  HL++

  HIGH = ((HL >> 8) & 255)
  LOW = (HL & 255)

  SetValueI(cpu.H, HIGH)
  SetValueI(cpu.L, LOW)
}

INXSP: proc() {
  exit("Cannot INXSP")
  //cpu->SP->Increment()
}

JC: proc() {
  if (0 == GetBit(cpu.Flags, CARRY_FLAG)) {
    cpu.PC = cpu.PC + 1
    cpu.PC = cpu.PC + 1
    return
  }

  addr = GetNextPC16()

  cpu.PC = (addr)
}

JM: proc() {
  if (0 == GetBit(cpu.Flags, SIGN_FLAG)) {
    cpu.PC = cpu.PC + 1
    cpu.PC = cpu.PC + 1
    return
  }

  addr = GetNextPC16()

  cpu.PC = (addr)
}

JMP: proc() {
  addr = GetNextPC16()
  cpu.PC = addr
}

JNC: proc() {
  if (1 == GetBit(cpu.Flags, CARRY_FLAG)) {
    cpu.PC = cpu.PC + 1
    cpu.PC = cpu.PC + 1
    return
  }

  addr = GetNextPC16()

  cpu.PC = addr
}

JNZ: proc() {
  if (1 == GetBit(cpu.Flags, ZERO_FLAG)) {
    cpu.PC = cpu.PC + 1
    cpu.PC = cpu.PC + 1
    return
  }

  addr = GetNextPC16()

  cpu.PC = addr
}

JP: proc() {
  if (1==GetBit(cpu.Flags, SIGN_FLAG)) {
    cpu.PC = cpu.PC + 1
    cpu.PC = cpu.PC + 1
    return
  }

  addr = GetNextPC16()

  cpu.PC = addr
}

JPE: proc() {
  if (0 == GetBit(cpu.Flags, PARITY_FLAG)) {
    cpu.PC = cpu.PC + 1
    cpu.PC = cpu.PC + 1
    return
  }

  addr = GetNextPC16()

  cpu.PC = addr
}

JPO: proc() {
  if (1==GetBit(cpu.Flags, PARITY_FLAG)) {
    cpu.PC = cpu.PC + 1
    cpu.PC = cpu.PC + 1
    return
  }

  addr = GetNextPC16()

  cpu.PC = addr
}

JZ: proc() {
  if (0 == GetBit(cpu.Flags, ZERO_FLAG)) {
    cpu.PC = cpu.PC + 1
    cpu.PC = cpu.PC + 1
    return
  }

  addr = GetNextPC16()

  cpu.PC = addr
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

  HIGH = ((val >> 8) & 255)
  LOW = (val & 255)

  SetValueI(cpu.B, HIGH)
  SetValueI(cpu.C, LOW)
}

LXID: proc() {
  val = GetNextPC16()

  HIGH = ((val >> 8) & 255)
  LOW = (val & 255)

  SetValueI(cpu.D, HIGH)
  SetValueI(cpu.E, LOW)
}

LXIH: proc() {
  val = GetNextPC16()

  HIGH = itob((val >> 8) & 255)
  LOW = itob(val & 255)

  SetValue(cpu.H, HIGH)
  SetValue(cpu.L, LOW)
}

LXISP: proc() {
  exit "Cannot LXISP"
  //val = GetNextPC16()

  //cpu->SP->Set(val)
}

MOVAA: proc() {}

MOVAB: proc() {
  r = cpu.A r.v = cpu.B.v
}

MOVAC: proc() {
  r = cpu.A r.v = cpu.C.v
}

MOVAD: proc() {
  r = cpu.A r.v = cpu.D.v
}

MOVAE: proc() {
  r = cpu.A r.v = cpu.E.v
}

MOVAH: proc() {
  r = cpu.A r.v = cpu.H.v
}

MOVAL: proc() {
  r = cpu.A r.v = cpu.L.v
}

MOVAM: proc() {
  r = cpu.A r.v = GetM(cpu)
}

MOVBA: proc() {
  r = cpu.B r.v = cpu.A.v
}

MOVBB: proc() { }

MOVBC: proc() {
  r = cpu.B r.v = cpu.C.v
}

MOVBD: proc() {
  r = cpu.B r.v = cpu.D.v
}

MOVBE: proc() {
  r = cpu.B r.v = cpu.E.v
}

MOVBH: proc() {
  r = cpu.B r.v = cpu.H.v
}

MOVBL: proc() {
  r = cpu.B r.v = cpu.L.v
}

MOVBM: proc() {
  r = cpu.B r.v = GetM(cpu)
}

MOVCA: proc() {
  r = cpu.C r.v = (cpu.A.v)
}

MOVCB: proc() {
  r = cpu.C r.v = (cpu.B.v)
}

MOVCC: proc() { }

MOVCD: proc() {
  r = cpu.C r.v = (cpu.D.v)
}

MOVCE: proc() {
  r = cpu.C r.v = (cpu.E.v)
}

MOVCH: proc() {
  r = cpu.C r.v = (cpu.H.v)
}

MOVCL: proc() {
  r = cpu.C r.v = (cpu.L.v)
}

MOVCM: proc() {
  r = cpu.C r.v = (GetM(cpu))
}

MOVDA: proc() {
  r = cpu.D r.v = (cpu.A.v)
}

MOVDB: proc() {
  r = cpu.D r.v = (cpu.B.v)
}

MOVDC: proc() {
  r = cpu.D r.v = (cpu.C.v)
}

MOVDD: proc() { }

MOVDE: proc() {
  r = cpu.D r.v = (cpu.E.v)
}

MOVDH: proc() {
  r = cpu.D r.v = (cpu.H.v)
}

MOVDL: proc() {
  r = cpu.D r.v = (cpu.L.v)
}

MOVDM: proc() {
  r = cpu.D r.v = (GetM(cpu))
}

MOVEA: proc() {
  r = cpu.E r.v = (cpu.A.v)
}

MOVEB: proc() {
  r = cpu.E r.v = (cpu.B.v)
}

MOVEC: proc() {
  r = cpu.E r.v = (cpu.C.v)
}

MOVED: proc() {
  r = cpu.E r.v = (cpu.D.v)
}

MOVEE: proc() { }

MOVEH: proc() {
  r = cpu.E r.v = (cpu.H.v)
}

MOVEL: proc() {
  r = cpu.E r.v = (cpu.L.v)
}

MOVEM: proc() {
  r = cpu.E r.v = (GetM(cpu))
}

MOVHA: proc() {
  r = cpu.H r.v = (cpu.A.v)
}

MOVHB: proc() {
  r = cpu.H r.v = (cpu.B.v)
}

MOVHC: proc() {
  r = cpu.H r.v = (cpu.C.v)
}

MOVHD: proc() {
  r = cpu.H r.v = (cpu.D.v)
}

MOVHE: proc() {
  r = cpu.H r.v = (cpu.E.v)
}

MOVHH: proc() {
}

MOVHL: proc() {
  r = cpu.H r.v = (cpu.L.v)
}

MOVHM: proc() {
  r = cpu.H r.v = (GetM(cpu))
}

MOVLA: proc() {
  r = cpu.L r.v = (cpu.A.v)
}

MOVLB: proc() {
  r = cpu.L r.v = (cpu.B.v)
}

MOVLC: proc() {
  r = cpu.L r.v = (cpu.C.v)
}

MOVLD: proc() {
  r = cpu.L r.v = (cpu.D.v)
}

MOVLE: proc() {
  r = cpu.L r.v = (cpu.E.v)
}

MOVLH: proc() {
  r = cpu.L r.v = (cpu.H.v)
}

MOVLL: proc() {
}

MOVLM: proc() {
  r = cpu.L r.v = (GetM(cpu))
}

MOVMA: proc() {
  SetMemory(cpu, GetHLUnsigned(), cpu.A.v)
}

MOVMB: proc() {
  SetMemory(cpu, GetHLUnsigned(), cpu.B.v)
}

MOVMC: proc() {
  SetMemory(cpu, GetHLUnsigned(), cpu.C.v)
}

MOVMD: proc() {
  SetMemory(cpu, GetHLUnsigned(), cpu.D.v)
}

MOVME: proc() {
  SetMemory(cpu, GetHLUnsigned(), cpu.E.v)
}

MOVMH: proc() {
  SetMemory(cpu, GetHLUnsigned(), cpu.H.v)
}

MOVML: proc() {
  SetMemory(cpu, GetHLUnsigned(), cpu.L.v)
}

MVIA: proc() {
  val = NextPC(cpu)
  r = cpu.A r.v = (val)
}

MVIB: proc() {
  val = NextPC(cpu)
  r = cpu.B r.v = (val)
}

MVIC: proc() {
  val = NextPC(cpu)
  r = cpu.C r.v = (val)
}

MVID: proc() {
  val = NextPC(cpu)
  r = cpu.D r.v = (val)
}

MVIE: proc() {
  val = NextPC(cpu)
  r = cpu.E r.v = (val)
}

MVIH: proc() {
  val = NextPC(cpu)
  r = cpu.H r.v = (val)
}

MVIL: proc() {
  val = NextPC(cpu)
  r = cpu.L r.v = (val)
}

MVIM: proc() {
  val = NextPC(cpu)
  SetMemory(cpu, GetHLUnsigned(), val)
}

NOP: proc() {}

SetOrFlags: proc(cpu: CPU, result: byte) {
  SetFlags(cpu,
    (result & 0y80) > 0y00,  // sign
    result == 0y00, // zero
    false  // carry
  )
}

ORAA: proc() {
  result = cpu.A.v
  SetOrFlags(cpu, result)
}

ORAB: proc() {
  result = cpu.A.v | cpu.B.v
  SetValue(cpu.A, result)
  SetOrFlags(cpu, result)
}

ORAC: proc() {
  result = cpu.A.v | cpu.C.v
  SetValue(cpu.A, result)
  SetOrFlags(cpu, result)
}

ORAD: proc() {
  result = cpu.A.v | cpu.D.v
  SetValue(cpu.A, result)
  SetOrFlags(cpu, result)
}

ORAE: proc() {
  result = cpu.A.v | cpu.E.v
  SetValue(cpu.A, result)
  SetOrFlags(cpu, result)
}

ORAH: proc() {
  result = cpu.A.v | cpu.H.v
  SetValue(cpu.A, result)
  SetOrFlags(cpu, result)
}

ORAL: proc() {
  result = cpu.A.v | cpu.L.v
  SetValue(cpu.A, result)
  SetOrFlags(cpu, result)
}

ORAM: proc() {
  result = cpu.A.v | GetM(cpu)
  SetValue(cpu.A, result)
  SetOrFlags(cpu, result)
}

ORI: proc() {
  result = cpu.A.v | NextPC(cpu)
  SetValue(cpu.A, result)
  SetOrFlags(cpu, result)
}

PCHL: proc() {
  addr = GetHLUnsigned()
  cpu.PC = (addr - 1)
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

  newCy = (A & 128) > 0

  A = A << 1
  SetValue(cpu.A, itob(A))
  SetBit(cpu.A, 0y00, itob(GetBit(cpu.Flags, CARRY_FLAG)))

  SetBitB(cpu.Flags, CARRY_FLAG, newCy)
}

RAR: proc() {
  A = btoi(cpu.A.v)

  newCy = (A & 1) > 0

  A = A >> 1
  SetValue(cpu.A, itob(A))
  SetBit(cpu.A, 0y07, itob(GetBit(cpu.Flags, CARRY_FLAG)))

  SetBitB(cpu.Flags, CARRY_FLAG, newCy)
}

RC: proc() {
  if (0 == GetBit(cpu.Flags, CARRY_FLAG)) {
    return
  }

  LOW = btoi(Pop(cpu))
  HIGH = btoi(Pop(cpu))

  addr = (HIGH << 8) | LOW

  cpu.PC = (addr - 1)
}

RET: proc() {
  LOW = btoi(Pop(cpu))
  HIGH = btoi(Pop(cpu))

  addr = (HIGH << 8) | LOW

  cpu.PC = (addr - 1)
}

RLC: proc() {
  A = btoi(cpu.A.v)

  newCy = (A & 128) > 0

  A = A << 1
  SetValueI(cpu.A, A)

  SetBitB(cpu.Flags, CARRY_FLAG, newCy)
  SetBitB(cpu.A, 0y00, newCy)
}

// what is this?
DSUB: proc() {
  HL = GetHLSigned()
  BC = GetBCSigned()

  result = HL - BC

  HIGH = itob((result >> 8) & 255)
  LOW = itob(result & 255)

  SetValue(cpu.H, HIGH)
  SetValue(cpu.L, LOW)
}

RM: proc() {
  if (0 == GetBit(cpu.Flags, SIGN_FLAG)) {
    return
  }

  LOW = btoi(Pop(cpu))
  HIGH = btoi(Pop(cpu))

  addr = (HIGH << 8) | LOW

  cpu.PC = (addr - 1)
}

RNC: proc() {
  if (1==GetBit(cpu.Flags, CARRY_FLAG)) {
    return
  }

  LOW = btoi(Pop(cpu))
  HIGH = btoi(Pop(cpu))

  addr = (HIGH << 8) | LOW

  cpu.PC = (addr - 1)
}

RNZ: proc() {
  if (1==GetBit(cpu.Flags, ZERO_FLAG)) {
    return
  }

  LOW = btoi(Pop(cpu))
  HIGH = btoi(Pop(cpu))

  addr = (HIGH << 8) | LOW

  cpu.PC = (addr - 1)
}

RP: proc() {
  if (1==GetBit(cpu.Flags, SIGN_FLAG)) {
    return
  }

  LOW = btoi(Pop(cpu))
  HIGH = btoi(Pop(cpu))

  addr = (HIGH << 8) | LOW

  cpu.PC = (addr - 1)
}

RPE: proc() {
  if (0 == GetBit(cpu.Flags, PARITY_FLAG)) {
    return
  }

  LOW = btoi(Pop(cpu))
  HIGH = btoi(Pop(cpu))

  addr = (HIGH << 8) | LOW

  cpu.PC = (addr - 1)
}

RPO: proc() {
  if (1==GetBit(cpu.Flags, PARITY_FLAG)) {
    return
  }

  LOW = btoi(Pop(cpu))
  HIGH = btoi(Pop(cpu))

  addr = (HIGH << 8) | LOW

  cpu.PC = (addr - 1)
}

RRC: proc() {
  A = cpu.A.v

  newCy = (A & 0y01) > 0y00

  A = A >> 0y01
  SetValue(cpu.A, A)

  SetBitB(cpu.Flags, CARRY_FLAG, newCy)
  SetBitB(cpu.A, 0y07, newCy)
}


RZ: proc() {
  if (0 == GetBit(cpu.Flags, ZERO_FLAG)) {
    return
  }

  LOW = btoi(Pop(cpu))
  HIGH = btoi(Pop(cpu))

  addr = (HIGH << 8) | LOW

  cpu.PC = (addr - 1)
}

// These seem oddly long
SBBA: proc() {
  A = btoi(cpu.A.v)

  Cy = GetBit(cpu.Flags, CARRY_FLAG)

  other = (!(A+Cy)) + 1

  res = A + other

  SetValueI(cpu.A, res)
  SetFlagsBasedOnI(A)
  SetCarry(res)
}

SBBB: proc() {
  A = btoi(cpu.A.v)

  Cy = GetBit(cpu.Flags, CARRY_FLAG)

  other = (!(btoi(cpu.B.v) + Cy)) + 1

  res = A + other

  SetValueI(cpu.A, res)
  SetFlagsBasedOnI(A)
  SetCarry(res)
}

SBBC: proc() {
  A = btoi(cpu.A.v)

  Cy = GetBit(cpu.Flags, CARRY_FLAG)

  other = (!(btoi(cpu.C.v) + Cy)) + 1

  res = A + other

  SetValueI(cpu.A, res)
  SetFlagsBasedOnI(A)
  SetCarry(res)
}

SBBD: proc() {
  A = btoi(cpu.A.v)

  Cy = GetBit(cpu.Flags, CARRY_FLAG)

  other = (!(btoi(cpu.D.v) + Cy)) + 1

  res = A + other

  SetValueI(cpu.A, res)
  SetFlagsBasedOnI(A)
  SetCarry(res)
}

SBBE: proc() {
  A = btoi(cpu.A.v)

  Cy = GetBit(cpu.Flags, CARRY_FLAG)

  other = (!(btoi(cpu.E.v) + Cy)) + 1

  res = A + other

  SetValueI(cpu.A, res)
  SetFlagsBasedOnI(A)
  SetCarry(res)
}

SBBH: proc() {
  A = btoi(cpu.A.v)

  Cy = GetBit(cpu.Flags, CARRY_FLAG)

  other = (!(btoi(cpu.H.v) + Cy)) + 1

  res = A + other

  SetValueI(cpu.A, res)
  SetFlagsBasedOnI(A)
  SetCarry(res)
}

SBBL: proc() {
  A = btoi(cpu.A.v)

  Cy = GetBit(cpu.Flags, CARRY_FLAG)

  other = (!(btoi(cpu.L.v) + Cy)) + 1

  res = A + other

  SetValueI(cpu.A, res)
  SetFlagsBasedOnI(A)
  SetCarry(res)
}

SBBM: proc() {
  A = btoi(cpu.A.v)

  Cy = GetBit(cpu.Flags, CARRY_FLAG)

  other = (!(btoi(GetM(cpu)) + Cy)) + 1

  res = A + other

  SetValueI(cpu.A, res)
  SetFlagsBasedOnI(A)
  SetCarry(res)
}

SBI: proc() {
  A = btoi(cpu.A.v)

  Cy = GetBit(cpu.Flags, CARRY_FLAG)

  other = (!(btoi(NextPC(cpu)) + Cy)) + 1

  res = A + other

  SetValueI(cpu.A, res)
  SetFlagsBasedOnI(A)
  SetCarry(res)
}

SHLD: proc() {
  addr = GetNextPC16()

  SetMemory(cpu, addr, cpu.L.v)
  SetMemory(cpu, addr + 1, cpu.H.v)
}

SPHL: proc() {
  exit "Cannot SPHL"
  //cpu->SP->Set(GetHLUnsigned())
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
  SetBit(cpu.Flags, CARRY_FLAG, 0y01)
}

SetCarry: proc(res: int) {
  SetBitB(cpu.Flags, CARRY_FLAG, (res & 255) != res)
}

SUBA: proc() {
  A = btoi(cpu.A.v)

  other = (!(A)) + 1

  res = A + other

  SetValueI(cpu.A, res)
  SetFlagsBasedOnI(A)
  SetCarry(res)
}

SUBB: proc() {
  A = btoi(cpu.A.v)

  other = (!(btoi(cpu.B.v))) + 1

  res = A + other

  SetValueI(cpu.A, res)
  SetFlagsBasedOnI(A)
  SetCarry(res)
}

SUBC: proc() {
  A = btoi(cpu.A.v)

  other = (!(btoi(cpu.C.v))) + 1

  res = A + other

  SetValueI(cpu.A, res)
  SetFlagsBasedOnI(A)
  SetCarry(res)
}

SUBD: proc() {
  A = btoi(cpu.A.v)

  other = (!(btoi(cpu.D.v))) + 1

  res = A + other

  SetValueI(cpu.A, res)
  SetFlagsBasedOnI(A)
  SetCarry(res)
}

SUBE: proc() {
  A = btoi(cpu.A.v)

  other = (!(btoi(cpu.E.v))) + 1

  res = A + other

  SetValueI(cpu.A, res)
  SetFlagsBasedOnI(A)
  SetCarry(res)
}

SUBH: proc() {
  A = btoi(cpu.A.v)

  other = (!(btoi(cpu.H.v))) + 1

  res = A + other

  SetValueI(cpu.A, res)
  SetFlagsBasedOnI(A)
  SetCarry(res)
}

SUBL: proc() {
  A = btoi(cpu.A.v)

  other = (!(btoi(cpu.L.v))) + 1

  res = A + other

  SetValueI(cpu.A, res)
  SetFlagsBasedOnI(A)
  SetCarry(res)
}

SUBM: proc() {
  A = btoi(cpu.A.v)

  other = (!(btoi(GetM(cpu)))) + 1

  res = A + other

  SetValueI(cpu.A, res)
  SetFlagsBasedOnI(A)
  SetCarry(res)
}

SUI: proc() {
  A = btoi(cpu.A.v)

  other = (!(btoi(NextPC(cpu)))) + 1

  res = A + other

  SetValueI(cpu.A, res)
  SetFlagsBasedOnI(A)
  SetCarry(res)
}

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

XRAA: proc() {
  SetValue(cpu.A, 0y00)
}

XRAB: proc() {
  A = cpu.A.v
  other = cpu.B.v

  r = cpu.A r.v = (A ^ other)
}

XRAC: proc() {
  A = cpu.A.v
  other = cpu.C.v

  r = cpu.A r.v = (A ^ other)
}

XRAD: proc() {
  A = cpu.A.v
  other = cpu.D.v

  r = cpu.A r.v = (A ^ other)
}

XRAE: proc() {
  A = cpu.A.v
  other = cpu.E.v

  r = cpu.A r.v = (A ^ other)
}

XRAH: proc() {
  A = cpu.A.v
  other = cpu.H.v

  r = cpu.A r.v = (A ^ other)
}

XRAL: proc() {
  A = cpu.A.v
  other = cpu.L.v

  r = cpu.A r.v = (A ^ other)
}

XRAM: proc() {
  A = cpu.A.v
  other = GetM(cpu)

  r = cpu.A r.v = (A ^ other)
}

XRI: proc() {
  A = cpu.A.v
  other = NextPC(cpu)

  // bit xor (!)
  r = cpu.A r.v = (A ^ other)
}

XTHL: proc() {
  exit "XTHL not implemented"
  // this can't be right...? throws away the values?
  //Pop(cpu)
  //Pop(cpu)

  //Push(cpu.stack, cpu.H.v)
  //Push(cpu.stack, cpu.L.v)
}

Increment(cpu.A)
Increment(cpu.A)
println cpu.A.v


runOneOp: proc(op: byte) {
  if op == 0y00 { NOP() }
  if op == 0y02 { STAXB() }
  if op == 0y03 { INXB() }
  if op == 0y04 { INRB() }
  if op == 0y05 { DCRB() }
  if op == 0y06 { MVIB() }
  if op == 0y07 { RLC() }
  if op == 0y08 { DSUB() }
  if op == 0y09 { DADB() }
  if op == 0y0a { LDAXB() }
  if op == 0y0b { DCXB() }
  if op == 0y0c { INRC() }
  if op == 0y0d { DCRC() }
  if op == 0y0e { MVIC() }
  if op == 0y0f { RRC() }
  if op == 0y11 { LXID() }
  if op == 0y12 { STAXD() }
  if op == 0y13 { INXD() }
  if op == 0y14 { INRD() }
  if op == 0y15 { DCRD() }
  if op == 0y16 { MVID() }
  if op == 0y17 { RAL() }
  if op == 0y19 { DADD() }
  if op == 0y1a { LDAXD() }
  if op == 0y1b { DCXD() }
  if op == 0y1c { INRE() }
  if op == 0y1d { DCRE() }
  if op == 0y1e { MVIE() }
  if op == 0y1f { RAR() }
  if op == 0y21 { LXIH() }
  if op == 0y22 { SHLD() }
  if op == 0y23 { INXH() }
  if op == 0y24 { INRH() }
  if op == 0y25 { DCRH() }
  if op == 0y26 { MVIH() }
  if op == 0y27 { DAA() }
  if op == 0y29 { DADH() }
  if op == 0y2a { LHLD() }
  if op == 0y2b { DCXH() }
  if op == 0y2c { INRL() }
  if op == 0y2d { DCRL() }
  if op == 0y2e { MVIL() }
  if op == 0y2f { CMA() }
  if op == 0y31 { LXISP() }
  if op == 0y32 { STA() }
  if op == 0y33 { INXSP() }
  if op == 0y34 { INRM() }
  if op == 0y35 { DCRM() }
  if op == 0y36 { MVIM() }
  if op == 0y37 { STC() }
  if op == 0y39 { DADSP() }
  if op == 0y3a { LDA() }
  if op == 0y3b { DCXSP() }
  if op == 0y3c { INRA() }
  if op == 0y3d { DCRA() }
  if op == 0y3e { MVIA() }
  if op == 0y3f { CMC() }
  if op == 0y40 { MOVBB() }
  if op == 0y41 { MOVBC() }
  if op == 0y42 { MOVBD() }
  if op == 0y43 { MOVBE() }
  if op == 0y44 { MOVBH() }
  if op == 0y45 { MOVBL() }
  if op == 0y46 { MOVBM() }
  if op == 0y47 { MOVBA() }
  if op == 0y48 { MOVCB() }
  if op == 0y49 { MOVCC() }
  if op == 0y4a { MOVCD() }
  if op == 0y4b { MOVCE() }
  if op == 0y4c { MOVCH() }
  if op == 0y4d { MOVCL() }
  if op == 0y4e { MOVCM() }
  if op == 0y4f { MOVCA() }
  if op == 0y50 { MOVDB() }
  if op == 0y51 { MOVDC() }
  if op == 0y52 { MOVDD() }
  if op == 0y53 { MOVDE() }
  if op == 0y54 { MOVDH() }
  if op == 0y55 { MOVDL() }
  if op == 0y56 { MOVDM() }
  if op == 0y57 { MOVDA() }
  if op == 0y58 { MOVEB() }
  if op == 0y59 { MOVEC() }
  if op == 0y5a { MOVED() }
  if op == 0y5b { MOVEE() }
  if op == 0y5c { MOVEH() }
  if op == 0y5d { MOVEL() }
  if op == 0y5e { MOVEM() }
  if op == 0y5f { MOVEA() }
  if op == 0y60 { MOVHB() }
  if op == 0y61 { MOVHC() }
  if op == 0y62 { MOVHD() }
  if op == 0y63 { MOVHE() }
  if op == 0y64 { MOVHH() }
  if op == 0y65 { MOVHL() }
  if op == 0y66 { MOVHM() }
  if op == 0y67 { MOVHA() }
  if op == 0y68 { MOVLB() }
  if op == 0y69 { MOVLC() }
  if op == 0y6a { MOVLD() }
  if op == 0y6b { MOVLE() }
  if op == 0y6c { MOVLH() }
  if op == 0y6d { MOVLL() }
  if op == 0y6e { MOVLM() }
  if op == 0y6f { MOVLA() }
  if op == 0y70 { MOVMB() }
  if op == 0y71 { MOVMC() }
  if op == 0y72 { MOVMD() }
  if op == 0y73 { MOVME() }
  if op == 0y74 { MOVMH() }
  if op == 0y75 { MOVML() }
  if op == 0y76 { HLT() }
  if op == 0y77 { MOVMA() }
  if op == 0y78 { MOVAB() }
  if op == 0y79 { MOVAC() }
  if op == 0y7a { MOVAD() }
  if op == 0y7b { MOVAE() }
  if op == 0y7c { MOVAH() }
  if op == 0y7d { MOVAL() }
  if op == 0y7e { MOVAM() }
  if op == 0y7f { MOVAA() }
  if op == 0y80 { ADDB() }
  if op == 0y81 { ADDC() }
  if op == 0y82 { ADDD() }
  if op == 0y83 { ADDE() }
  if op == 0y84 { ADDH() }
  if op == 0y85 { ADDL() }
  if op == 0y86 { ADDM() }
  if op == 0y87 { ADDA() }
  if op == 0y88 { ADCB() }
  if op == 0y89 { ADCC() }
  if op == 0y8a { ADCD() }
  if op == 0y8b { ADCE() }
  if op == 0y8c { ADCH() }
  if op == 0y8d { ADCL() }
  if op == 0y8e { ADCM() }
  if op == 0y8f { ADCA() }
  if op == 0y90 { SUBB() }
  if op == 0y91 { SUBC() }
  if op == 0y92 { SUBD() }
  if op == 0y93 { SUBE() }
  if op == 0y94 { SUBH() }
  if op == 0y95 { SUBL() }
  if op == 0y96 { SUBM() }
  if op == 0y97 { SUBA() }
  if op == 0y98 { SBBB() }
  if op == 0y99 { SBBC() }
  if op == 0y9a { SBBD() }
  if op == 0y9b { SBBE() }
  if op == 0y9c { SBBH() }
  if op == 0y9d { SBBL() }
  if op == 0y9e { SBBM() }
  if op == 0y9f { SBBA() }
  if op == 0ya0 { ANAB() }
  if op == 0ya1 { ANAC() }
  if op == 0ya2 { ANAD() }
  if op == 0ya3 { ANAE() }
  if op == 0ya4 { ANAH() }
  if op == 0ya5 { ANAL() }
  if op == 0ya6 { ANAM() }
  if op == 0ya7 { ANAA() }
  if op == 0ya8 { XRAB() }
  if op == 0ya9 { XRAC() }
  if op == 0yaa { XRAD() }
  if op == 0yab { XRAE() }
  if op == 0yac { XRAH() }
  if op == 0yad { XRAL() }
  if op == 0yae { XRAM() }
  if op == 0yaf { XRAA() }
  if op == 0yb0 { ORAB() }
  if op == 0yb1 { ORAC() }
  if op == 0yb2 { ORAD() }
  if op == 0yb3 { ORAE() }
  if op == 0yb4 { ORAH() }
  if op == 0yb5 { ORAL() }
  if op == 0yb6 { ORAM() }
  if op == 0yb7 { ORAA() }
  if op == 0yb8 { CMPB() }
  if op == 0yb9 { CMPC() }
  if op == 0yba { CMPD() }
  if op == 0ybb { CMPE() }
  if op == 0ybc { CMPH() }
  if op == 0ybd { CMPM() }
  if op == 0ybf { CMPA() }
  if op == 0yc0 { RNZ() }
  if op == 0yc1 { POPB() }
  if op == 0yc2 { JNZ() }
  if op == 0yc3 { JMP() }
  if op == 0yc4 { CNZ() }
  if op == 0yc5 { PUSHB() }
  if op == 0yc6 { ADI() }
  if op == 0yc8 { RZ() }
  if op == 0yc9 { RET() }
  if op == 0yca { JZ() }
  if op == 0ycc { CZ() }
  if op == 0ycd { CALL() }
  if op == 0yce { ACI() }
  if op == 0yd0 { RNC() }
  if op == 0yd1 { POPD() }
  if op == 0yd2 { JNC() }
  if op == 0yd4 { CNC() }
  if op == 0yd5 { PUSHD() }
  if op == 0yd6 { SUI() }
  if op == 0yd8 { RC() }
  if op == 0yda { JC() }
  if op == 0ydc { CC() }
  if op == 0yde { SBI() }
  if op == 0ye0 { RPO() }
  if op == 0ye1 { POPH() }
  if op == 0ye2 { JPO() }
  if op == 0ye3 { XTHL() }
  if op == 0ye4 { CPO() }
  if op == 0ye5 { PUSHH() }
  if op == 0ye6 { ANI() }
  if op == 0ye8 { RPE() }
  if op == 0ye9 { PCHL() }
  if op == 0yea { JPE() }
  if op == 0yeb { XCHG() }
  if op == 0yec { CPE() }
  if op == 0yee { XRI() }
  if op == 0yf0 { RP() }
  if op == 0yf1 { POPPSW() }
  if op == 0yf2 { JP() }
  if op == 0yf4 { CP() }
  if op == 0yf5 { PUSHPSW() }
  if op == 0yf6 { ORI() }
  if op == 0yf8 { RM() }
  if op == 0yf9 { SPHL() }
  if op == 0yfa { JM() }
  if op == 0yfc { CM() }
}

