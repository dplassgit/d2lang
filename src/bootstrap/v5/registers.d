A_REG=0
B_REG=1
C_REG=2
D_REG=3
XMM0=0
XMM1=1
XMM2=2
XMM3=3

DOUBLE_REGISTERS=[
  'XMM0',
  'XMM1',
  'XMM2',
  'XMM3'
]

REGISTERS=[
  'AL',
  'BL',
  'CL',
  'DL',
  'AX',
  'BX',
  'CX',
  'DX',
  'EAX',
  'EBX',
  'ECX',
  'EDX',
  'RAX',
  'RBX',
  'RCX',
  'RDX'
]

makeRegister: proc(base: int, varType: VarType): string {
  if varType == TYPE_DOUBLE {
    return DOUBLE_REGISTERS[base]
  }
  index = base
  bytes = varType.size
  if bytes == 2 { index = index + 4}
  elif bytes == 4 { index = index + 8}
  elif bytes == 8 { index = index + 12}
  return REGISTERS[index]
}

push: proc(register: int, type: VarType, emitter: Emitter) {
  if type != TYPE_DOUBLE {
    emitter_emit(emitter, "push " + REGISTERS[register + 12]) // makeRegister(register, TYPE_LONG))
  } else {
    emitter_emit(emitter, "sub RSP, 8")
    emitter_emit(emitter, "movq [RSP], " + makeRegister(register, TYPE_DOUBLE))
  }
  
}

pop: proc(register: int, type: VarType, emitter: Emitter) {
  if type != TYPE_DOUBLE {
    emitter_emit(emitter, "pop " + REGISTERS[register + 12]) // makeRegister(register, TYPE_LONG))
  } else {
    emitter_emit(emitter, "movq " + makeRegister(register, TYPE_DOUBLE) + ", [RSP]")
    emitter_emit(emitter, "add RSP, 8")
  }
}
