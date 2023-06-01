///////////////////////////////////////////////////////////////////////////////
//                                    TYPES                                  //
//                           VERSION 2 (COMPILED BY V1)                      //
///////////////////////////////////////////////////////////////////////////////

MAX_RECORDS = 20


// This record declaration has to be before its first use because
// the record finder only registers the NAME not the FIELDS. So the first
// time a field was attempted to be set, it failed. Similarly, it does not
// set the SIZE of the record to malloc...

VarType: record {
  name: string
  // TODO: DELETE THIS
  oldType: int
  size: int

  isArray: bool
  baseType: VarType

  isRecord: bool
  recordType: RecordType

  next: VarType
}

// This really should be a map of name to VarType
types = new VarType

TYPE_UNKNOWN=0
nTYPE_UNKNOWN = types
nTYPE_UNKNOWN.oldType = TYPE_UNKNOWN
nTYPE_UNKNOWN.name = 'unknown'
TYPE_INT=1 nTYPE_INT = makeVarType('int', 8, TYPE_INT)
TYPE_BOOL=2 nTYPE_BOOL = makeVarType('bool', 8, TYPE_BOOL)
TYPE_STRING=3 nTYPE_STRING = makeVarType('string', 8, TYPE_STRING)
TYPE_VOID=4 nTYPE_VOID = makeVarType('void', 0, TYPE_VOID)
TYPE_NULL=5  nTYPE_NULL = makeVarType('null', 8, TYPE_NULL) // what's the difference between null and void? (no pun intended)

TYPE_RECORD_BASE=6
// types 6 to 6+MAX_RECORDS are records.
TYPE_ARRAY_BASE=TYPE_RECORD_BASE + MAX_RECORDS + 1// NOTE THIS IS NOT AN OFFICIAL TYPE
TYPE_INT_ARRAY=TYPE_ARRAY_BASE+TYPE_INT
TYPE_BOOL_ARRAY=TYPE_ARRAY_BASE+TYPE_BOOL
TYPE_STRING_ARRAY=TYPE_ARRAY_BASE+TYPE_STRING

TYPE_NAMES:string[TYPE_STRING_ARRAY + MAX_RECORDS + 1] // should this be more?
TYPE_NAMES[TYPE_UNKNOWN] = "unknown"
TYPE_NAMES[TYPE_INT] = "int"
TYPE_NAMES[TYPE_BOOL] = "bool"
TYPE_NAMES[TYPE_STRING] = "string"
TYPE_NAMES[TYPE_INT_ARRAY] = "int[]"
TYPE_NAMES[TYPE_BOOL_ARRAY] = "bool[]"
TYPE_NAMES[TYPE_STRING_ARRAY] = "string[]"
TYPE_NAMES[TYPE_VOID] = "void"
TYPE_NAMES[TYPE_NULL] = "null"
TYPE_NAMES[TYPE_RECORD_BASE] = "record (of some type)"

TYPE_SIZES:int[TYPE_STRING_ARRAY + MAX_RECORDS + 1]
TYPE_SIZES[TYPE_UNKNOWN] = 0
TYPE_SIZES[TYPE_INT] = 4
TYPE_SIZES[TYPE_BOOL] = 1
TYPE_SIZES[TYPE_STRING] = 8
TYPE_SIZES[TYPE_INT_ARRAY] = 8
TYPE_SIZES[TYPE_BOOL_ARRAY] = 8
TYPE_SIZES[TYPE_STRING_ARRAY] = 8
TYPE_SIZES[TYPE_VOID] = 0
TYPE_SIZES[TYPE_NULL] = 8  // a null is a pointer, so it's 8.


findType: proc(name: string): VarType {
  head = types while head != null do head = head.next {
    if head.name == name {
      return head
    }
  }
  return null
}

findTypeById: proc(id: int): VarType {
  if id >= TYPE_ARRAY_BASE {
    baseType = findTypeById(id - TYPE_ARRAY_BASE)
    // now make an array of base type
    return makeArrayType(baseType)
  }
  head = types while head != null do head = head.next {
    if head.oldType == id {
      return head
    }
  }
  print "Type Id " print id print " not found!"
  exit
}

makeVarType: proc(name: string, size: int, oldType: int): VarType {
  head = types last = head while head != null do head = head.next {
    if head.name == name {
      typeError("Cannot re-define type " + name)
      exit
    }
    last = head
  }
  newType = new VarType
  newType.name = name
  newType.size = size
  newType.oldType = oldType
  last.next = newType
  return newType
}

addVarType: proc(varType: VarType) {
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
  at.oldType = baseType.oldType + TYPE_ARRAY_BASE
  at.size = 8

  // Insert in the list after the base type
  oldNext = baseType.next
  baseType.next = at
  at.next = oldNext
  return at
}

sameTypes: proc(left: VarType, right: VarType): bool {
  if left == right {
    // I-DENTICAL
    return true
  }
  if left.isArray and right.isArray and sameTypes(left.baseType, right.baseType) {
    return true
  }
  // anything else?
  return false
}

fromOldType: proc(oldType: int): VarType {
  head = types while head != null do head = head.next {
    if head.oldType == oldType {
      return head
    }
  }
  typeError("Cannot find old type")
  exit // ha, this is a compilation error
}

checkTypes: proc(leftType: int, rightType: int) {
  if compatibleTypes(leftType, rightType) {
    return
  }
  typeError("left is " + typeName(leftType) + ", but right is " + typeName(rightType))
  exit
}

compatibleTypes: proc(leftType: int, rightType: int): bool {
  if (isRecordType(leftType) and rightType == TYPE_NULL)
    or (isRecordType(rightType) and leftType == TYPE_NULL) {
    return true
  }
  return leftType == rightType
}

isArrayType: proc(type: int): bool {
  return type > TYPE_ARRAY_BASE
}

toBaseType: proc(arrayType: int): int {
  return arrayType - TYPE_ARRAY_BASE
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

sizeByType: proc(type: int): int {
  if type == TYPE_UNKNOWN {
    typeError("Cannot get size of unknown type")
    exit
  }

  // TODO: when all the RAX's are fixed, return TYPE_SIZES[type]
  return 8
}


///////////////////////////////////////
// GLOBALS
///////////////////////////////////////

// TODO: This could be a treeset or hashset
VarSymbol: record {
  name: string
  type: int // OLD type
  varType: VarType
  offset: int
  next: VarSymbol
}

// TODO: when declarations exist, this should be:
// globals:VarSymbol
// globals=null
// and then we can delete numGlobals
globals=new VarSymbol
numGlobals = 0

registerGlobal: proc(name: string, type: int) {
  if type == TYPE_UNKNOWN {
    typeError("Cannot register global '" + name + "' with unknown type")
    exit
  }
  if numGlobals == 0 {
    if debug {
      println "  ; first global " + name
    }
    numGlobals = 1
    // first one!
    globals.type = type
    globals.varType = findTypeById(type)
    globals.name = name
    return
  }
  numGlobals = numGlobals + 1
  head = globals last = head while head != null do head = head.next {
    if head.name == name {
      typeError("Duplicate global: " + name)
      exit
    }
    last = head
  }
  // head is null, the last = old head
  newEntry = new VarSymbol
  last.next = newEntry
  newEntry.name = name
  newEntry.type = type
  if debug {
    print "; Adding global name " + name print "\n"
  }
}

lookupGlobal: proc(name: string): int {
  // this is a little goofy but avoids a NPE in the while
  if numGlobals == 0 {
    return TYPE_UNKNOWN
  }
  head = globals while head != null do head = head.next {
    if head.name == name {
      return head.type
    }
  }
  return TYPE_UNKNOWN
}


///////////////////////////////////////
// PROCS
///////////////////////////////////////

// TODO: This really should be a map from proc name to ProcSymbol
ProcSymbol: record {
  name: string
  returnType: int // TEMPORARY: will be VarType eventually

  // TODO: This really should be a map from name to VarSymbol
  numParams: int
  params: VarSymbol

  // TODO: This really should be a map from name to VarSymbol
  numLocals: int
  locals: VarSymbol

  next: ProcSymbol
}

// TODO: when declarations exist, this should be:
// procs:ProcSymbol
// procs=null
// and then we can delete numProcs
procs=new ProcSymbol
numProcs = 0

LOCALS_PER_PROC = 10

// TODO: when declarations exist, this should be:
// currentProc:ProcSymbol
// currentProc=null
currentProc = procs
currentProc = null

setCurrentProc: proc(name: string) {
  // set the global
  currentProc = lookupProc(name)
}

clearCurrentProc: proc {
  currentProc = null
}

registerProc: proc(name: string): ProcSymbol {
  if numProcs == 0 {
    // this is a little goofy
    numProcs = 1
    procs.name = name
    return procs
  }
  head = procs last = head while head != null do head = head.next {
    if head.name == name {
      typeError("Proc '" + name + "' already declared")
      exit
    }
    last = head
  }
  newEntry = new ProcSymbol
  newEntry.name = name
  last.next = newEntry
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

lookupProcReturnType: proc(name: string): int {
  symbol = lookupProc(name)
  return symbol.returnType
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

// returns the lookup VarSymbol, or null if not found.
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

// Lookup the type of the given global, param or local.
lookupType: proc(name: string): int {
  type = lookupGlobal(name)
  if type != TYPE_UNKNOWN {
    return type
  }
  if currentProc != null {
    sym = lookupParam(name)
    if sym != null {
      return sym.type
    }
    sym = lookupLocal(name)
    if sym != null {
      return sym.type
    }
  }

  typeError("Cannot find variable '" + name + "'")
  exit
}

// Returns the offset of this local.
registerLocal: proc(name: string, type: int): int {
  if type == TYPE_UNKNOWN {
    typeError("Cannot register local '" + name + "' with unknown type")
    exit
  }
  if currentProc == null {
    typeError("Cannot add local '" + name + "' because not in a proc")
    exit
  }
  if currentProc.numLocals == LOCALS_PER_PROC {
    typeError("Cannot have more than 10 locals yet")
    exit
  }

  offset = 0
  head = currentProc.locals last = head while head != null do head = head.next {
    if head.name == name {
      typeError("Cannot register local '" + name + "' because already registered")
      exit
    }
    offset = head.offset
    last = head
  }

  newLocal = new VarSymbol
  newLocal.name = name
  newLocal.type = type
  newLocal.offset = offset - sizeByType(type)
  if last == null {
    currentProc.locals = newLocal
  } else {
    last.next = newLocal
  }
  currentProc.numLocals = currentProc.numLocals + 1

  return newLocal.offset
}

registerParam: proc(name: string, type: int): int {
  if type == TYPE_UNKNOWN {
    typeError("Cannot register param '" + name + "' with unknown type")
    exit
  }

  currentProc.numParams = currentProc.numParams + 1
  offset = 8 // eventually: 8 (return location on stack) + sizeByType(type)
  // add up param offsets
  head = currentProc.params last = head while head != null do head = head.next {
    if head.name == name {
      typeError("Cannot register param '" + name + "' because already registered")
      exit
    }
    offset = head.offset
    last = head
  }
  newParam = new VarSymbol
  newParam.name = name
  newParam.type = type
  newParam.offset = offset + sizeByType(type)
  if last == null {
    currentProc.params = newParam
  } else {
    last.next = newParam
  }

  return offset
}

///////////////////////////////////////
// RECORDS
///////////////////////////////////////

numRecords = 0

FieldSymbol: record {
  name: string
  type: VarType
  offset: int
  next: FieldSymbol
}
RecordType: record {
  name: string  // name of this type, redundant with the VarType field "name"
  oldType: int
  size: int
  fields: FieldSymbol
  next: RecordType
}

// TODO: when declarations exist, this should be:
// records:RecordType
// records=null
records = new RecordType
records.oldType = TYPE_RECORD_BASE - 1
records.name = "__donotuse"

typeName: proc(type: int): string {
  recType = findTypeById(type)
  if recType.isRecord {
    return "Record " + recType.name
  }
  return recType.name
}

registerRecord: proc(name: string): RecordType {
  if numRecords == MAX_RECORDS {
    // this is still needed because of the "id" namespace
    typeError("Max records already defined. Cannot add " + name)
    exit
  }
  numRecords = numRecords + 1
  head = records last = head while head != null do head=head.next {
    if head.name == name {
      typeError("nRecord '" + name + "' already declared")
      exit
    }
    last = head
  }
  newRec = new RecordType
  newRec.name = name
  newRec.oldType = last.oldType + 1
  last.next = newRec

  recVarType = new VarType
  recVarType.isRecord = true
  recVarType.name = name
  recVarType.recordType = newRec
  recVarType.oldType = newRec.oldType
  recVarType.size = 8 // 8 bytes per pointer, not 8 bytes per record!
  addVarType(recVarType)

  return newRec
}

lookupRecord: proc(name: string): RecordType {
  head = records while head != null do head=head.next {
    if head.name == name {
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

// TODO: This must be after numRecords is defined. bug #212
isRecordType: proc(type: int): bool {
  rt = findTypeById(type)
  return rt.isRecord
}
