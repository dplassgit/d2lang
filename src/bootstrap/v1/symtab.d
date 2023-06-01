///////////////////////////////////////////////////////////////////////////////
//                                    TYPES                                  //
//                           VERSION 1 (COMPILED BY V0)                      //
///////////////////////////////////////////////////////////////////////////////

MAX_RECORDS = 20

TYPE_UNKNOWN=0
TYPE_INT=1
TYPE_BOOL=2
TYPE_STRING=3
TYPE_VOID=4
TYPE_NULL=5 // what's the difference between null and void? (no pun intended)
TYPE_RECORD_BASE=6
// types 10 to 10+MAX_RECORDS are records.
TYPE_ARRAY=TYPE_RECORD_BASE + MAX_RECORDS + 1// NOTE THIS IS NOT AN OFFICIAL TYPE
TYPE_INT_ARRAY=TYPE_ARRAY+TYPE_INT
TYPE_BOOL_ARRAY=TYPE_ARRAY+TYPE_BOOL
TYPE_STRING_ARRAY=TYPE_ARRAY+TYPE_STRING

TYPE_NAMES:string[TYPE_STRING_ARRAY + 1] // should this be more?
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

TYPE_SIZES:int[11]
TYPE_SIZES[TYPE_UNKNOWN] = 0
TYPE_SIZES[TYPE_INT] = 4
TYPE_SIZES[TYPE_BOOL] = 1
TYPE_SIZES[TYPE_STRING] = 8
TYPE_SIZES[TYPE_INT_ARRAY] = 8
TYPE_SIZES[TYPE_BOOL_ARRAY] = 8
TYPE_SIZES[TYPE_STRING_ARRAY] = 8
TYPE_SIZES[TYPE_VOID] = 0
TYPE_SIZES[TYPE_NULL] = 8  // a null is a pointer, so it's 8.


///////////////////////////////////////////////////////////////////////////////
//                              SYMBOL TABLES                                //
///////////////////////////////////////////////////////////////////////////////

numStrings = 0
MAX_STRINGS=500
stringTable: string[MAX_STRINGS]  // v0.d has 311 strings

// Create a string constant and return its index. If the string already is in the table,
// will not re-create it.
addStringConstant: proc(s: string): int  {
  i = 0 while i < numStrings do i = i + 1 {
    if stringTable[i] == s {
      return i
    }
  }
  stringTable[numStrings] = s
  numStrings = numStrings + 1
  if numStrings == MAX_STRINGS - 1 {
    typeError("Too many strings")
    exit
  }
  return numStrings - 1
}

sizeByType: proc(type: int): int {
  if type == TYPE_UNKNOWN {
    typeError("Cannot get size of unknown type")
    exit
  }

  // TODO: when all the RAX's are fixed, return TYPE_SIZES[type]
  return 8
}

numGlobals = 0
MAX_GLOBALS=200 // currently v0.d has > 100 globals
globalNames: string[MAX_GLOBALS]
globalTypes: int[MAX_GLOBALS]

registerGlobal: proc(name: string, type: int) {
  if type == TYPE_UNKNOWN {
    typeError("Cannot register global '" + name + "' with unknown type")
    exit
  }
  i = 0 while i < numGlobals do i = i + 1 {
    if globalNames[i] == name {
      return
    }
  }
  if debug {
    print "; Adding global name " + name print "\n"
  }
  if numGlobals == MAX_GLOBALS - 1 {
    typeError("Too many globals")
    exit
  }
  globalNames[numGlobals] = name
  globalTypes[numGlobals] = type
  numGlobals = numGlobals + 1
}

lookupGlobal: proc(name: string): int {
  i = 0 while i < numGlobals do i = i + 1 {
    if globalNames[i] == name {
      return globalTypes[i]
    }
  }
  return TYPE_UNKNOWN
}


MAX_PROCS = 200 // currently v0.d has ~60 procs
numProcs = 0
procNames: string[MAX_PROCS]
returnTypes: int[MAX_PROCS]

numParams: int[MAX_PROCS]
PARAMS_PER_PROC = 4
// These are sparse arrays; the start index for the 0th param of each proc is 4 * proc num
paramNames: string[MAX_PROCS * PARAMS_PER_PROC]
paramTypes: int[MAX_PROCS * PARAMS_PER_PROC]
paramOffsets: int[MAX_PROCS * PARAMS_PER_PROC]

numLocals: int[MAX_PROCS]
LOCALS_PER_PROC = 10
// These are sparse arrays; the start index for the 0th local of each proc is 10 * proc num
localNames: string[MAX_PROCS * LOCALS_PER_PROC]
localTypes: int[MAX_PROCS * LOCALS_PER_PROC]
localOffsets: int[MAX_PROCS * LOCALS_PER_PROC]

currentProcNum = -1

registerProc: proc(name: string, returnType: int) {
  if returnType == TYPE_UNKNOWN {
    typeError("Cannot have unknown proc return type")
    exit
  }
  i = 0 while i < numProcs do i = i + 1 {
    // Make sure it doesn't exist yet
    if procNames[i] == name {
      typeError("Procedure '" + name + "' already declared")
      exit
    }
  }
  if numProcs == MAX_PROCS - 1 {
    typeError("Too many procs")
    exit
  }
  procNames[numProcs] = name
  returnTypes[numProcs] = returnType
  numProcs = numProcs + 1
}

setCurrentProcNum: proc(name: string) {
  i = 0 while i < numProcs do i = i + 1 {
    if procNames[i] == name {
      currentProcNum = i
      return
    }
  }
  parserError("Cannot set current proc num for proc '" + name)
  exit
}

lookupProcReturnType: proc(name: string): int {
  i = 0 while i < numProcs do i = i + 1 {
    if name == procNames[i] {
      return returnTypes[i]
    }
  }
  parserError("Cannot find proc '" + name)
  exit
}

// returns the index of the param in the arrays
lookupParam: proc(name: string): int {
  if currentProcNum == -1 {
    typeError("Cannot lookup parameter " + name + " because not in a proc")
    exit
  }

  base = currentProcNum * PARAMS_PER_PROC
  i = 0 while i < numParams[currentProcNum] do i = i + 1 {
    if paramNames[base] == name {
      return base
    }
    base = base + 1
  }
  return -1
}

// returns the index of the local in the arrays
lookupLocal: proc(name: string): int {
  if currentProcNum == -1 {
    typeError("Cannot lookup local " + name + " because not in a proc")
    exit
  }
  base = currentProcNum * 10
  i = 0 while i < numLocals[currentProcNum] do i = i + 1 {
    if localNames[base] == name {
      return base
    }
    base = base + 1
  }

  return -1
}

// Lookup the type of the given global, param or local.
lookupType: proc(name: string): int {
  type = lookupGlobal(name)
  if type != TYPE_UNKNOWN {
    return type
  }
  if currentProcNum != -1 {
    index = lookupParam(name)
    if index != -1 {
      return paramTypes[index]
    }
    index = lookupLocal(name)
    if index != -1 {
      return localTypes[index]
    }
  }

  typeError("Cannot find variable " + name)
  exit
}

// Returns the offset of this local.
registerLocal: proc(name: string, type: int): int {
  if type == TYPE_UNKNOWN {
    typeError("Cannot register local '" + name + "' with unknown type")
    exit
  }
  myLocalCount = numLocals[currentProcNum]
  if myLocalCount == LOCALS_PER_PROC {
    typeError("Too many locals. Max is 10")
    exit
  }

  // These are sparse arrays; the start index for the 0th local of each proc is 10 * proc num
  base = currentProcNum * LOCALS_PER_PROC + myLocalCount

  offset = -sizeByType(type)
  if myLocalCount > 0 {
    // add our offset to previous offset
    offset = offset + localOffsets[base - 1]
  }
  localOffsets[base] = offset
  localNames[base] = name
  localTypes[base] = type
  numLocals[currentProcNum] = myLocalCount + 1
  return offset
}

isArrayType: proc(type: int): bool {
  return type > TYPE_ARRAY
}

toBaseType: proc(arrayType: int): int {
  return arrayType - TYPE_ARRAY
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

numRecords = 0
recordNames: string[MAX_RECORDS]
numFields: int[MAX_RECORDS]
recordSizes: int[MAX_RECORDS]
FIELDS_PER_RECORD = 20
// These are sparse arrays; the start index for the 0th field of each record is 20 * record num
fieldNames: string[MAX_RECORDS * FIELDS_PER_RECORD]
fieldTypes: int[MAX_RECORDS * FIELDS_PER_RECORD]
fieldOffsets: int[MAX_RECORDS * FIELDS_PER_RECORD]

typeName: proc(type: int): string {
  if isRecordType(type) {
    return "Record " + recordNames[type - TYPE_RECORD_BASE]
  }
  return TYPE_NAMES[type]
}


// Returns the index of the record
registerRecord: proc(name: string): int {
  if numRecords == MAX_RECORDS {
    typeError("Max records already defined. Cannot add " + name)
    exit
  }
  i = 0 while i < numRecords do i = i + 1 {
    // Make sure it doesn't exist yet
    if recordNames[i] == name {
      typeError("Record '" + name + "' already declared")
      exit
    }
  }
  recordNames[numRecords] = name
  if debug {
    print "; registered " print name print " as index " print numRecords print "\n"
  }
  numRecords = numRecords + 1
  return numRecords - 1
}

lookupRecord: proc(name: string): int {
  i = 0 while i < numRecords do i = i + 1 {
    // Make sure it doesn't exist yet
    if recordNames[i] == name {
      return i
    }
  }
  return -1
}

lookupField: proc(recordIndex: int, fieldName: string): int {
  i = recordIndex * FIELDS_PER_RECORD j=0 while j < numFields[recordIndex] do j = j + 1 {
    if fieldNames[i] == fieldName {
      return i
    }
    i =  i + 1
  }
  return -1
}

isRecordType: proc(type: int): bool {
  return type >= TYPE_RECORD_BASE and type < TYPE_RECORD_BASE + numRecords
}
