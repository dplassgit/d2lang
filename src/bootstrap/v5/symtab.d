///////////////////////////////////////////////////////////////////////////////
//                                    TYPES                                  //
///////////////////////////////////////////////////////////////////////////////


// This record declaration has to be before its first use because
// the record finder only registers the NAME not the FIELDS. So the first
// time a field was attempted to be set, it failed. Similarly, it does not
// set the SIZE of the record to malloc...

VarType: record {
  name: string
  size: int // bytes

  isPrimitive: bool
  isIntegral: bool

  isArray: bool
  baseType: VarType

  isRecord: bool
  recordType: RecordType

  opcodeSize: string // BYTE, DWORD, QWORD
  dataSize: string // db, dw, dq

  next: VarType
}

// This really should be a map of name to VarType
types: VarType
types=null

TYPE_UNKNOWN = makeVarType('unknown', 0)
TYPE_INT = makePrimitiveType('int', 4, ' DWORD ', 'dd')
TYPE_INT.isIntegral = true
TYPE_BOOL = makePrimitiveType('bool', 1, ' BYTE ', 'db')
TYPE_STRING = makePrimitiveType('string', 8, ' QWORD ', 'dq')
TYPE_VOID = makePrimitiveType('void', 0, '', '')
TYPE_NULL = makePrimitiveType('null', 8, '', '')
TYPE_BYTE = makePrimitiveType('byte', 1, ' BYTE ', 'db')
TYPE_BYTE.isIntegral = true
TYPE_LONG = makePrimitiveType('long', 8, ' QWORD ', 'dq')
TYPE_LONG.isIntegral = true
TYPE_DOUBLE = makePrimitiveType('double', 8, '', 'dq')


findType: proc(name: string): VarType {
  head = types while head != null do head = head.next {
    headname = head.name
    if head.isPrimitive and stricmp(headname, name) == 0 {
      return head
    } elif head.name == name {
      return head
    }
  }
  return null
}

makeVarType: proc(name: string, size: int): VarType {
  newType = new VarType
  newType.name = name
  newType.size = size
  addVarType(newType)
  return newType
}

makePrimitiveType: proc(name: string, size: int, opcodeSize: string, dataSize: string): VarType {
  vt = makeVarType(name, size)
  vt.isPrimitive = true
  vt.dataSize = dataSize
  vt.opcodeSize = opcodeSize
  return vt
}

addVarType: proc(varType: VarType) {
  if types == null {
    // first one.
    types = varType
    return
  }
  name = varType.name
  head = types last = head while head != null do head = head.next {
    if head.name == name {
      typeError("Cannot re-define type " + name)
      exit
    }
    last = head
  }
  last.next = varType
}

makeArrayType: proc(baseType: VarType): VarType {
  arrayName = baseType.name + "[]"
  existingVarType = findType(arrayName)
  if existingVarType != null {
    return existingVarType
  }

  at = new VarType
  at.name = arrayName
  at.isArray = true
  at.baseType = baseType
  at.size = 8
  at.dataSize = "dq"
  at.opcodeSize = " QWORD "

  // Insert in the list after the base type
  oldNext = baseType.next
  baseType.next = at
  at.next = oldNext
  return at
}

sameTypes: proc(expected: VarType, actual: VarType): bool {
  if expected == actual {
    // I-DENTICAL
    return true
  }
  if expected == null {
    generalError("Internal", "expected is null")
    return false
  }
  if actual == null {
    generalError("Internal", "actual is null")
    return false
  }
  if expected.isArray and actual.isArray {
    // NOTE: cannot write
    // expected.isArray and actual.isArray and sameTypes(...
    // because there is no short-circuit in v3.
    return sameTypes(expected.baseType, actual.baseType)
  }
  // anything else?
  return false
}

checkTypes: proc(expected: VarType, actual: VarType) {
  if compatibleTypes(expected, actual) {
    return
  }
  typeError("Expected " + expected.name + ", but found " + actual.name)
  exit
}

compatibleTypes: proc(expected: VarType, actual: VarType): bool {
  if (expected.isRecord and actual == TYPE_NULL)
    or (actual.isRecord and expected == TYPE_NULL)
    or (expected == TYPE_STRING and actual == TYPE_NULL)
    or (actual == TYPE_STRING and expected == TYPE_NULL) {
    return true
  }
  return sameTypes(expected, actual)
}


///////////////////////////////////////////////////////////////////////////////
//                              SYMBOL TABLES                                //
///////////////////////////////////////////////////////////////////////////////


///////////////////////////////////////
// STRINGS
///////////////////////////////////////

// TODO: this could be a map (string -> int)
stringTable=new StringList

// Create a string constant and return its index. If the string already is in the table,
// will not re-create it.
addStringConstant: proc(s: string): int  {
  node = append(stringTable, s)
  return node.index
}


///////////////////////////////////////
// GLOBALS
///////////////////////////////////////

// TODO: This could be a map from name to VarSymbol
VarSymbol: record {
  name: string
  varType: VarType
  isGlobal: bool
  offset: int
  next: VarSymbol
}

globals:VarSymbol
globals=null

registerGlobal: proc(name: string, varType: VarType): VarSymbol {
  if varType == TYPE_UNKNOWN {
    // TODO: improve this message
    typeError("Cannot register global '" + name + "' with unknown type")
    exit
  }

  head = globals last = head while head != null do head = head.next {
    if head.name == name {
      // TODO: improve this message:
      // Type error at line 2: Variable 'a' already declared as INT, cannot be redeclared as BOOL
      typeError("Duplicate global: " + name)
      exit
    }
    last = head
  }

  newEntry = new VarSymbol
  newEntry.name = name
  newEntry.varType = varType
  newEntry.isGlobal = true

  if last == null {
    // first one
    globals = newEntry
  } else {
    // last = old head
    last.next = newEntry
  }
  if debug {
    println "; Adding global" + name
  }

  return newEntry
}

lookupGlobal: proc(name: string): VarSymbol {
  head = globals while head != null do head = head.next {
    if head.name == name {
      return head
    }
  }
  return null
}

// Returns a string that can be used to reference the variable
// e.g., [_global], [RBP-4] for a local, [RBP+8] for a param
toReference: proc(symbol : VarSymbol): string {
  if symbol.isGlobal {
    return "[_" + symbol.name + "]"
  }
  offset = symbol.offset
  if offset == 0 {
    return "[RBP]"
  } elif offset < 0 {
    return "[RBP - " + toString(-offset) + "]"
  } else {
    return "[RBP + " + toString(offset) + "]"
  }
}


///////////////////////////////////////
// PROCS
///////////////////////////////////////

// TODO: This really should be a map from proc name to ProcSymbol
ProcSymbol: record {
  name: string
  returnType: VarType

  numParams: int   // for type checking
  params: VarSymbol

  // TODO: This really should be a map from name to VarSymbol
  locals: VarSymbol

  isExtern: bool

  next: ProcSymbol
}

procs:ProcSymbol
procs=null

currentProc:ProcSymbol
currentProc=null

setCurrentProc: proc(name: string) {
  currentProc = lookupProc(name)
}

clearCurrentProc: proc {
  currentProc = null
}

registerProc: proc(name: string): ProcSymbol {
  head = procs last = head while head != null do head = head.next {
    if head.name == name {
      typeError("Proc '" + name + "' already declared")
      exit
    }
    last = head
  }

  newEntry = new ProcSymbol
  newEntry.name = name

  if last == null {
    // first one
    procs = newEntry
  } else {
    last.next = newEntry
  }
  return newEntry
}

lookupProc: proc(name: string): ProcSymbol {
  head=procs while head != null do head=head.next {
    if head.name == name {
      return head
    }
  }
  typeError("Cannot find proc '" + name + "'")
  exit
}

// returns the param VarSymbol, or null if not found.
lookupParam: proc(name: string): VarSymbol {
  if currentProc == null {
    typeError("Cannot lookup parameter '" + name + "' because not in a proc")
    exit
  }

  head = currentProc.params while head != null do head=head.next {
    if head.name == name {
      return head
    }
  }
  return null
}

// returns the local VarSymbol, or null if not found.
lookupLocal: proc(name: string): VarSymbol {
  if currentProc == null {
    typeError("Cannot lookup local '" + name + "' because not in a proc")
    exit
  }

  head = currentProc.locals while head != null do head=head.next {
    if head.name == name {
      return head
    }
  }
  return null
}

// add up the locals sizes instead of using the last offset.
localsSize: proc(p: ProcSymbol): int {
  offset = 0
  head = p.locals while head != null do head=head.next {
    offset = offset + head.varType.size
  }
  return offset
}

// Lookup the type of the given global, param or local.
lookupType: proc(name: string): VarType {
  sym = lookupGlobal(name)
  if sym == null {
    // not a global, try to find as param or local
    if currentProc != null {
      sym = lookupParam(name)
      if sym == null {
        sym = lookupLocal(name)
      }
    }
  }
  if sym != null {
    return sym.varType
  }
  return TYPE_UNKNOWN
}

// Returns the symbol of this local.
registerLocal: proc(name: string, varType: VarType): VarSymbol {
  if varType == TYPE_UNKNOWN {
    typeError("Cannot register local '" + name + "' with unknown type")
    exit
  }
  if currentProc == null {
    typeError("Cannot add local '" + name + "' because not in a proc")
    exit
  }

  offset = 0
  head = currentProc.locals last = head while head != null do head = head.next {
    if head.name == name {
      // TODO: improve this message:
      // Type error at line 2: Variable 'a' already declared as INT, cannot be redeclared as BOOL
      typeError("Duplicate local '" + name + "' in proc '" + currentProc.name + "'")
      exit
    }
    offset = head.offset
    last = head
  }

  newLocal = new VarSymbol
  newLocal.name = name
  newLocal.varType = varType
  newLocal.offset = offset - varType.size
  if last == null {
    currentProc.locals = newLocal
  } else {
    last.next = newLocal
  }

  return newLocal
}

registerParam: proc(name: string, varType: VarType) {
  if varType == TYPE_UNKNOWN {
    typeError("Cannot register param '" + name + "' with unknown type")
    exit
  }

  currentProc.numParams = currentProc.numParams + 1
  offset = 8
  // add up param offsets
  head = currentProc.params last = head while head != null do head = head.next {
    if head.name == name {
      typeError("Duplicate parameter '" + name + "' to proc '" + currentProc.name + "'")
      exit
    }
    offset = head.offset
    last = head
  }

  newParam = new VarSymbol
  newParam.name = name
  newParam.varType = varType
  newParam.offset = offset + 8 // always pushes 8 bytes. varType.size
  if last == null {
    currentProc.params = newParam
  } else {
    last.next = newParam
  }
}

lookupVariable: proc(variable: string): VarSymbol {
  symbol = lookupGlobal(variable)
  if symbol != null {
    return symbol
  }

  if currentProc == null {
    // Not in a proc, cannot look up local.
    typeError("Unknown global '" + variable + "'")
    exit
  }

  symbol = lookupLocal(variable)
  if symbol != null {
    return symbol
  }

  symbol = lookupParam(variable)
  if symbol != null {
    return symbol
  }
  typeError("Unknown local or param '" + variable + "'")
  exit
}


///////////////////////////////////////
// RECORDS
///////////////////////////////////////


FieldSymbol: record {
  name: string
  // TODO: rename this "varType"
  type: VarType
  offset: int
  next: FieldSymbol
}

RecordType: record {
  name: string  // name of this type, redundant with the VarType field "name"
  size: int
  fields: FieldSymbol
  next: RecordType
}

records:RecordType
records=null

registerRecord: proc(name: string): RecordType {
  head = records last = head while head != null do head=head.next {
    if head.name == name {
      typeError("Record '" + name + "' already declared")
      exit
    }
    last = head
  }

  newRec = new RecordType
  newRec.name = name

  if last == null {
    // first one
    records = last
  } else {
    last.next = newRec
  }

  recVarType = new VarType
  recVarType.isRecord = true
  recVarType.name = name
  recVarType.recordType = newRec
  recVarType.size = 8 // 8 bytes per pointer, not 8 bytes per record!
  recVarType.dataSize = "dq"
  recVarType.opcodeSize = " QWORD "
  addVarType(recVarType)

  return newRec
}

lookupRecord: proc(name: string): VarType {
  head = types while head != null do head=head.next {
    if head.isRecord and head.name == name {
      return head
    }
  }
  return null
}

lookupField: proc(rt: RecordType, name: string): FieldSymbol {
  head = rt.fields while head != null do head=head.next {
    if head.name == name {
      return head
    }
  }
  return null
}

registerField: proc(rt: RecordType, name: string, varType: VarType): FieldSymbol {
  if varType == null {
    generalError("Internal error", "Vartype is null for record type " +rt.name + " field " + name)
    exit
  }

  offset = 0 head = rt.fields last = head while head != null do head=head.next {
    if head.name == name {
      typeError("Duplicate field " + name + " in record " + rt.name)
      exit
    }
    if head.type != null {
      offset = offset + head.type.size
    }
    last = head
  }

  newField = new FieldSymbol
  newField.name = name
  newField.type = varType
  newField.offset = offset

  if last == null {
    // first one
    rt.fields = newField
  } else {
    last.next = newField
  }
  if varType == null {
    generalError("Internal", "Vartype is null, cannot update size of " + rt.name)
    exit
  } else {
    rt.size = rt.size + varType.size
  }
  return newField
}

