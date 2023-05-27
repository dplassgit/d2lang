///////////////////////////////////////////////////////////////////////////////
//                                    PARSER                                 //
//                           VERSION 1 (COMPILED BY V0)                      //
///////////////////////////////////////////////////////////////////////////////

advanceParser: proc() {
  nextToken()
  if debug {
    //print "; new token is " + lexTokenString print "\n"
  }
}

expectToken: proc(expectedTokenType: int, tokenStr: string) {
  if lexTokenType != expectedTokenType  {
    parserError("Unexpected '" + lexTokenString + "'; expected '" + tokenStr + "'")
    exit
  }
  advanceParser() // eat the expected token
}

expectKeyword: proc(expectedKwType: int) {
  if lexTokenType != TOKEN_KEYWORD or lexTokenKw != expectedKwType {
    parserError("Unexpected '" + lexTokenString + "'; expected '" + KEYWORDS[expectedKwType] + "'")
    exit
  }
  advanceParser() // eat the keyword
}


///////////////////////////////////////////////////////////////////////////////
//                                CODEGEN UTILS                              //
///////////////////////////////////////////////////////////////////////////////

labelId=0
nextLabel: proc(prefix:string) : string {
  labelId = labelId + 1
  return "__" + prefix + toString(labelId)
}

emitLabel: proc(label: string) {
  print "\n" print label print ":\n"
}

// Map from token to binary opcode
OPCODES:string[14]
// OPCODES[0]=";nop"
OPCODES[TOKEN_PLUS]="add"
OPCODES[TOKEN_MINUS]="sub"
OPCODES[TOKEN_MULT]="imul"
OPCODES[TOKEN_AND]="and"
OPCODES[TOKEN_OR]="or"
// OPCODES[0]="; div"
// OPCODES[0]="; mod"
OPCODES[TOKEN_EQEQ]="setz"
OPCODES[TOKEN_NEQ]="setnz"
OPCODES[TOKEN_LT]="setl"
OPCODES[TOKEN_GT]="setg"
OPCODES[TOKEN_LEQ]="setle"
OPCODES[TOKEN_GEQ]="setge"


emitExtern: proc(name: string) {
  // TODO: don't emit if we already emitted this extern
  print "  extern " + name + "\n"
  print "  sub RSP, 0x20\n"
  print "  call " + name + "\n"
  print "  add RSP, 0x20\n\n"
}

// TODO: make this a procedure?
generateNpeTest: proc() {
  oklabel = nextLabel("not_null")
  print "  cmp RAX, 0\n"
  print "  jne " print oklabel print "\n"

  npeMessageIndex = addStringConstant("Null pointer error at line %d.\n")
  print "  mov RCX, CONST_" print npeMessageIndex print "\n"
  print "  mov RDX, " print lexCurrentLine print "\n"
  emitExtern("printf")
  print "  mov RCX, -1\n"
  emitExtern("exit")

  emitLabel(oklabel)
}

///////////////////////////////////////////////////////////////////////////////
//                              EXPRESSION RULES                             //
///////////////////////////////////////////////////////////////////////////////

// expr
//   leftType = boolAnd
//   while token == or {
//      right = boolAnd
//      generate code for left (op) right
//   }
//   return leftType
// boolAnd -> compare (op) compare
// compare -> addSub (op) addSub
// addSub -> muldiv (op) mulDiv
// mulDiv -> unary (op) unary
// unary -> {
//    if minus { eat token; e = parseExpression; generate -e }
//    if plus { eat token; return parseexpressoin}
//    else return composite
// composite -> atom | atom [ int ] | atom . field
// atom -> int constant, bool constant, string constant, variable, '(' expr ')', 'input'


// Each of these returns the type of the expression: TYPE_INT, TYPE_BOOL, TYPE_STRING, etc.
expr: proc(): int {
  return boolOr()
}

boolOr: proc(): int {
  leftType = boolAnd()
  // TODO: int/TOKEN_OR
  if lexTokenType == TOKEN_KEYWORD and leftType == TYPE_BOOL {
    while lexTokenKw == KW_OR {
      advanceParser() // eat the symbol
      print "  push RAX\n"

      rightType = boolAnd()
      checkTypes(leftType, rightType)

      print "  pop RBX\n" // pop the left side
      // left = left (bool) OR right
      print "  or BL, AL\n"
      print "  mov AL, BL\n"
    }
  }
  return leftType
}

boolAnd: proc(): int {
  leftType = compare()
  // TODO: int/TOKEN_AND
  if lexTokenType == TOKEN_KEYWORD and leftType == TYPE_BOOL {
    while lexTokenKw == KW_AND {
      advanceParser() // eat the symbol
      print "  push RAX\n"

      rightType = compare()
      checkTypes(leftType, rightType)

      print "  pop RBX\n" // pop the left side
      // left = left and right
      print "  and BL, AL\n"
      print "  mov AL, BL\n"
    }
  }
  return leftType
}

compare: proc(): int {
  leftType = addSub()
  if leftType == TYPE_INT and (lexTokenType >= TOKEN_EQEQ and lexTokenType <= TOKEN_GEQ) {
    op = lexTokenType
    opstring = lexTokenString
    advanceParser() // eat the symbol
    print "  push RAX\n"

    rightType = addSub()
    checkTypes(leftType, rightType)

    print "  pop RBX\n" // pop the left side

    // left = left (op) right
    // TODO: This is too big for ints, should just use EBX, EAX
    print "  cmp RBX, RAX  ; int " print opstring print " int\n"
    print "  " print OPCODES[op] print " AL\n"
    return TYPE_BOOL
  } elif leftType == TYPE_STRING and (lexTokenType == TOKEN_EQEQ or lexTokenType == TOKEN_NEQ) {
    op = lexTokenType
    advanceParser() // eat the symbol
    print "  push RAX\n"

    rightType = addSub()
    checkTypes(leftType, rightType)

    print "  mov RDX, RAX  ; right side\n"
    print "  pop RCX  ; left side\n"
    emitExtern("strcmp")
    print "  cmp RAX, 0\n"
    print "  " print OPCODES[op] print " AL\n"
    return TYPE_BOOL
  } elif leftType == TYPE_BOOL and (lexTokenType >= TOKEN_EQEQ and lexTokenType <= TOKEN_GEQ) {
    op = lexTokenType
    opstring = lexTokenString
    advanceParser() // eat the symbol
    print "  push RAX\n"

    rightType = addSub()
    checkTypes(leftType, rightType)

    print "  pop RBX\n" // pop the left side
    print "  cmp BL, AL  ; bool " print opstring print " bool\n"
    print "  " print OPCODES[op] print " AL\n"
    return TYPE_BOOL
  } elif (isRecordType(leftType) or leftType == TYPE_NULL) and
         (lexTokenType == TOKEN_EQEQ or lexTokenType == TOKEN_NEQ) {
    op = lexTokenType
    opstring = lexTokenString
    advanceParser() // eat the symbol
    print "  push RAX\n"

    rightType = addSub()
    checkTypes(leftType, rightType)

    print "  pop RBX\n" // pop the left side
    // left = left (op) right
    print "  cmp RBX, RAX  ; record " print opstring print " record\n"
    print "  " print OPCODES[op] print " AL\n"
    return TYPE_BOOL
  }
  return leftType
}

addSub: proc(): int {
  leftType = mulDiv()
  while lexTokenType == TOKEN_PLUS or lexTokenType == TOKEN_MINUS {
    op = lexTokenType
    opstring = lexTokenString
    advanceParser() // eat the symbol
    print "  push RAX\n"

    rightType = mulDiv()
    checkTypes(leftType, rightType)

    print "  pop RBX\n" // pop the left side
    if leftType == TYPE_BOOL {
      typeError("Cannot add or subtract booleans")
      exit
    }
    if leftType == TYPE_STRING {
      // 1. mov rsi, rax. get length of rsi
      print "  mov RSI, RAX  ; lhs in RSI\n"
      print "  mov RCX, RSI\n"
      emitExtern("strlen")
      print "  mov RDI, RAX  ; RHS length in RDI\n"
      // 2. get length of rbx
      print "  mov RCX, RBX\n"
      emitExtern("strlen")
      print "  mov RCX, RAX  ; LHS length in RCX\n"
      // 3. add them
      print "  add RCX, RDI  ; total length in RCX\n"
      print "  inc RCX       ; plus one byte for null\n"
      // 4. allocate new string of new size, result in rax
      print "   ; new string location in RAX\n"
      emitExtern("malloc")
      // 5. strcpy rbx to new location
      print "  push RAX      ; save new location for later\n"
      print "  mov RCX, RAX  ; dest (new location)\n"
      print "  mov RDX, RBX  ; source\n"
      print "  ; copy LHS to new location\n"
      emitExtern("strcpy")
      // 6. strcat rsi to rax
      print "  pop RCX       ; get new location back into RCX as dest\n"
      print "  mov RDX, RSI  ; source\n"
      print "  ; concatenate RHS at new location\n"
      emitExtern("strcat")
      print "\n"
      continue
    }
    if leftType == TYPE_STRING and lexTokenType == TOKEN_MINUS {
      typeError("Cannot subtract strings")
      exit
    }

    if leftType == TYPE_INT {
      // TODO: This is too big for ints, should just use EBX, EAX
      // left = left (op) right
      // TODO: If plus, can just do add rax, rbx instead of two lines
      print "  " print OPCODES[op] print " RBX, RAX  ; int " print opstring print " int\n"
    }
    print "  mov RAX, RBX\n"
  }
  return leftType
}

mulDiv: proc(): int {
  leftType = unary()
  while leftType == TYPE_INT and
      (lexTokenType == TOKEN_MULT or lexTokenType == TOKEN_DIV or lexTokenType == TOKEN_MOD) {
    op = lexTokenType
    advanceParser() // eat the symbol
    print "  push RAX\n"

    rightType = unary()
    checkTypes(leftType, rightType)

    print "  pop RBX\n" // pop the left side

    // TODO: This is too big for ints, should just use EAX & EBX
    // rax = rax (op) rbx
    if op == TOKEN_DIV or op == TOKEN_MOD {
      print "  xchg RAX, RBX  ; put numerator in RAX, denominator in EBX\n"
      // TODO: THIS IS TOO BIG FOR INTS
      print "  cqo  ; sign extend rax to rdx\n"
      print "  idiv RBX  ; rax=rax/rbx\n"
      if op == TOKEN_MOD {
        // modulo is in rdx
        print "  mov RAX, RDX  ; modulo is in rdx\n"
      }
    } else {
      print "  imul RAX, RBX  ; int * int\n"
    }
  }
  return leftType
}

// -unary | +unary | length(expr) | asc(expr) | chr(expr) | not expr
unary: proc(): int {
  if lexTokenType == TOKEN_PLUS {
    advanceParser() // eat the plus
    type = unary()
    if type == TYPE_INT {
      return type
    }
    typeError("Cannot codegen positive non-ints")
    exit
  } elif lexTokenType == TOKEN_MINUS {
    advanceParser() // eat the minus
    type = unary()
    if type == TYPE_INT {
      // TODO: This is too big for ints, should just use EAX
      print "  neg RAX  ; unary minus\n"
      return type
    }
    typeError("Cannot codegen negative non-ints")
    exit
  } elif lexTokenType == TOKEN_KEYWORD and lexTokenKw == KW_LENGTH {
    advanceParser() // eat the length
    expectToken(TOKEN_LPAREN, '(')
    type = expr()
    expectToken(TOKEN_RPAREN, ')')
    if type == TYPE_STRING {
      print "  mov RCX, RAX\n"
      emitExtern("strlen")
    } elif isArrayType(type) {
      // RAX has location of array
      print "  inc RAX               ; skip past # of dimensions\n"
      print "  mov DWORD EAX, [RAX]  ; get length (4 bytes only)\n" // Fun fact: the upper 32 bits are zero-extended
    }
    else {
      typeError("Cannot take LENGTH of " + typeName(type))
      exit
    }
    return TYPE_INT
  } elif lexTokenType == TOKEN_KEYWORD and lexTokenKw == KW_ASC {
    advanceParser() // eat the asc
    expectToken(TOKEN_LPAREN, '(')
    type = expr()
    expectToken(TOKEN_RPAREN, ')')
    if type != TYPE_STRING {
      typeError("Cannot take ASC of " + typeName(type))
      exit
    }

    // get the first character (byte)
    print "  mov BYTE AL, [RAX]\n"
    // clear out the high bytes
    print "  and RAX, 255\n"
    return TYPE_INT

  } elif lexTokenType == TOKEN_KEYWORD and lexTokenKw == KW_CHR {
    advanceParser() // eat the chr

    expectToken(TOKEN_LPAREN, '(')
    type = expr()
    print "  push RAX\n" // save the int to be made into a string
    expectToken(TOKEN_RPAREN, ')')

    if type != TYPE_INT {
      typeError("Cannot take CHR of " + typeName(type))
      exit
    }

    // allocate a 2-byte string
    print "  mov RCX, 2\n"
    print "  mov RDX, 1\n"
    emitExtern("calloc")
    print "  pop RBX\n"
    print "  mov BYTE [RAX], BL  ; store byte\n"
    return TYPE_STRING
  } elif lexTokenType == TOKEN_KEYWORD and lexTokenKw == KW_NOT {
    advanceParser() // eat the NOT

    type = expr()

    if type != TYPE_BOOL {
      typeError("Cannot apply NOT to " + typeName(type))
      exit
    }

    print "  xor AL, 0x01  ; NOT\n"
    return type
  } elif lexTokenType == TOKEN_KEYWORD and lexTokenKw == KW_NEW {
    advanceParser() // eat the NEW
    recordName = lexTokenString
    if lexTokenType != TOKEN_VARIABLE {
      expectToken(TOKEN_VARIABLE, "variable")
      exit
    }
    advanceParser() // eat the variable
    // make sure it's a record
    index = lookupRecord(recordName)
    if index == -1 {
      typeError("Record '" + recordName + "' not defined")
      exit
    }
    size = recordSizes[index]
    print "  mov RCX, " print size print " ; size of record type " print recordName print "\n"
    print "  mov RDX, 1\n"
    emitExtern("calloc")
    return TYPE_RECORD_BASE + index
  }

  return composite()
}


// Makes sure RAX as index is >= 0
// TODO: Make this a subroutine
generateIndexPositiveCheck: proc() {
  oklabel = nextLabel("index_ge0")
  print "  cmp RAX, 0\n"
  print "  jge " print oklabel print "\n"
  // TODO: include the line # and index here
  errorindex = addStringConstant("\nERROR: index cannot be negative\n")
  print "  mov RCX, CONST_" print errorindex print "\n"
  emitExtern("printf")
  emitExtern("_flushall")
  print "  mov RCX, -1\n"
  emitExtern("exit")

  emitLabel(oklabel)
}


// Generate a "get" of foo[int]
// returns the base array type
generateArrayIndex: proc(arrayType: int): int {
  baseType = toBaseType(arrayType)

  print "  push RAX  ; save array base location\n"
  indexType = expr()
  if indexType != TYPE_INT {
    typeError("Array index must be int; was " + typeName(indexType))
    exit
  }
  expectToken(TOKEN_RBRACKET, ']')

  generateIndexPositiveCheck()
  // TODO: make sure index < length

  // 1. multiply index by size
  print "  imul RAX, " print sizeByType(baseType) print "  ; bytes per entry (temporarily 8)\n"
  // 2. add 5
  print "  add RAX, 5  ; skip header\n"
  // 3. add to base
  print "  pop RBX  ; base location\n"
  print "  add RAX, RBX  ; add to base location\n"
  // 4. get value
  // TODO: This is too big for ints or bools
  print "  mov RAX, [RAX]  ; get value\n"

  return baseType
}

// Generate a "get" of foo[int]
generateStringIndex: proc() {
  print "  push RAX\n"
  indexType = expr()
  if indexType != TYPE_INT {
    typeError("String index must be int; was " + typeName(indexType))
    exit
  }
  expectToken(TOKEN_RBRACKET, ']')
  print "  push RAX\n"

  generateIndexPositiveCheck()
  // TODO: check that index < string length

  // allocate a 2-byte string
  print "  mov RCX, 2\n"
  print "  mov RDX, 1\n"
  emitExtern("calloc")
  print "  pop RBX\n"
  print "  pop RCX\n"
  print "  add RCX, RBX  ; base+index\n"
  print "  mov BYTE CL, [RCX]  ; get byte\n"
  print "  mov BYTE [RAX], CL  ; store byte\n"
}

// atom | atom [ index ] | atom . fieldname
composite: proc(): int {
  leftType = atom()
  while lexTokenType == TOKEN_LBRACKET or lexTokenType == TOKEN_DOT {
    generateNpeTest()

    if lexTokenType == TOKEN_LBRACKET {
      // array or string index
      expectToken(TOKEN_LBRACKET, '[')

      if isArrayType(leftType) {
        // Overwrite return type
        leftType = generateArrayIndex(leftType)
      } elif leftType == TYPE_STRING {
        generateStringIndex()
      } else {
        typeError("Cannot take index of " + typeName(leftType))
        exit
      }
    } elif lexTokenType == TOKEN_DOT {
      // field
      expectToken(TOKEN_DOT, '.')
      if isRecordType(leftType) {
        if lexTokenType != TOKEN_VARIABLE {
          typeError("Expected field name but found: " + lexTokenString)
          exit
        }
        fieldName = lexTokenString

        advanceParser() // eat the field name

        index = leftType - TYPE_RECORD_BASE
        fieldIndex = lookupField(index, fieldName)
        if fieldIndex == -1 {
          typeError("Unknown field '" + fieldName + "' of record type " + recordNames[index])
          exit
        }

        fieldType = fieldTypes[fieldIndex]
        fieldOffset = fieldOffsets[fieldIndex]
        if fieldOffset > 0 {
          print "  add RAX, " print fieldOffset print "\n"
        }

        // TODO: this is too big for ints
        print "  mov RAX, [RAX]  ; get record." print fieldName print "\n"

        // Overwrite return type to be *this* field's type
        leftType = fieldType
      } else {
        typeError("Cannot take field of " + typeName(leftType))
        exit
      }
    }
  }
  return leftType
}

generateGetVariable: proc(variable: string): int {
  varType = lookupGlobal(variable)
  if varType != TYPE_UNKNOWN {
    // TODO: This is too big for ints, should just use EAX;
    // also too big for bools
    // print "  ; source type is " print varType print "\n"
    print "  mov RAX, [_" print variable print "]  ; get global '" print variable print "'\n"
    return varType
  }
  if currentProcNum == -1 {
    // not in a proc, cannot look up local
    typeError("Cannot find global variable " + variable)
    exit
  }

  index = lookupLocal(variable)
  if index != -1 {
    offset = localOffsets[index]
    // TODO: This is too big for ints, should just use EAX;
    // also too big for bools
    print "  mov RAX, [RBP" print offset print "]  ; get local '" print variable print "'\n"
    return localTypes[index]
  }

  index = lookupParam(variable)
  if index == -1 {
    typeError("Cannot find variable " + variable)
    exit
  }
  offset = paramOffsets[index]
  // TODO: This is too big for ints, should just use EAX;
  // also too big for bools
  print "  mov RAX, [RBP+" print offset print "]  ; get param '" print variable print "'\n"
  return paramTypes[index]
}

REGISTERS:string[4]
REGISTERS[0]="RBX"
REGISTERS[1]="RCX"
REGISTERS[2]="RDX"
REGISTERS[3]="RSI"

generateProcCall: proc(procname: string) {
  expectToken(TOKEN_LPAREN, '(')

  numArgs = 0
  while lexTokenType != TOKEN_RPAREN and lexTokenType != TOKEN_EOF {
    numArgs = numArgs + 1
    argType = expr()
    // TODO: check arg type
    print "  push RAX  ; push arg\n"
    if lexTokenType == TOKEN_COMMA {
      advanceParser() // eat the comma
    }
  }

  // reverse the order of the top of the stack
  if numArgs > 1 {
    i = 0 while i < numArgs do i = i + 1 {
      print "  pop " print REGISTERS[i] print "\n"
    }
    i = 0 while i < numArgs do i = i + 1 {
      print "  push " print REGISTERS[i] print "\n"
    }
  }

  expectToken(TOKEN_RPAREN, ')')

  // emit call; the return value will be in RAX, EAX, AL
  print "  call _" print procname print "\n"

  // # of bytes we have to adjust the stack (pseudo-pop)
  bytes = 8 * numArgs
  if bytes > 0 {
    print "  add RSP, " print bytes print "  ; adjust stack for pushed params\n\n"
  }
}

generateInput: proc() {
  emitExtern("_flushall")

  // 1. calloc 1mb
  print "  mov RDX, 1048576  ; allocate 1mb\n"
  print "  mov RCX, 1\n"
  emitExtern("calloc")
  print "  push RAX\n"

  // 3. _read up to 1mb
  print "  mov RCX, 0  ; 0=stdio\n"
  print "  mov RDX, RAX  ; destination\n"
  print "  mov R8, 1048576  ; count\n"
  emitExtern("_read")

  // TODO: create a smaller buffer with just the right size, then copy to it,
  // then free the original 1mb buffer.
  print "  pop RAX\n"
}

// atom -> constant | variable | variable '(' args ')' | '(' expr ')' | null
atom: proc(): int {
  if lexTokenType == TOKEN_KEYWORD and lexTokenKw == KW_NULL {
    advanceParser()
    print "  mov RAX, 0  ; null\n"
    return TYPE_NULL
  } elif lexTokenType == TOKEN_STRING {
    // string constant
    index = addStringConstant(lexTokenString)
    advanceParser()
    print "  mov RAX, CONST_" print index print "\n"
    return TYPE_STRING

  } elif lexTokenType == TOKEN_INT {
    // int constant
    intval = lexTokenInt
    advanceParser()
    // TODO: This is too big for ints, should just use EAX
    print "  mov RAX, " print intval print "\n"
    return TYPE_INT

  } elif lexTokenType == TOKEN_BOOL {
    // bool constant
    boolval = lexTokenBool
    advanceParser()
    if boolval {
      print "  mov AL, 1\n"
    } else {
      print "  xor RAX, RAX\n"
    }
    return TYPE_BOOL

  } elif lexTokenType == TOKEN_VARIABLE {

    variable = lexTokenString
    advanceParser() // eat the variable
    if lexTokenType != TOKEN_LPAREN {
      varType = generateGetVariable(variable)
      return varType
    }

    // procedure call
    generateProcCall(variable)

    type = lookupProcReturnType(variable)
    if type == TYPE_VOID {
      typeError("Return type of " + variable + " is void. Cannot assign it to a variable.")
      exit
    }

    return type

  } elif lexTokenType == TOKEN_LPAREN {

    // (expr)
    advanceParser()  // eat the (
    exprType = expr()

    expectToken(TOKEN_RPAREN, ')')

    return exprType
  } elif lexTokenType == TOKEN_KEYWORD and lexTokenKw == KW_INPUT {
    advanceParser() // eat the input
    generateInput()
    return TYPE_STRING
  }

  parserError("Cannot parse token in atom(): " + lexTokenString)
  exit
}


///////////////////////////////////////////////////////////////////////////////
//                               STATEMENT RULES                             //
///////////////////////////////////////////////////////////////////////////////

parseType: proc(): int {
  i = 1 while i <= 3 do i = i + 1 {
    if TYPE_NAMES[i] == lexTokenString {
      advanceParser()
      // TODO: allow arrays here
      return i
    }
  }
  // Lookup record here
  recordIndex = lookupRecord(lexTokenString)
  if recordIndex != -1 {
    print "  ; found record type " print lexTokenString print "\n"
    advanceParser()
    return TYPE_RECORD_BASE + recordIndex
  }

  parserError("Unknown type " + lexTokenString)
  exit
}


// Array declaration
parseArrayDecl: proc(variable: string) {
  // the next token should be a type.
  baseType = parseType()
  expectToken(TOKEN_LBRACKET, '[')

  sizeType = expr()
  if sizeType != TYPE_INT {
    typeError("Array size must be an int, but was " + typeName(sizeType))
    exit
  }
  expectToken(TOKEN_RBRACKET, ']')

  // allocate "RAX * sizebyType + 5" bytes:
  print "  mov EBX, EAX  ; save size\n"
  print "  imul RAX, " print sizeByType(baseType) print "  ; bytes per entry\n"
  print "  add RAX, 5    ; 5 more bytes for dimensions & # entries\n"
  print "  mov RCX, RAX  ; num items\n"
  print "  mov RDX, 1    ; bytes per item\n"
  emitExtern("calloc")
  // byte 1 is for # of dimensions, byte 2 is # of entries
  print "  mov BYTE [RAX], 1  ; # of dimensions\n"
  print "  mov DWORD [RAX+1], EBX  ; # of entries\n"

  arrayType = baseType + TYPE_ARRAY
  generateAssignment(variable, arrayType)
}

// Whether we have seen a "return" in this proc.
hasReturn=false

// Procedure declaration
parseProc: proc(procName: string) {
  hasReturn=false

  expectKeyword(KW_PROC)
  if currentProcNum != -1 {
    parserError("Cannot define nested procs")
    exit
  }
  setCurrentProcNum(procName)

  if lexTokenType == TOKEN_LPAREN {
    // if next token is (, advance parser, read parameters until )
    expectToken(TOKEN_LPAREN, '(')

    // Parse params (but not really; they've already been added to the symbol table)
    while lexTokenType != TOKEN_RPAREN {
      expectToken(TOKEN_VARIABLE, 'variable')
      expectToken(TOKEN_COLON, ':')
      parseType()

      if lexTokenType == TOKEN_COMMA {
        advanceParser() // eat the comma
      } else {
        break
      }
    }

    expectToken(TOKEN_RPAREN, ')')
  }

  // if next token is :, read return type
  returnType = TYPE_VOID
  if lexTokenType == TOKEN_COLON {
    advanceParser()  // eat the :
    returnType = parseType()
  }

  afterProc = nextLabel("afterProc")
  print "\n  ; guard around proc\n"
  print "  jmp " print afterProc print "\n"

  // start of proc
  print "\n  ; proc " print procName print ":\n"
  emitLabel("_" + procName)
  print "  push RBP\n"
  print "  mov RBP, RSP\n"
  // int bytes = 16 * (op.localBytes() / 16 + 1);
  // assume localbytes = 10*8=80, bytes = 96
  print "  sub RSP, 96  ; space for up to 10 8-byte locals\n\n"

  parseBlock()
  if returnType != TYPE_VOID {
    if hasReturn {} else {
      // ugh, v0 doesn't support NOT
      parserError("No return from proc '" + procName)
      exit
    }
  }
  currentProcNum = -1

  print "__exit_of_" print procName print ":\n"
  print "  mov RSP, RBP\n"
  print "  pop RBP\n"
  print "  ret\n"
  emitLabel(afterProc)
}

parseProcSignature: proc(procName: string) {
  myProcNum = numProcs
  if lexTokenType == TOKEN_LPAREN {
    // if next token is (, advance parser, read parameters until )
    expectToken(TOKEN_LPAREN, '(')

    // Parse params
    offset = 8 // first 8 bytes is for return address
    paramIndex = myProcNum * PARAMS_PER_PROC
    index = 0
    while lexTokenType != TOKEN_RPAREN {
      if lexTokenType != TOKEN_VARIABLE {
        expectToken(TOKEN_VARIABLE, "variable")
        exit
      }
      if numParams[myProcNum] == PARAMS_PER_PROC {
        typeError("More than 4 parameters declared for proc " + procName)
        exit
      }
      paramName = lexTokenString
      advanceParser() // eat the param name

      expectToken(TOKEN_COLON, ':')

      type = parseType()

      // store the name and type of the parameter
      // TODO: detect duplicate parameters
      // TODO: write registerParam(paramName, type, index)
      paramNames[paramIndex] = paramName
      paramTypes[paramIndex] = type
      offset = offset + 8 // all params start at 8 bytes increments on the stack (?)
      paramOffsets[paramIndex] = offset
      paramIndex = paramIndex + 1
      index = index + 1
      numParams[myProcNum] = numParams[myProcNum] + 1

      if lexTokenType == TOKEN_COMMA {
        advanceParser()
      } else {
        break
      }
    }
    expectToken(TOKEN_RPAREN, ')')
  }

  // if next token is :, read return type
  returnType = TYPE_VOID
  if lexTokenType == TOKEN_COLON {
    advanceParser()  // eat the :
    returnType = parseType()
  }
  registerProc(procName, returnType)
  // print "; procs: " print procNames[0] print "\n"
  // print "; numParams: " print numParams[0] print "\n"
  // print "; return types: " print returnTypes print "\n"
  // print "; params: " print paramNames[0] print "\n"
  // print "; param types: " print paramTypes[0] print "\n"
  // print "; offsets    : " print paramOffsets print "\n"
}

isAtStartOfExpression: proc(): bool {
  if lexTokenType == TOKEN_KEYWORD {
    return
      lexTokenKw == KW_ASC or
      lexTokenKw == KW_CHR or
      lexTokenKw == KW_INPUT or
      lexTokenKw == KW_LENGTH or
      lexTokenKw == KW_NEW or
      lexTokenKw == KW_NOT
  }
  return
    lexTokenType == TOKEN_INT or
    lexTokenType == TOKEN_BOOL or
    lexTokenType == TOKEN_STRING or
    lexTokenType == TOKEN_BIT_NOT or
    lexTokenType == TOKEN_LPAREN or
    lexTokenType == TOKEN_MINUS or
    lexTokenType == TOKEN_PLUS or
    lexTokenType == TOKEN_VARIABLE
}

parseReturn: proc() {
  // if we're not in a procedure: error
  if currentProcNum == -1 {
    parserError("Cannot return from outside proc")
    exit
  }

  hasReturn=true

  currentProcName = procNames[currentProcNum]
  actualType = TYPE_VOID
  if isAtStartOfExpression() {
    // if we're at the start of an expression, parse it.
    actualType = expr()
  }

  expectedType = returnTypes[currentProcNum]

  // Check that return types match
  if actualType != expectedType {
    typeError("Incorrect return type of '" + currentProcName + "'. Expected "
      + typeName(expectedType) + " but found " + typeName(actualType))
    exit
  }
  print "  jmp __exit_of_" print currentProcName print "\n"
}

generateAssignment: proc(variable: string, exprType: int): int {
  varType = lookupGlobal(variable)
  // print "  ; in assignment exprType is " print exprType print "\n"
  isGlobal = varType != TYPE_UNKNOWN or currentProcNum == -1
  if isGlobal {
    if varType == TYPE_UNKNOWN {
      registerGlobal(variable, exprType)
      varType = exprType
    }

    // TODO: This is too big for ints, should just use EAX for int, AL for bool
    print "  mov [_" print variable print "], RAX\n\n"
    return varType
  }

  // not global; try param or local
  index = lookupParam(variable)
  offset = 0
  if index != -1 {
    // found it
    offset = paramOffsets[index]
    varType = paramTypes[index]
    print "  mov [RBP+" print offset print "], RAX  ; assignment to param '" print variable print "'\n\n"
    return varType
  }

  index = lookupLocal(variable)
  if index != -1 {
    offset = localOffsets[index]
    varType = localTypes[index]
  } else {
    offset = registerLocal(variable, exprType)
    varType = exprType
  }
  print "  mov [RBP" print offset print "], RAX  ; assignment to local '" print variable print "'\n\n"
  return varType
}

generateArraySet: proc(variable: string) {
  type = lookupType(variable)

  // Make sure 'variable' is an array
  if isArrayType(type) { // TODO: flip this logic: if not isArrayType(type)...

    expectToken(TOKEN_LBRACKET, '[')
    indexType = expr()
    if indexType != TYPE_INT {
      typeError("Array index must be INT; was " + typeName(indexType))
      exit
    }
    print "  mov RBX, RAX\n"
    // TODO: FIX ME
    print "  shl RBX, 3  ; bytes per element TEMPORARY\n"
    print "  add RBX, 5  ; header\n"
    generateGetVariable(variable)
    print "  add RBX, RAX\n"
    print "  push RBX\n"
    expectToken(TOKEN_RBRACKET, ']')
    expectToken(TOKEN_EQ, '=')

    rightType = expr()

    checkTypes(toBaseType(type), rightType)

    print "  pop RBX\n"
    print "  mov [RBX], RAX  ; array set\n\n"
  } else {
    typeError("Cannot take index of non-array variable " + variable)
    exit
  }
}

parseRecordDecl: proc(recordName: string) {
  recIndex = registerRecord(recordName)
  expectKeyword(KW_RECORD)
  expectToken(TOKEN_LBRACE, "{")

  offset = 0
  fieldIndex = recIndex * FIELDS_PER_RECORD
  index = 0
  size = 0
  // zero or more variable declarations, NOT followed by commas
  while lexTokenType != TOKEN_RBRACE and lexTokenType != TOKEN_EOF {
    // record the field names and types
    if lexTokenType != TOKEN_VARIABLE {
      expectToken(TOKEN_VARIABLE, 'variable')
      exit
    }
    if numFields[recIndex] == FIELDS_PER_RECORD {
      typeError("More than 20 parameters declared for record " + recordName)
      exit
    }
    fieldName = lexTokenString
    advanceParser() // eat the field name

    expectToken(TOKEN_COLON, ':')

    type = parseType()

    // store the name and type of the field
    // TODO: detect duplicate fields
    // registerField(fieldName, type)
    fieldNames[fieldIndex] = fieldName
    fieldTypes[fieldIndex] = type
    fieldOffsets[fieldIndex] = offset
    size = size + sizeByType(type)
    offset = offset + sizeByType(type)
    fieldIndex = fieldIndex + 1
    index = index + 1
    numFields[recIndex] = numFields[recIndex] + 1
  }
  expectToken(TOKEN_RBRACE, "}")
  recordSizes[recIndex] = size

  if debug {
    print "; # records: " print numRecords print "\n"
    print "; record name: " print recordNames[recIndex] print "\n"
    print "; numFields: " print numFields[recIndex] print "\n"
    print "; size: " print recordSizes[recIndex] print "\n"
    print "; fields: " i=0 while i < numFields[recIndex] do i = i + 1 {print fieldNames[recIndex*FIELDS_PER_RECORD+i] print " " }
    print "\n"
    print "; field types: " i=0 while i < numFields[recIndex] do i = i + 1 {print typeName(fieldTypes[recIndex*FIELDS_PER_RECORD+i]) print " " }
    print "\n"
    print "; offsets: " i=0 while i < numFields[recIndex] do i = i + 1 {print fieldOffsets[recIndex*FIELDS_PER_RECORD+i] print " " }
    print "\n"
  }
}


skipRecordDecl: proc(recordName: string) {
  expectKeyword(KW_RECORD)
  expectToken(TOKEN_LBRACE, "{")
  // zero or more variable declarations, NOT followed by commas
  while lexTokenType != TOKEN_RBRACE and lexTokenType != TOKEN_EOF {
    expectToken(TOKEN_VARIABLE, 'field')
    expectToken(TOKEN_COLON, ':')
    parseType()
  }
  expectToken(TOKEN_RBRACE, "}")
}

generateFieldSet: proc(variable: string) {
  generateGetVariable(variable)
  generateNpeTest()
  print "  push RAX\n"  // base of record in memory

  expectToken(TOKEN_DOT, '.')
  type = lookupType(variable)

  if lexTokenType != TOKEN_VARIABLE {
    typeError("Expected field name but found: " + lexTokenString)
    exit
  }
  fieldName = lexTokenString

  advanceParser() // eat the field name
  expectToken(TOKEN_EQ, "=")

  rhsType = expr()

  index = type - TYPE_RECORD_BASE
  fieldIndex = lookupField(index, fieldName)
  if fieldIndex == -1 {
    typeError("Unknown field '" + fieldName + "' of record " + recordNames[index])
    exit
  }
  fieldType = fieldTypes[fieldIndex]

  checkTypes(fieldType, rhsType)
  fieldOffset = fieldOffsets[fieldIndex]
  // print "  ; setting field index " print fieldIndex print " of record type " print recordNames[index] print "\n"
  print "  pop RBX\n"
  if fieldOffset > 0 {
    print "  add RBX, " print fieldOffset print "  ; add field offset\n"
  }

  // TODO: this is too big for ints
  print "  mov [RBX], RAX  ; set " print variable print "." print fieldName print "\n\n" // store it
}

// variable=expression, procname: proc(), procname(), arrayname:type[intexpr]
parseStartsWithVariable: proc() {
  variable = lexTokenString
  advanceParser()  // eat the variable
  if lexTokenType == TOKEN_EQ {
    advanceParser()  // eat the eq

    exprType = expr()
    varType = generateAssignment(variable, exprType)

    checkTypes(varType, exprType)

    return
  } elif lexTokenType == TOKEN_COLON {
    advanceParser() // eat the colon
    if lexTokenType == TOKEN_KEYWORD and lexTokenKw == KW_PROC {
      parseProc(variable)
    } elif lexTokenType == TOKEN_KEYWORD and lexTokenKw == KW_RECORD {
      skipRecordDecl(variable)
    } else {
      parseArrayDecl(variable)
    }
    return
  } elif lexTokenType == TOKEN_LPAREN {
    generateProcCall(variable)
    return
  } elif lexTokenType == TOKEN_LBRACKET {
    // array set
    generateArraySet(variable)
    return
  } elif lexTokenType == TOKEN_DOT {
    generateFieldSet(variable)
    return
  }

  parserError("expected one of '=' ':' '(' '[' but found: " + lexTokenString)
  exit
}

// expect {, parse statements until }
parseBlock: proc() {
  expectToken(TOKEN_LBRACE, '{')
  while lexTokenType != TOKEN_RBRACE and lexTokenType != TOKEN_EOF {
    parseStmt()
  }
  expectToken(TOKEN_RBRACE, '}')
}

parseIf: proc() {
  // 1. calculate the condition
  print "  ; 'if' condition\n"
  condType = expr()

  if condType != TYPE_BOOL {
    typeError("Expected boolean condition in if but found " + typeName(condType))
    exit
  }

  // 2. if false, go to else
  elseLabel = nextLabel('else')
  print "  cmp AL, 0\n"
  print "  jz " print elseLabel print "\n\n"

  // 3. { parse statements until }
  print "  ; 'if' block\n"
  parseBlock()

  // this may not be necessary if there are no elses or elifs
  // after the successful "if" block, jump down to the end.
  afterAllLabel = nextLabel('afterIf')
  print "  jmp " print afterAllLabel print "\n"

  // 4. elseLabel: while token = elif
  emitLabel(elseLabel)
  while lexTokenType == TOKEN_KEYWORD and lexTokenKw == KW_ELIF {
    print "  ; 'elif' block\n"
    advanceParser() // eat the elif
    condType = expr()
    if condType != TYPE_BOOL {
      typeError("Expected boolean condition in elif but found " + typeName(condType))
      exit
    }

    elseLabel = nextLabel('afterElif')
    print "  cmp AL, 0\n"
    // if the condition is false, go to our local elseLabel
    print "  jz " print elseLabel print "\n\n"
    parseBlock()
    // after successful "elif" block, jump to the end
    print "  jmp " print afterAllLabel print "\n"
    emitLabel(elseLabel)
  }

  // 5. else:
  if lexTokenType == TOKEN_KEYWORD and lexTokenKw == KW_ELSE {
    print "  ; 'else' block\n"
    advanceParser() // eat the "else"
    parseBlock()
  }

  // 6. afterAll label
  emitLabel(afterAllLabel)
}

numWhiles=0
whileBreakLabels:string[100]
whileContinueLabels:string[100]

parseBreak: proc() {
  if numWhiles == 0 {
    parserError("Cannot have break outside while loop")
    exit
  }
  print "  jmp " print whileBreakLabels[numWhiles - 1] print "\n"
}

parseContinue: proc() {
  if numWhiles == 0 {
    parserError("Cannot have continue outside loop")
    exit
  }
  print "  jmp " print whileContinueLabels[numWhiles - 1] print "\n"
}

parseWhile: proc() {
  continueLabel = nextLabel("while_continue")
  emitLabel(continueLabel)

  condType = expr()
  if condType != TYPE_BOOL {
    typeError("Expected boolean condition in while but found " + typeName(condType))
    exit
  }
  print "  cmp AL, 0\n"
  afterLabel = nextLabel("after_while")
  print "  jz " print afterLabel print "\n"
  // push the "afterLabel" onto the stack
  whileBreakLabels[numWhiles] = afterLabel

  // process 'do'
  doLabel = ""
  hasDo = false
  whileBlockLabel = ""
  if lexTokenType == TOKEN_KEYWORD and lexTokenKw == KW_DO {
    hasDo = true

    advanceParser() // eat "do"

    // This is a guard so the "do" is only run at the end of the loop.
    // We can't emit it at the end, because we have to emit the block first,
    // so we jump past it here, and back to it after the block.
    whileBlockLabel = nextLabel("while_block")
    print "  jmp " print whileBlockLabel print "\n"

    doStmtLabel = nextLabel("do_stmt")
    // do_stmt:
    emitLabel(doStmtLabel)
    //    emit do code
    parseStmt()
    //    jmp while_continue
    print "  jmp " print continueLabel print "\n"
  }

  // whileBlock:
  print "  ; while block\n"
  // Push either "doStmtLabel" or "continueLabel" onto the "continue stack"
  if hasDo {
    emitLabel(whileBlockLabel)
    // push the "afterLabel" onto the stack
    whileContinueLabels[numWhiles] = doStmtLabel
  } else {
    whileContinueLabels[numWhiles] = continueLabel
  }
  numWhiles = numWhiles + 1

  parseBlock()

  if hasDo {
    // jump back to the "do", which will then jump back to the continuelabel
    print "  jmp " print doStmtLabel print "\n"
  }  else {
    print "  jmp " print continueLabel print "\n"
  }
  emitLabel(afterLabel)

  // Pop the while label stack
  numWhiles = numWhiles - 1
}

parsePrint: proc(isPrintln: bool) {
  exprType = expr()  // puts result in RAX
  if exprType == TYPE_NULL {
    nullindex = addStringConstant("null")
    print "  mov RCX, CONST_" print nullindex print "\n"
  } elif exprType == TYPE_STRING {
    print "  mov RCX, RAX\n"
  } elif exprType == TYPE_BOOL {
    trueindex = addStringConstant("true")
    falseindex = addStringConstant("false")
    print "  cmp AL, 1\n"
    print "  mov RCX, CONST_" print falseindex print "\n"
    print "  mov RDX, CONST_" print trueindex print "\n"
    print "  cmovz RCX, RDX\n"
  } elif exprType == TYPE_INT {
    index = addStringConstant("%d")
    print "  mov RCX, CONST_" print index print "\n"
    // TODO: This is too big for ints, should just use EDX, EAX
    print "  mov RDX, RAX\n"
  } else {
    parserError("Cannot generate printing " + typeName(exprType))
    exit
  }
  print "  sub RSP, 0x20\n"
  print "  extern printf\n"
  print "  call printf  ; print " print typeName(exprType) print "\n"
  print "  extern _flushall\n"
  print "  call _flushall\n"
  print "  add RSP, 0x20\n\n"
  if isPrintln {
    print "  mov RCX, 10\n"
    emitExtern("putchar")
  }
}

parseStmt: proc() {
  if lexTokenType == TOKEN_EOF {
    return
  } elif lexTokenType == TOKEN_KEYWORD {
    kw = lexTokenKw
    advanceParser() // eat the token
    if kw == KW_PRINT or kw==KW_PRINTLN {
      parsePrint(kw==KW_PRINTLN)
      return
    } elif kw == KW_EXIT {
      print "  mov RCX, -1\n"
      print "  call exit\n"
      return
    } elif kw == KW_IF {
      parseIf()
      return
    } elif kw == KW_WHILE {
      parseWhile()
      return
    } elif kw == KW_BREAK {
      parseBreak()
      return
    } elif kw == KW_CONTINUE {
      parseContinue()
      return
    } elif kw == KW_RETURN {
      parseReturn()
      return
    }
  } elif lexTokenType == TOKEN_VARIABLE {
    parseStartsWithVariable()
    return
  }

  parserError("Cannot parse start of statement. Found: " + lexTokenString)
  exit
}


///////////////////////////////////////////////////////////////////////////////
//                          MAIN LOOP & OUTPUT ROUTINES                      //
///////////////////////////////////////////////////////////////////////////////


emitStringTable: proc() {
  i = 0 while i < numStrings do i = i + 1 {
    print "  CONST_" print i print ': db "'
    entry = stringTable[i]
    j = 0 while j < length(entry) do j = j + 1 {
      ch = asc(entry[j])
      if ch == 10 or ch == 13 or ch == 34 or ch == 37 {
        // unprintable characters (\n, \r, " or %) become ints
        print '", ' print ch print ', "'
      } else {
        print entry[j]
      }
    }
    print '", 0\n'
  }
}

emitGlobalTable: proc() {
  i = 0 while i < numGlobals do i = i + 1 {
    print "  _" print globalNames[i] print ": "
    if globalTypes[i] == TYPE_STRING {
      print "dq"
    } else {
      // TODO: this is 64 bits, which is too many for a 32-bit int or a boolean
      print "dq"
    }
    print " 0\n"
  }
}

parseProgram: proc() {
  print "global main\n"
  print "extern exit\n\n"
  print "section .text\n"
  print "main:\n"
  while lexTokenType != TOKEN_EOF {
    parseStmt()
  }
  print "  mov RCX, 0\n"
  print "  call exit\n\n"

  if numStrings > 0 or numGlobals > 0 {
    print "section .data\n"
  }
  if numStrings > 0 {
    emitStringTable()
  }
  if numGlobals > 0 {
    emitGlobalTable()
  }
}

procFinder: proc() {
  while lexTokenType != TOKEN_EOF {
    if lexTokenType == TOKEN_VARIABLE {
      variable = lexTokenString
      advanceParser() // eat the variable
      if lexTokenType == TOKEN_COLON {
        advanceParser() // eat the colon
        if lexTokenType == TOKEN_KEYWORD and lexTokenKw == KW_PROC {
          advanceParser() // eat "proc"
          parseProcSignature(variable)
        }
        continue
      }
      continue
    }
    advanceParser()
  }
  resetLexer()
  advanceParser()
}

recordFinder: proc() {
  while lexTokenType != TOKEN_EOF {
    if lexTokenType == TOKEN_VARIABLE {
      variable = lexTokenString
      advanceParser() // eat the variable
      if lexTokenType == TOKEN_COLON {
        advanceParser() // eat the colon
        if lexTokenType == TOKEN_KEYWORD and lexTokenKw == KW_RECORD {
          parseRecordDecl(variable)
        }
        continue
      }
      continue
    }
    advanceParser()
  }
  resetLexer()
  advanceParser()
}

initParser: proc() {
  text = input
//    print "text = " print text print "\n"
  if debug {
  }
  newLexer(text)
  advanceParser()
}


initParser()
recordFinder()
procFinder()
parseProgram()
