A_REG=0
B_REG=1
C_REG=2
D_REG=3

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
  index = base
  bytes = varType.size
  if bytes == 2 { index = index + 4}
  elif bytes == 4 { index = index + 8}
  elif bytes == 8 { index = index + 12}
  return REGISTERS[index]
}
