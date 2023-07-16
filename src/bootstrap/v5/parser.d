///////////////////////////////////////////////////////////////////////////////
//                                    PARSER                                 //
///////////////////////////////////////////////////////////////////////////////

Parser: record {
  lexer: Lexer
  emitter: Emitter
  labelId: int
  npeCheckNeeded: bool
  indexPositiveCheckNeeded: bool
  numWhiles: int
  token: Token
}

parser=initParser()

advanceParser: proc: Token {
  lastToken = parser.token
  token = nextToken(parser.lexer)
  if debug {
    //println "; new token is " + parser.token.stringValue
  }
  parser.token = token
  return lastToken
}

expectToken: proc(expectedTokenType: int, tokenStr: string) {
  if parser.token.type != expectedTokenType  {
    parserError("Unexpected token. Expected: " + tokenStr + ", actual: '" + parser.token.stringValue + "'")
    exit
  }
  advanceParser() // eat the expected token
}

expectKeyword: proc(expectedKwType: int) {
  if parser.token.type != TOKEN_KEYWORD or parser.token.keyword != expectedKwType {
    parserError("Unexpected token. Expected: " + KEYWORDS[expectedKwType] + ", actual: '" + parser.token.stringValue + "'")
    exit
  }
  advanceParser() // eat the keyword
}


///////////////////////////////////////////////////////////////////////////////
//                                CODEGEN UTILS                              //
///////////////////////////////////////////////////////////////////////////////

nextLabel: proc(prefix: string): string {
  parser.labelId = parser.labelId + 1
  return "__" + prefix + toString(parser.labelId)
}

emit0: proc(s: string): StringListEntry {
  return emitter_emit0(parser.emitter, s)
}

emit: proc(s: string): StringListEntry {
  return emitter_emit(parser.emitter, s)
}

emitNum: proc(s: string, n: int): StringListEntry {
  return emitter_emitNum(parser.emitter, s, n)
}

emitLabel: proc(s: string): StringListEntry {
  return emitter_emitLabel(parser.emitter, s)
}

emitExtern: proc(name: string) {
  // TODO: don't emit if we already emitted this extern
  emit("extern " + name)
  emit("sub RSP, 0x20")
  emit("call " + name)
  emit("add RSP, 0x20")
}

emitNpeCheckProc: proc {
  afterProc = nextLabel("after_npe")
  emit("jmp " + afterProc)
  emitLabel("__npe_check_proc")
  emit("push RBP")
  emit("mov RBP, RSP")

  emit("cmp RAX, 0")
  okLabel = nextLabel("not_null")
  emit("jne " + okLabel)

  npeMessageIndex = addStringConstant("\nNull pointer error at line %d.\n")
  emitNum("mov RCX, CONST_", npeMessageIndex)
  emit("mov RDX, [RBP + 16]  ; line #")
  emit("sub RSP, 0x20")
  emit("extern printf")
  emit("call printf")
  emit("extern _flushall")
  emit("call _flushall")
  emit("add RSP, 0x20")
  emit("mov RCX, -1")
  emit("extern exit")
  emit("call exit")

  emitLabel(okLabel)
  emit("mov RSP, RBP")
  emit("pop RBP")
  emit("ret")

  emitLabel(afterProc)
}

generateNpeCheck: proc {
  parser.npeCheckNeeded = true

  emitNum("mov RCX, ", parser.lexer.line)
  emit("push RCX")
  emit("call __npe_check_proc")
  emit("add RSP, 8\n")
}

emitIndexPositiveCheckProc: proc {
  afterProc = nextLabel("after_index_check")
  emit("jmp " + afterProc)
  emitLabel("__index_positive_check")
  emit("push RBP")
  emit("mov RBP, RSP")

  okLabel = nextLabel("index_ge0")
  emit("cmp EAX, 0")
  emit("jge " + okLabel)

  errorindex = addStringConstant(
    "\nInvalid index error at line %d: must not be negative; was %d\n")
  emitNum("mov RCX, CONST_", errorindex)
  emit("mov RDX, [RBP + 16]  ; line #")
  emit("mov R8D, EAX") // actual index
  emit("sub RSP, 0x20")
  emit("extern printf")
  emit("call printf")
  emit("extern _flushall")
  emit("call _flushall")
  emit("add RSP, 0x20")
  emit("mov RCX, -1")
  emit("extern exit")
  emit("call exit")

  emitLabel(okLabel)
  emit("mov RSP, RBP")
  emit("pop RBP")
  emit("ret")

  emitLabel(afterProc)
}

// Makes sure RAX is >= 0. 'type' is either STRING or ARRAY
generateIndexPositiveCheck: proc {
  parser.indexPositiveCheckNeeded = true

  emitNum("mov RCX, ", parser.lexer.line)
  emit("push RCX")
  emit("call __index_positive_check")
  emit("add RSP, 8\n")
}

// RBX has the array location
// RAX has the index.
// If EAX is < 0 or > the length, shows an error.
// Makes sure EAX is < the length of the array.
// TODO: Make this a subroutine
generateArrayIndexInRangeCheck: proc {
  generateIndexPositiveCheck()

  inRangeLabel = nextLabel("index_inRange")
  emit("mov DWORD R8d, [RBX + 1]") // 1 for dimension
  emit("cmp EAX, R8d")
  emit("jl " + inRangeLabel)

  errorindex = addStringConstant(
      "\nInvalid index error at line %d: ARRAY index out of bounds (length %d); was %d\n")
  emitNum("mov RCX, CONST_", errorindex)
  emitNum("mov RDX, ", parser.lexer.line)
  emit("mov R9d, EAX") // actual index
  emit("sub RSP, 0x20")
  emit("extern printf")
  emit("call printf")
  emit("extern _flushall")
  emit("call _flushall")
  emit("add RSP, 0x20")
  emit("mov RCX, -1")
  emit("extern exit")
  emit("call exit")

  emitLabel(inRangeLabel)
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
// atom -> numeric constant, bool constant, string constant, variable, '(' expr ')', 'input', 'args'


// Each of these returns the type of the expression
expr: proc: VarType {
  return boolOr()
}

// Map from token to binary opcode
OPCODES=[
    ";nop ",  // eof
    "add ",
    "sub ",
    "imul ",
    "and ",
    "or ",
    "xor ",
    "; div ",
    "; mod ",
    "setz ",  // ==
    "setnz ", // !=
    "setl ",  // <
    "setg ",  // >
    "setle ", // <=
    "setge ",  // >=
    "shl ",
    "shr "
]

boolOr: proc: VarType {
  leftType = boolXor()
  if leftType == TYPE_BOOL {
    while parser.token.keyword == KW_OR {
      advanceParser() // eat the symbol
      emit("push RAX")

      rightType = boolXor()
      checkTypes(leftType, rightType)

      emit("pop RBX") // pop the left side
      // left = right (bool) OR left
      emit("or AL, BL")
    }
  } elif leftType.isIntegral {
    ax = makeRegister(A_REG, leftType)
    bx = makeRegister(B_REG, leftType)
    while parser.token.type == TOKEN_BIT_OR {
      op = parser.token.type
      advanceParser() // eat the symbol
      emit("push RAX")

      rightType = boolXor()
      checkTypes(leftType, rightType)

      emit("pop RBX") // pop the left side
      // left = right (int) OR left
      emit(OPCODES[op] + ax + ", " + bx)
    }
  }
  return leftType
}

boolXor: proc: VarType {
  leftType = boolAnd()
  if leftType == TYPE_BOOL {
    while parser.token.keyword == KW_XOR {
      advanceParser() // eat the symbol
      emit("push RAX")

      rightType = boolAnd()
      checkTypes(leftType, rightType)

      emit("pop RBX") // pop the left side
      // left = right XOR left
      emit("XOR AL, BL")
    }
  } elif leftType.isIntegral {
    ax = makeRegister(A_REG, leftType)
    bx = makeRegister(B_REG, leftType)
    while parser.token.type == TOKEN_BIT_XOR {
      op = parser.token.type
      advanceParser() // eat the symbol
      emit("push RAX")

      rightType = boolAnd()
      checkTypes(leftType, rightType)

      emit("pop RBX") // pop the left side
      // left = right XOR left
      emit(OPCODES[op] + ax + ", " + bx)
    }
  }
  return leftType
}

boolAnd: proc: VarType {
  leftType = compare()
  if leftType == TYPE_BOOL {
    while parser.token.keyword == KW_AND {
      advanceParser() // eat the symbol
      emit("push RAX")

      rightType = compare()
      checkTypes(leftType, rightType)

      emit("pop RBX") // pop the left side
      // left = right and left
      emit("and AL, BL")
    }
  } elif leftType.isIntegral {
    ax = makeRegister(A_REG, leftType)
    bx = makeRegister(B_REG, leftType)
    while parser.token.type == TOKEN_BIT_AND {
      op = parser.token.type
      advanceParser() // eat the symbol
      emit("push RAX")

      rightType = compare()
      checkTypes(leftType, rightType)

      emit("pop RBX") // pop the left side
      // left = right and left
      emit(OPCODES[op] + ax + ", " + bx)
    }
  }
  return leftType
}

compare: proc: VarType {
  leftType = shift()
  if leftType.isIntegral and (parser.token.type >= TOKEN_EQEQ and parser.token.type <= TOKEN_GEQ) {
    op = parser.token.type
    opstring = parser.token.stringValue
    advanceParser() // eat the symbol
    emit("push RAX")

    rightType = shift()
    checkTypes(leftType, rightType)

    emit("pop RBX") // pop the left side

    // left = left (op) right
    ax = makeRegister(A_REG, leftType)
    bx = makeRegister(B_REG, leftType)
    emit("cmp " + bx + ", " + ax + "  ; " + leftType.name)
    emit(OPCODES[op] + "AL")
    return TYPE_BOOL
  } elif leftType == TYPE_STRING and (parser.token.type >= TOKEN_EQEQ and parser.token.type <= TOKEN_GEQ) {

    op = parser.token.type

    advanceParser() // eat the symbol
    emit("push RAX")

    rightType = shift()
    checkTypes(leftType, rightType)

    emit("mov RDX, RAX") // right side
    emit("pop RCX") // left side

    // test for either being null
    emit("imul RAX, RCX")
    emit("cmp RAX, 0")
    strcmpLabel = nextLabel("strcmp")
    emit("jne " + strcmpLabel)

    // if rax is 0, just use cmp
    setAlLabel = nextLabel("strcmpSetAl")
    emit("cmp RCX, RDX")
    emit("jmp " + setAlLabel)

    emitLabel(strcmpLabel)
    emitExtern("strcmp")
    emit("cmp RAX, 0")
    emitLabel(setAlLabel)
    emit(OPCODES[op] + "AL")
    return TYPE_BOOL

  } elif leftType == TYPE_BOOL and (parser.token.type >= TOKEN_EQEQ and parser.token.type <= TOKEN_GEQ) {
    op = parser.token.type
    opstring = parser.token.stringValue
    advanceParser() // eat the symbol
    emit("push RAX")

    rightType = shift()
    checkTypes(leftType, rightType)

    emit("pop RBX") // pop the left side
    emit("cmp BL, AL  ; bool " + opstring + " bool")
    emit(OPCODES[op] + "AL")
    return TYPE_BOOL
  } elif (leftType.isRecord or leftType == TYPE_NULL) and
         (parser.token.type == TOKEN_EQEQ or parser.token.type == TOKEN_NEQ) {
    op = parser.token.type
    opstring = parser.token.stringValue
    advanceParser() // eat the symbol
    emit("push RAX")

    rightType = shift()
    checkTypes(leftType, rightType)

    emit("pop RBX") // pop the left side
    // left = left (op) right
    // TODO: use memcmp
    emit("cmp RBX, RAX  ; record " + opstring + " record")
    emit(OPCODES[op] + "AL")
    return TYPE_BOOL
  }
  return leftType
}

shift: proc: VarType {
  leftType = addSub()
  while leftType.isIntegral and (parser.token.type == TOKEN_SHIFT_LEFT or parser.token.type == TOKEN_SHIFT_RIGHT) {
    op = parser.token.type
    opstring = parser.token.stringValue
    advanceParser() // eat the symbol
    emit("push RAX")

    rightType = addSub()
    checkTypes(leftType, rightType)

    emit("mov RCX, RAX") // amount to shift
    emit("pop RAX") // pop the left side

    // left = left (op) right
    ax = makeRegister(A_REG, leftType)
    emit(OPCODES[op] + ax + ", CL")
    return leftType
  }
  return leftType
}


addSub: proc: VarType {
  leftType = mulDiv()
  while parser.token.type == TOKEN_PLUS or parser.token.type == TOKEN_MINUS {
    op = parser.token.type
    opstring = parser.token.stringValue
    advanceParser() // eat the symbol
    emit("push RAX")

    rightType = mulDiv()
    checkTypes(leftType, rightType)

    emit("pop RBX") // pop the left side
    if leftType == TYPE_STRING and op == TOKEN_PLUS {
      // 1. mov rsi, rax. get length of rsi
      emit("mov RSI, RAX  ; lhs in RSI")
      emit("mov RCX, RSI")
      emitExtern("strlen")
      emit("mov RDI, RAX  ; RHS length in RDI")
      // 2. get length of rbx
      emit("mov RCX, RBX")
      emitExtern("strlen")
      emit("mov RCX, RAX  ; LHS length in RCX")
      // 3. add them
      emit("add RCX, RDI  ; total length in RCX")
      emit("inc RCX       ; plus one byte for null")
      // 4. allocate new string of new size, result in rax
      emit("; new string location in RAX")
      emitExtern("malloc")
      // 5. strcpy rbx to new location
      emit("push RAX      ; save new location for later")
      emit("mov RCX, RAX  ; dest (new location)")
      emit("mov RDX, RBX  ; source")
      emit("; copy LHS to new location")
      emitExtern("strcpy")
      // 6. strcat rsi to rax
      emit("pop RCX       ; get new location back into RCX as dest")
      emit("mov RDX, RSI  ; source")
      emit("; concatenate RHS at new location")
      emitExtern("strcat")
    } elif leftType.isIntegral {
      // left = left (op) right
      bx = makeRegister(B_REG, leftType)
      ax = makeRegister(A_REG, leftType)
      if op == TOKEN_PLUS {
        // If plus, can just do add eax, ebx instead of two lines
        emit("add " + ax + ", " + bx)
      } else {
        // minus; have to do sub ebx, eax
        emit("sub " + bx + ", " + ax)
        emit("mov RAX, RBX")
      }
    } else {
      typeError("Cannot apply " + parser.token.stringValue + " to " + leftType.name)
      exit
    }
  }
  return leftType
}

mulDiv: proc: VarType {
  leftType = unary()
  while leftType.isIntegral and
      (parser.token.type == TOKEN_MULT or parser.token.type == TOKEN_DIV or parser.token.type == TOKEN_MOD) {
    op = parser.token.type

    advanceParser() // eat the symbol
    emit("push RAX")

    rightType = unary()
    checkTypes(leftType, rightType)

    emit("pop RBX") // pop the left side

    // xax = xax (op) xbx
    bx = makeRegister(B_REG, leftType)
    ax = makeRegister(A_REG, leftType)
    if op == TOKEN_DIV or op == TOKEN_MOD {
      // These are all too big for bytes
      emit("xchg RAX, RBX  ; put numerator in RAX, denominator in RBX")
      if leftType == TYPE_INT {
        emit("cdq  ; sign extend eax to edx")
      } elif leftType == TYPE_LONG {
        emit("cqo  ; sign extend rax to rdx")
      } else {
        emit("cbw  ; sign extend al to ax")
      }
      emit("idiv " + bx + "  ; a=a/b")
      if op == TOKEN_MOD {
        if leftType == TYPE_BYTE {
          emit("mov AL, AH  ; remainder in ah")
        } else {
          emit("mov RAX, RDX  ; remainder in rdx/edx")
        }
      }
    } else {
      // multiply: al=al*bl or rax=rax*rbx
      emit("imul " + bx)
    }
  }
  return leftType
}

// -unary | +unary | length(expr) | asc(expr) | chr(expr) | not expr
unary: proc: VarType {
  if parser.token.type == TOKEN_PLUS {
    advanceParser() // eat the plus
    type = unary()
    if not type.isIntegral {
      typeError("Cannot apply unary plus to " + type.name)
      exit
    }
    return type
  } elif parser.token.type == TOKEN_MINUS {
    advanceParser() // eat the minus
    type = unary()
    if not type.isIntegral {
      typeError("Cannot apply unary minus to " + type.name)
      exit
    }
    // two's complement
    emit("neg " + makeRegister(A_REG, type))
    return type
  } elif parser.token.keyword == KW_LENGTH {
    advanceParser() // eat the length
    expectToken(TOKEN_LPAREN, '(')
    varType = expr()
    expectToken(TOKEN_RPAREN, ')')
    if varType == TYPE_STRING {
      generateNpeCheck()

      emit("mov RCX, RAX")
      emitExtern("strlen")
    } elif varType.isArray {
      // RAX has location of array
      emit("inc RAX  ; skip past # of dimensions")
      emit("mov DWORD EAX, [RAX]  ; get length (4 bytes only)") // Fun fact: the upper 32 bits are zero-extended
    }
    else {
      typeError("Cannot take LENGTH of " + type.name)
      exit
    }
    return TYPE_INT
  } elif parser.token.keyword == KW_ASC {
    advanceParser() // eat the asc
    expectToken(TOKEN_LPAREN, '(')
    type = expr()
    expectToken(TOKEN_RPAREN, ')')
    if type != TYPE_STRING {
      typeError("Cannot take ASC of " + type.name)
      exit
    }
    generateNpeCheck()

    // get the first character (byte)
    emit("mov BYTE AL, [RAX]")
    // clear out the high bytes
    emit("and RAX, 255")
    return TYPE_INT

  } elif parser.token.keyword == KW_CHR {
    advanceParser() // eat the chr

    expectToken(TOKEN_LPAREN, '(')
    type = expr()
    emit("push RAX") // save the int to be made into a string
    expectToken(TOKEN_RPAREN, ')')

    if type != TYPE_INT {
      typeError("Cannot take CHR of " + type.name)
      exit
    }

    // allocate a 2-byte string
    emit("mov RCX, 2")
    emit("mov RDX, 1")
    emitExtern("calloc")
    emit("pop RBX")
    emit("mov BYTE [RAX], BL  ; store byte")
    return TYPE_STRING
  } elif parser.token.keyword == KW_NOT {
    advanceParser() // eat the NOT

    type = expr()

    if type != TYPE_BOOL {
      typeError("Cannot apply NOT to " + type.name)
      exit
    }

    emit("xor AL, 0x01  ; NOT")
    return type
  } elif parser.token.type == TOKEN_BIT_NOT {
    advanceParser() // eat the !

    type = expr()

    if not type.isIntegral {
      typeError("Cannot apply ! to " + type.name)
      exit
    }

    ax = makeRegister(A_REG, type)
    emit("not " + ax)
    return type
  } elif parser.token.keyword == KW_NEW {
    advanceParser() // eat the NEW
    recordName = parser.token.stringValue
    if parser.token.type != TOKEN_VARIABLE {
      expectToken(TOKEN_VARIABLE, "variable")
      exit
    }
    advanceParser() // eat the variable
    // make sure it's a record
    recVarType = lookupRecord(recordName)
    if recVarType == null {
      typeError("Record '" + recordName + "' not defined")
      exit
    }
    size = recVarType.recordType.size
    emit("; size of record type " + recordName)
    emitNum("mov RCX, ", size)
    emit("mov RDX, 1")
    emitExtern("calloc")
    return recVarType
  }

  return composite()
}


// Generate a "get" of foo[int]
// returns the base array type
generateArrayIndex: proc(arrayType: VarType): VarType {
  baseType = arrayType.baseType // VarType

  emit("push RAX  ; save array base location")
  indexType = expr()
  if indexType != TYPE_INT {
    typeError("Array index must be int; was " + indexType.name)
    exit
  }
  expectToken(TOKEN_RBRACKET, ']')

  emit("pop RBX  ; base location")
  // RBX has location, RAX has index
  generateArrayIndexInRangeCheck()

  // 1. multiply index by size
  emitNum("imul RAX, ", baseType.size)
  // 2. add 5
  emit("add RAX, 5  ; skip header")
  // 3. add to base
  emit("add RAX, RBX  ; add to base location")
  // 4. get value

  emit("; get array slot value:")
  emit("mov " + baseType.opcodeSize + makeRegister(A_REG, baseType) + ", [RAX]")

  return baseType
}

// Generate a "get" of foo[int] for strings
generateStringIndex: proc {
  emit("push RAX  ; string location")
  indexType = expr()
  if indexType != TYPE_INT {
    typeError("String index must be int; was " + indexType.name)
    exit
  }
  expectToken(TOKEN_RBRACKET, ']')
  emit("push RAX")

  generateIndexPositiveCheck()

  // Check that index < string length

  emit("mov RBX, RAX") // save index
  emit("mov RCX, [RSP+8]") // string
  emitExtern("strlen") // rax=strlen
  emit("cmp RBX, RAX")
  goodIndexLabel = nextLabel("good_index")
  emit("jl " + goodIndexLabel)

  // out of range exception
  errorIndex = addStringConstant(
      "\nInvalid index error at line %d: out of bounds (length %d); was %d\n")
  emitNum("mov RCX, CONST_", errorIndex)
  emitNum("mov RDX, ", parser.lexer.line)
  emit("mov R8d, EAX") // length
  emit("mov R9d, EBX") // index
  emit("sub RSP, 0x20")
  emit("extern printf")
  emit("call printf")
  emit("extern _flushall")
  emit("call _flushall")
  emit("add RSP, 0x20")
  emit("mov RCX, -1")
  emit("extern exit")
  emit("call exit")

  // continue:
  emitLabel(goodIndexLabel)
  // allocate a 2-byte string
  emit("mov RCX, 2")
  emit("mov RDX, 1")
  emitExtern("calloc")
  emit("pop RBX") // index
  emit("pop RCX") // string
  emit("add RCX, RBX  ; base+index")
  emit("mov BYTE CL, [RCX]  ; get byte from string index")
  emit("mov BYTE [RAX], CL  ; store one-char string")
}

// atom | atom [ index ] | atom . fieldname
composite: proc: VarType {
  leftType = atom()
  while parser.token.type == TOKEN_LBRACKET or parser.token.type == TOKEN_DOT {
    generateNpeCheck()

    if parser.token.type == TOKEN_LBRACKET {
      // array or string index
      expectToken(TOKEN_LBRACKET, '[')

      if leftType.isArray {
        // Overwrite return type
        leftType = generateArrayIndex(leftType)
      } elif leftType == TYPE_STRING {
        generateStringIndex()
      } else {
        typeError("Cannot take index of " + leftType.name)
        exit
      }
    } elif parser.token.type == TOKEN_DOT {
      // field
      expectToken(TOKEN_DOT, '.')
      if leftType.isRecord {
        if parser.token.type != TOKEN_VARIABLE {
          typeError("Expected field name, but found: " + parser.token.stringValue)
          exit
        }

        // TODO: This must check that the record type exists!
        recType = leftType
        recSym = recType.recordType

        fieldName = parser.token.stringValue

        advanceParser() // eat the field name

        fldSym = lookupField(recSym, fieldName)
        if fldSym == null {
          typeError("Unknown field '" + fieldName + "' of record type " + recSym.name)
          exit
        }

        if fldSym.offset > 0 {
          emitNum("add RAX, ", fldSym.offset)
        }

        type= fldSym.type
        emit("; get record." + fieldName)
        emit("mov " + type.opcodeSize + makeRegister(A_REG, type) + ", [RAX]")

        // Overwrite return type to be *this* field's type
        leftType = fldSym.type
      } else {
        typeError("Cannot take field of " + leftType.name)
        exit
      }
    }
  }
  return leftType
}

generateGetVariable: proc(variable: string): VarType {
  symbol = lookupVariable(variable)
  ref = toReference(symbol)
  reg = makeRegister(A_REG, symbol.varType)
  emit("mov " + symbol.varType.opcodeSize + reg + ", " + ref + "  ; get variable " + variable)
  return symbol.varType
}


ARG_REGISTERS=['RCX', 'RDX', 'R8', 'R9']

generateProcCall: proc(procName: string) {
  procSym = lookupProc(procName)
  expectToken(TOKEN_LPAREN, '(')

  numArgs = 0
  param = procSym.params
  while parser.token.type != TOKEN_RPAREN and parser.token.type != TOKEN_EOF {
    numArgs++
    actualArgType = expr()
    if numArgs > procSym.numParams {
      typeError("Too many args to proc '" + procName + "'")
    }
    expectedType = param.varType

    if not compatibleTypes(expectedType, actualArgType) {
      typeError("Incorrect type for arg '" + param.name + "' to proc '" + procName +
        "'. Expected: " + expectedType.name + ", actual: " + actualArgType.name)
    }
    param = param.next
    if procSym.isExtern and numArgs > 4 {
      generalError("Internal", "too many args to extern " + procSym.name)
      exit
    }
    // NOTE: Always using 8 bytes per arg.
    emit("push RAX")
    if parser.token.type == TOKEN_COMMA {
      advanceParser() // eat the comma
    }
  }
  // TODO: print the actual and expected #s of arguments
  if numArgs < procSym.numParams {
    typeError("Too few args to proc '" + procName + "'")
  }
  if numArgs > procSym.numParams {
    typeError("Too many args to proc '" + procName + "'")
  }

  // reverse the order of the top of the stack
  if numArgs > 1 and not procSym.isExtern {
    emit("; swap args on stack")
    // NOTE: Always using 8 bytes per arg.
    destOffset = (numArgs - 1) * 8
    sourceOffset = 0 i = 0 while i < numArgs do i = i + 2 {
      // swap arg i with arg numArgs - i (hence the i = i + 2)
      emit("mov RBX, [RSP + " + toString(sourceOffset) + "]")
      emit("xchg RBX, [RSP + " + toString(destOffset) + "]")
      emit("mov [RSP + " + toString(sourceOffset) + "], RBX")
      sourceOffset = sourceOffset + 8
      destOffset = destOffset - 8
    }
  }
  if procSym.isExtern {
    // if proc is an extern, put into RCX, RDX, R8, R9 instead
    // of stack-based params
    emit("; get args from stack to registers for extern call")
    i=numArgs-1 while i >= 0 do i-- {
      emit("pop " + ARG_REGISTERS[i])
    }
  }

  expectToken(TOKEN_RPAREN, ')')

  // emit call; the return value will be in RAX, EAX, AL
  if procSym.isExtern {
    emit("extern " + procName)
    emit("call " + procName)
  } else {
    emit("call _" + procName)
  }

  // # of bytes we have to adjust the stack (pseudo-pop)
  bytes = 8 * numArgs
  if bytes > 0 and not procSym.isExtern {
    emit("; adjust stack for pushed params")
    emitNum("add RSP, ", bytes)
  }
}

generateInput: proc {
  emitExtern("_flushall")

  // 1. calloc 1mb
  emit("mov RCX, 1048576 ; allocate 1mb")
  emit("mov RDX, 1")
  emitExtern("calloc")
  emit("push RAX")

  // 3. _read up to 1mb
  emit("xor RCX, RCX     ; 0=stdin")
  emit("mov RDX, RAX     ; destination")
  emit("mov R8, 1048576  ; count (1 mb)")
  emitExtern("_read")

  // TODO: create a smaller buffer with just the right size, then copy to it,
  // then free the original 1mb buffer.
  emit("pop RAX  ; calloc'ed buffer")
}

ARGS_NAME='ARGS'

// atom -> constant | variable | variable '(' params ')' | '(' expr ')' | null | ARGS
atom: proc: VarType {
  type = parser.token.type
  if type == TOKEN_KEYWORD and parser.token.keyword == KW_NULL {
    advanceParser()
    emit("xor RAX, RAX")
    return TYPE_NULL

  } elif type == TOKEN_KEYWORD and parser.token.keyword == KW_ARGS {
    advanceParser() // eat args
    // args is a global
    emit("mov RAX, [_ARGS]")
    argsSym = lookupGlobal(ARGS_NAME)
    if argsSym != null {
      return argsSym.varType
    }
    // TODO: at this point we know we have 'args', so we have to
    // generate the code to set up the global
    argsType = makeArrayType(TYPE_STRING)
    registerGlobal(ARGS_NAME, argsType)
    return argsType

  } elif type == TOKEN_STRING {
    // string constant
    index = addStringConstant(parser.token.stringValue)
    advanceParser()
    emitNum("mov RAX, CONST_", index)
    return TYPE_STRING

  } elif type == TOKEN_INT or type == TOKEN_LONG or type == TOKEN_BYTE {
    constType: VarType
    // TODO: make this a map?
    if type == TOKEN_INT {
      constType = TYPE_INT
    } elif type == TOKEN_LONG {
      constType = TYPE_LONG
    } elif type == TOKEN_BYTE {
      constType = TYPE_BYTE
    }
    // int long or byte constant
    theValue = parser.token.stringValue
    advanceParser()
    if theValue == '0' {
      emit("xor RAX, RAX")
    } else {
      ax = makeRegister(A_REG, constType)
      if type == TOKEN_BYTE {
        theValue = "0x" + theValue
      }
      emit("mov " + ax + ", " + theValue)
    }

    return constType

  } elif type == TOKEN_BOOL {
    // bool constant
    boolval = parser.token.boolValue
    advanceParser()
    if boolval {
      emit("mov AL, 1")
    } else {
      emit("xor AL, AL")
    }
    return TYPE_BOOL

  } elif type == TOKEN_VARIABLE {

    variable = parser.token.stringValue
    advanceParser() // eat the variable
    if parser.token.type != TOKEN_LPAREN {
      varType = generateGetVariable(variable)
      return varType
    }

    // procedure call
    generateProcCall(variable)

    returnType = lookupProc(variable).returnType
    if returnType == TYPE_VOID {
      typeError("Return type of " + variable + " is void. Cannot assign it to a variable.")
      exit
    }

    return returnType

  } elif parser.token.type == TOKEN_LPAREN {

    // (expr)
    expectToken(TOKEN_LPAREN, '(')
    exprType = expr()
    expectToken(TOKEN_RPAREN, ')')
    return exprType

  } elif parser.token.type == TOKEN_KEYWORD and parser.token.keyword == KW_INPUT {

    expectKeyword(KW_INPUT)
    generateInput()
    return TYPE_STRING

  } elif parser.token.type == TOKEN_LBRACKET {
    // array literal
    return parseArrayLiteral()
  }

  parserError("Unexpected token in atom: " + parser.token.stringValue)
  exit
}


///////////////////////////////////////////////////////////////////////////////
//                               STATEMENT RULES                             //
///////////////////////////////////////////////////////////////////////////////

// Parses an arg or field.
parseType: proc: VarType {
  head = findType(parser.token.stringValue)
  if head == null {
    parserError("Unknown type " + parser.token.stringValue)
    exit
  }
  if head == TYPE_VOID or head == TYPE_NULL or head == TYPE_UNKNOWN {
    parserError("Illegal type declaration: " + parser.token.stringValue)
  }
  advanceParser()
  // allows arrays here
  if parser.token.type == TOKEN_LBRACKET {
    expectToken(TOKEN_LBRACKET, '[')
    expectToken(TOKEN_RBRACKET, ']')
    return makeArrayType(head)
  }
  return head
}

// Skips an arg or field.
skipType: proc {
  head = findType(parser.token.stringValue)
  if head == TYPE_VOID or head == TYPE_NULL or head == TYPE_UNKNOWN {
    parserError("Illegal type declaration: " + parser.token.stringValue)
  }
  advanceParser()
  // TODO: allow arrays here
}


parseBaseType: proc: VarType {
  head = types while head != null do head = head.next {
    if head.name == parser.token.stringValue and not head.isArray {
      if head == TYPE_VOID or head == TYPE_NULL or head == TYPE_UNKNOWN {
        parserError("Illegal type for array declaration: " + parser.token.stringValue)
        exit
      }
      advanceParser()
      return head
    }
  }

  parserError("Illegal type for array base type: " + parser.token.stringValue)
  exit
}

// Declaration: type or type[int]
parseDecl: proc(variable: string) {
  type = parseBaseType()
  if parser.token.type != TOKEN_LBRACKET {
    // variable declaration
    if currentProc == null {
      // global
      registerGlobal(variable, type)
    } else {
      registerLocal(variable, type)
    }
    return
  }
  // It's an array declaration
  baseType = type
  expectToken(TOKEN_LBRACKET, '[')

  sizeType = expr()
  if sizeType != TYPE_INT {
    typeError("Array size must be an int, but was " + sizeType.name)
    exit
  }
  expectToken(TOKEN_RBRACKET, ']')

  // allocate "RAX * sizebyType + 5" bytes:
  emit("; allocate array declaration")
  emit("mov EBX, EAX  ; save # of items")
  emit("; bytes per entry")
  emitNum("imul RAX, ", baseType.size)
  emit("add RAX, 5    ; 5 more bytes for dimensions & # entries")
  emit("mov RCX, RAX  ; num items")
  emit("mov RDX, 1")
  emitExtern("calloc")

  // byte 1 is for # of dimensions, byte 2 is # of entries
  emit("; set dimensions & # of entries")
  emit("mov BYTE [RAX], 1")
  emit("mov DWORD [RAX + 1], EBX")

  arrayType = makeArrayType(baseType)
  generateAssignment(variable, arrayType)
}

// Whether we have seen a "return" in this proc.
hasReturn=false

// Procedure declaration
parseProc: proc(procName: string) {
  hasReturn=false

  expectKeyword(KW_PROC)
  if currentProc != null {
    parserError("Cannot define nested procs")
    exit
  }
  setCurrentProc(procName)

  if parser.token.type == TOKEN_LPAREN {
    // if next token is (, advance parser, read parameters until )
    expectToken(TOKEN_LPAREN, '(')

    // Parse params (but not really; they've already been added to the symbol table)
    while parser.token.type != TOKEN_RPAREN {
      expectToken(TOKEN_VARIABLE, 'variable')
      expectToken(TOKEN_COLON, ':')
      parseType()

      if parser.token.type == TOKEN_COMMA {
        advanceParser() // eat the comma
      } else {
        break
      }
    }

    expectToken(TOKEN_RPAREN, ')')
  }

  // if next token is :, read return type
  returnType = TYPE_VOID
  if parser.token.type == TOKEN_COLON {
    advanceParser()  // eat the :
    returnType = parseType()
  }

  afterProc = nextLabel("afterProc")
  emit("; guard around proc")
  emit("jmp " + afterProc)

  // start of proc
  emit("; " + procName + ": proc {")
  emitLabel("_" + procName)
  emit("push RBP")
  emit("mov RBP, RSP")

  emit("; space for locals:")
  localReserveEntry = emit("sub RSP, 96")
  emit("")

  parseBlock()
  if returnType != TYPE_VOID {
    if not hasReturn {
      parserError("No RETURN statement from non-void proc: " + procName)
      exit
    }
  }
  clearCurrentProc()

  // Update the slot with the local reserve
  procSym = lookupProc(procName)
  bytes = localsSize(procSym)
  if bytes > 0 {
    bytes = 16 * (bytes / 16 + 1)
    localReserveEntry.value = "  sub RSP, " + toString(bytes)
  } else {
    localReserveEntry.value = "  ; (no locals)"
  }

  emitLabel("__exit_of_" + procName)
  emit("mov RSP, RBP")
  emit("pop RBP")
  emit("ret")
  emitLabel(afterProc)
}

parseExtern: proc(procName: string) {
  expectKeyword(KW_EXTERN)
  expectKeyword(KW_PROC)
  if currentProc != null {
    parserError("Cannot define nested procs")
    exit
  }
  setCurrentProc(procName)

  if parser.token.type == TOKEN_LPAREN {
    // if next token is (, advance parser, read parameters until )
    expectToken(TOKEN_LPAREN, '(')

    // Parse params (but not really; they've already been added to the symbol table)
    while parser.token.type != TOKEN_RPAREN {
      expectToken(TOKEN_VARIABLE, 'variable')
      expectToken(TOKEN_COLON, ':')
      parseType()

      if parser.token.type == TOKEN_COMMA {
        advanceParser() // eat the comma
      } else {
        break
      }
    }

    expectToken(TOKEN_RPAREN, ')')
  }

  // if next token is :, read return type
  if parser.token.type == TOKEN_COLON {
    advanceParser()  // eat the :
    parseType()
  }

  clearCurrentProc()
}

parseProcSignature: proc(procName: string): ProcSymbol {
  procSymbol = registerProc(procName)
  setCurrentProc(procName)

  if parser.token.type == TOKEN_LPAREN {
    // if next token is (, advance parser, read parameters until )
    expectToken(TOKEN_LPAREN, '(')

    // Parse params
    index = 0
    while parser.token.type != TOKEN_RPAREN {
      if parser.token.type != TOKEN_VARIABLE {
        expectToken(TOKEN_VARIABLE, "variable")
        exit
      }
      paramName = parser.token.stringValue
      advanceParser() // eat the param name

      expectToken(TOKEN_COLON, ':')

      type = parseType()

      // Store the name and type of the parameter
      registerParam(paramName, type)

      if parser.token.type == TOKEN_COMMA {
        advanceParser()
      } else {
        break
      }
    }
    expectToken(TOKEN_RPAREN, ')')
  }

  // if next token is :, read return type
  returnType = TYPE_VOID
  if parser.token.type == TOKEN_COLON {
    advanceParser()  // eat the :
    returnType = parseType()
  }
  procSymbol.returnType = returnType
  clearCurrentProc()
  return procSymbol
}

isAtStartOfExpression: proc: bool {
  if parser.token.type == TOKEN_KEYWORD {
    return
      parser.token.keyword == KW_ASC or
      parser.token.keyword == KW_CHR or
      parser.token.keyword == KW_INPUT or
      parser.token.keyword == KW_LENGTH or
      parser.token.keyword == KW_NEW or
      parser.token.keyword == KW_NOT or
      parser.token.keyword == KW_NULL
  }
  return
    parser.token.type == TOKEN_INT or
    parser.token.type == TOKEN_LONG or
    parser.token.type == TOKEN_BYTE or
    parser.token.type == TOKEN_BOOL or
    parser.token.type == TOKEN_STRING or
    parser.token.type == TOKEN_BIT_NOT or
    parser.token.type == TOKEN_LPAREN or
    parser.token.type == TOKEN_MINUS or
    parser.token.type == TOKEN_PLUS or
    parser.token.type == TOKEN_VARIABLE
}

parseReturn: proc {
  // if we're not in a procedure: error
  if currentProc == null {
    parserError("Cannot return from outside proc")
    exit
  }

  hasReturn=true

  currentProcName = currentProc.name
  actualType = TYPE_VOID
  if isAtStartOfExpression() {
    // if we're at the start of an expression, parse it.
    actualType = expr()
  }

  expectedType = currentProc.returnType

  // Check that return types match
  if not compatibleTypes(actualType, expectedType) {
    typeError("Incorrect return type to '" + currentProcName + "'. Expected "
      + expectedType.name + " but found " + actualType.name)
    exit
  }
  emit("jmp __exit_of_" + currentProcName)
}

generateAssignment: proc(variable: string, exprType: VarType): VarType {
  symbol = lookupGlobal(variable)

  isGlobal = symbol != null or currentProc == null
  if isGlobal {
    if symbol == null {
      symbol = registerGlobal(variable, exprType)
    }
  } else {
    // not global; try param or local
    symbol = lookupParam(variable)
    if symbol == null {
      symbol = lookupLocal(variable)
      if symbol == null {
        symbol = registerLocal(variable, exprType)
      }
    }
  }

  // TODO: make sure type matches existing variable.
  if symbol == null {
    typeError("Cannot generate assignment (!) for " + variable)
    exit
  }

  ref = toReference(symbol)
  reg = makeRegister(A_REG, symbol.varType)
  emit("mov " + symbol.varType.opcodeSize + ref + ", " + reg + "  ; set variable " + variable)
  return symbol.varType
}

generateArraySet: proc(variable: string) {
  varType = lookupType(variable)

  // Make sure 'variable' is an array
  if not varType.isArray {
    typeError("Cannot take index of non-array variable " + variable)
    exit
  }
  expectToken(TOKEN_LBRACKET, '[')
  indexType = expr()
  if indexType != TYPE_INT {
    typeError("Incorrect type for array index. Expected: int, actual: " + indexType.name)
    exit
  }

  // RAX has the index
  emit("push RAX")
  generateGetVariable(variable)
  emit("mov RBX, RAX")
  emit("pop RAX")

  // now RBX has array, RAX has index
  generateArrayIndexInRangeCheck()

  emit("; bytes per entry")
  baseType = varType.baseType
  emitNum("imul RAX, ", baseType.size)
  emit("add RAX, 5  ; header")
  emit("add RBX, RAX")
  emit("push RBX") // destination location
  expectToken(TOKEN_RBRACKET, ']')
  expectToken(TOKEN_EQ, '=')

  rightType = expr()

  checkTypes(baseType, rightType)

  emit("pop RBX")
  emit("; array set:")
  emit("mov " + baseType.opcodeSize + "[RBX], " + makeRegister(A_REG, baseType))
}

registerRecordName: proc(recordName: string) {
  registerRecord(recordName)

  // Skips the rest of the record now that the name is registered.
  expectKeyword(KW_RECORD)
  expectToken(TOKEN_LBRACE, "{")
  while parser.token.type != TOKEN_RBRACE and parser.token.type != TOKEN_EOF {
    expectToken(TOKEN_VARIABLE, 'field')
    expectToken(TOKEN_COLON, ':')
    skipType()
    if parser.token.type == TOKEN_LBRACKET {
      expectToken(TOKEN_LBRACKET, '[')
      expr() // skip the size
      expectToken(TOKEN_RBRACKET, ']')
    }
  }
  expectToken(TOKEN_RBRACE, "}")
}


// Gather all the fields
parseRecordDecl: proc(recordName: string) {
  recSym = lookupRecord(recordName)
  if recSym == null {
    typeError("Record " + recordName + " not found")
    exit
  }
  expectKeyword(KW_RECORD)
  expectToken(TOKEN_LBRACE, "{")

  // zero or more variable declarations, NOT followed by commas
  while parser.token.type != TOKEN_RBRACE and parser.token.type != TOKEN_EOF {
    // record the field names and types
    if parser.token.type != TOKEN_VARIABLE {
      expectToken(TOKEN_VARIABLE, 'variable')
      exit
    }
    fieldName = parser.token.stringValue
    advanceParser() // eat the field name

    expectToken(TOKEN_COLON, ':')

    fieldType = parseType()

    // store the name and type of the field
    registerField(recSym.recordType, fieldName, fieldType)
  }
  expectToken(TOKEN_RBRACE, "}")
}

generateFieldSet: proc(variable: string) {
  generateGetVariable(variable)
  generateNpeCheck()

  emit("push RAX")  // base of record in memory

  expectToken(TOKEN_DOT, '.')
  recType = lookupType(variable)

  if parser.token.type != TOKEN_VARIABLE {
    typeError("Expected field name, but found: " + parser.token.stringValue)
    exit
  }
  fieldName = parser.token.stringValue

  advanceParser() // eat the field name
  expectToken(TOKEN_EQ, "=")

  rhsType = expr()

  recSym = recType.recordType
  if recSym == null {
    typeError("Record type " + recSym.name + " not found")
    exit
  }
  fldSym = lookupField(recSym, fieldName)
  if fldSym == null {
    typeError("Field " + fieldName + " of record type " + recSym.name + " not found")
    exit
  }

  checkTypes(fldSym.type, rhsType)

  emit("pop RBX")
  if fldSym.offset > 0 {
    emitNum("add RBX, ", fldSym.offset)
  }

  emit("; set " + variable + "." + fieldName)
  emit("mov " + fldSym.type.opcodeSize + "[RBX], " + makeRegister(A_REG, fldSym.type))
}


generateIncDec: proc(variable: string, inc: bool) {
  advanceParser() // eat the ++ or --

  op = "dec"
  if inc { op = "inc" }

  symbol = lookupVariable(variable)
  if not symbol.varType.isIntegral {
    typeError("Cannot " + op + "rement variable '" + variable +
        "'; declared as " + symbol.varType.name)
    exit
  }
  ref = toReference(symbol)
  emit(op + symbol.varType.opcodeSize + ref)
}


// Allocates and populates an array literal. Returns the array type.
parseArrayLiteral: proc: VarType {
  expectToken(TOKEN_LBRACKET, '[')

  slotType = TYPE_UNKNOWN
  slotCount = 0
  emit("; collect array literal entries")
  while parser.token.type != TOKEN_RBRACKET and parser.token.type != TOKEN_EOF {
    thisSlotType = expr()
    emit("push RAX")
    slotCount++
    if slotType == TYPE_UNKNOWN {
      slotType = thisSlotType
    } else {
      checkTypes(slotType, thisSlotType)
    }
    if parser.token.type == TOKEN_COMMA {
      expectToken(TOKEN_COMMA, ',')
    } else {
      break
    }
  }
  expectToken(TOKEN_RBRACKET, ']')
  arrayType = makeArrayType(slotType)

  emitNum("mov RCX, ", slotCount * slotType.size + 5)
  emit("mov RDX, 1")
  emitExtern("calloc")
  emit("mov BYTE [RAX], 1")
  emitNum("mov DWORD [RAX + 1], ", slotCount)
  emit0("")

  emit("; populate array literal")
  // Start at the last one
  offset = 5 + slotType.size * (slotCount - 1)
  i = 0 while i < slotCount do i++ {
    emit("pop RBX")
    emit("mov " + slotType.opcodeSize + "[RAX + " + toString(offset) + "], " +
        makeRegister(B_REG, slotType))
    // go to previous one
    offset = offset - slotType.size
  }
  emit0("")
  return arrayType
}


// variable=expression, procname: proc, procname(), arrayname:type[intexpr]
parseStartsWithVariable: proc {
  variable = parser.token.stringValue
  advanceParser()  // eat the variable
  if parser.token.type == TOKEN_EQ {
    advanceParser()  // eat the eq

    exprType = expr()
    varType = generateAssignment(variable, exprType)

    checkTypes(varType, exprType)

    return
  } elif parser.token.type == TOKEN_COLON {
    advanceParser() // eat the colon
    if parser.token.type == TOKEN_KEYWORD and parser.token.keyword == KW_PROC {
      parseProc(variable)
    } elif parser.token.type == TOKEN_KEYWORD and parser.token.keyword == KW_EXTERN {
      parseExtern(variable)
    } elif parser.token.type == TOKEN_KEYWORD and parser.token.keyword == KW_RECORD {
      parseRecordDecl(variable)
    } else {
      parseDecl(variable)
    }
    return
  } elif parser.token.type == TOKEN_LPAREN {
    generateProcCall(variable)
    return
  } elif parser.token.type == TOKEN_LBRACKET {
    // array set
    generateArraySet(variable)
    return
  } elif parser.token.type == TOKEN_DOT {
    generateFieldSet(variable)
    return
  } elif parser.token.type == TOKEN_INC or parser.token.type == TOKEN_DEC {
    generateIncDec(variable, parser.token.type == TOKEN_INC)
    return
  }

  parserError("Expected one of '=' ':' '(' '[' '.' but found: " + parser.token.stringValue)
  exit
}

// expect {, parse statements until }
parseBlock: proc {
  expectToken(TOKEN_LBRACE, '{')
  while parser.token.type != TOKEN_RBRACE and parser.token.type != TOKEN_EOF {
    parseStmt()
    // newline between statements
    emit0("")
  }
  expectToken(TOKEN_RBRACE, '}')
}

parseIf: proc {
  // 1. calculate the condition
  emit("; 'if' condition")
  condType = expr()

  if condType != TYPE_BOOL {
    typeError("Incorrect type of condition in 'if'. Expected: bool, actual: " + condType.name)
    exit
  }

  // 2. if false, go to else
  elseLabel = nextLabel('else')
  emit("cmp AL, 0")
  emit("jz " + elseLabel)

  // 3. { parse statements until }
  emit("; 'if' block")
  parseBlock()

  // this may not be necessary if there are no elses or elifs
  // after the successful "if" block, jump down to the end.
  afterAllLabel = nextLabel('afterIf')
  emit("jmp " + afterAllLabel)

  // 4. elseLabel: while token = elif
  emitLabel(elseLabel)
  while parser.token.type == TOKEN_KEYWORD and parser.token.keyword == KW_ELIF {
    emit("; 'elif' block")
    advanceParser() // eat the elif
    condType = expr()
    if condType != TYPE_BOOL {
      typeError("Incorrect type of condition in 'elif'. Expected: bool, actual: " + condType.name)
      exit
    }

    elseLabel = nextLabel('afterElif')
    emit("cmp AL, 0")
    // if the condition is false, go to our local elseLabel
    emit("jz " + elseLabel)
    parseBlock()
    // after successful "elif" block, jump to the end
    emit("jmp " + afterAllLabel)
    emitLabel(elseLabel)
  }

  // 5. else:
  if parser.token.type == TOKEN_KEYWORD and parser.token.keyword == KW_ELSE {
    emit("; 'else' block")
    advanceParser() // eat the "else"
    parseBlock()
  }

  // 6. afterAll label
  emitLabel(afterAllLabel)
}

// Can't move these to Parser record until records support
// arrays as record fields...
whileBreakLabels:string[100]
whileContinueLabels:string[100]

parseBreak: proc {
  if parser.numWhiles == 0 {
    parserError("Cannot have 'break' outside 'while' loop")
    exit
  }
  emit("jmp " + whileBreakLabels[parser.numWhiles - 1])
}

parseContinue: proc {
  if parser.numWhiles == 0 {
    parserError("Cannot have continue outside loop")
    exit
  }
  emit("jmp " + whileContinueLabels[parser.numWhiles - 1])
}

parseWhile: proc {
  continueLabel = nextLabel("while_continue")
  emitLabel(continueLabel)

  condType = expr()
  if condType != TYPE_BOOL {
    typeError("Incorrect type of condition in 'while'. Expected: bool, actual: " + condType.name)
    exit
  }
  emit("cmp AL, 0")
  afterLabel = nextLabel("after_while")
  emit("jz " + afterLabel)
  // push the "afterLabel" onto the stack
  whileBreakLabels[parser.numWhiles] = afterLabel

  // process 'do'
  doLabel = ""
  hasDo = false
  whileBlockLabel = ""
  if parser.token.type == TOKEN_KEYWORD and parser.token.keyword == KW_DO {
    hasDo = true

    advanceParser() // eat "do"

    // This is a guard so the "do" is only run at the end of the loop.
    // We can't emit it at the end, because we have to emit the block first,
    // so we jump past it here, and back to it after the block.
    whileBlockLabel = nextLabel("while_block")
    emit("jmp " + whileBlockLabel)

    doStmtLabel = nextLabel("do_stmt")
    // do_stmt:
    emitLabel(doStmtLabel)
    // emit do code
    parseStmt()
    //    jmp while_continue
    emit("jmp " + continueLabel)
  }

  // whileBlock:
  emit("; while block")
  // Push either "doStmtLabel" or "continueLabel" onto the "continue stack"
  if hasDo {
    emitLabel(whileBlockLabel)
    // push the "afterLabel" onto the stack
    whileContinueLabels[parser.numWhiles] = doStmtLabel
  } else {
    whileContinueLabels[parser.numWhiles] = continueLabel
  }
  parser.numWhiles = parser.numWhiles + 1

  parseBlock()

  if hasDo {
    // jump back to the "do", which will then jump back to the continuelabel
    emit("jmp " + doStmtLabel)
  }  else {
    emit("jmp " + continueLabel)
  }
  emitLabel(afterLabel)

  // Pop the while label stack
  parser.numWhiles = parser.numWhiles - 1
}

parsePrint: proc(isPrintln: bool) {
  exprType = expr()  // puts result in RAX
  if exprType == TYPE_NULL {
    nullindex = addStringConstant("null")
    emitNum("mov RCX, CONST_", nullindex)
  } elif exprType == TYPE_STRING {

    emit("cmp RAX, 0")
    nullindex = addStringConstant("null")
    emitNum("mov RCX, CONST_", nullindex) // default if null
    emit("cmovnz RCX, RAX") // copy RAX to RCX if not null

  } elif exprType == TYPE_BOOL {
    trueindex = addStringConstant("true")
    falseindex = addStringConstant("false")
    emit("cmp AL, 1")
    emitNum("mov RCX, CONST_", falseindex)
    emitNum("mov RDX, CONST_", trueindex)
    emit("cmovz RCX, RDX")
  } elif exprType == TYPE_INT or exprType == TYPE_BYTE {
    index = addStringConstant("%d")
    emitNum("mov RCX, CONST_", index)
    ax = makeRegister(A_REG, exprType)
    emit("movsx RDX, " + ax)
  } elif exprType == TYPE_LONG {
    index = addStringConstant("%lld")
    emitNum("mov RCX, CONST_", index)
    emit("mov RDX, RAX")
  } else {
    parserError("Cannot generate printing " + exprType.name)
    exit
  }
  emit("sub RSP, 0x20")
  emit("extern printf")
  emit("call printf")
  if isPrintln {
    // char 10=newline
    emit("mov RCX, 10")
    emit("extern putchar")
    emit("call putchar")
  }
  emit("extern _flushall")
  emit("call _flushall")
  emit("add RSP, 0x20")
}

generateExit: proc {
  if isAtStartOfExpression() {
    // if we're at the start of an expression, parse it.
    exitExprType = expr()
    if exitExprType != TYPE_STRING {
      typeError("Incorrect type of 'exit' expression. Expected: string, actual: " + exitExprType.name)
      exit
    }
    messageIndex = addStringConstant("ERROR: %s\n")
    emitNum("mov RCX, CONST_", messageIndex)
    emit("mov RDX, RAX")
    emit("sub RSP, 0x20")
    emit("extern printf")
    emit("call printf")
    emit("extern _flushall")
    emit("call _flushall")
    emit("add RSP, 0x20\n")
  }

  emit("mov RCX, -1")
  emit("call exit")
}

parseStmt: proc {
  if parser.token.type == TOKEN_EOF {
    return
  } elif parser.token.type == TOKEN_KEYWORD {
    kw = parser.token.keyword
    advanceParser() // eat the token
    if kw == KW_PRINT or kw==KW_PRINTLN {
      parsePrint(kw==KW_PRINTLN)
      return
    } elif kw == KW_EXIT {
      generateExit()
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
  } elif parser.token.type == TOKEN_VARIABLE {
    parseStartsWithVariable()
    return
  }

  parserError("Cannot parse start of statement. Found: " + parser.token.stringValue)
  exit
}

emitArgsSetup: proc {
  emitLabel("__args_setup")
  emit("; convert argc, argv to ARGS global array")
  emit("mov DWORD [RSP + 16], ECX")
  emit("mov QWORD [RSP + 24], RDX")
  emitExtern("__main")

  // argc is now in [rsp+16] and argv start at [[rsp+24]]
  // 1. allocate an array of size 8*argc + 5
  emit("mov edx, DWORD [RSP + 16]  ; edx = argc")
  emit("imul edx, 8  ; 8 bytes per string")
  emit("add edx, 5  ; 1 byte for dimensions, 4 for size")
  emit("mov RCX, 1  ; # of 'entries' for calloc")
  emitExtern("calloc")
  // 2. put it at ARGS
  emit("mov [_ARGS], RAX")
  // 3. set ARGS[0] to 1, ARGS[1] to argc
  emit("mov BYTE [RAX], 1  ; # dimensions ")
  // edx may have been destroyed by calloc.
  emit("mov r8d, DWORD [RSP + 16]  ; r8d = argc")
  emit("mov DWORD [RAX + 1], r8d  ; argc")
  // 4. copy argv to ARGS[5]
  emit("; copy argv to the ARGS array")
  // we need to copy argc*8 bytes from argv to args+5
  emit("mov rcx, [_ARGS]  ; dest")
  emit("add rcx, 5  ; args+5")
  emit("mov rdx, [rsp+24] ; location of first entry")
  emit("imul r8d, 8  ; 8 bytes per string")
  emitExtern("memcpy")
  emit("jmp __args_setup_done")
}

///////////////////////////////////////////////////////////////////////////////
//                          MAIN LOOP & OUTPUT ROUTINES                      //
///////////////////////////////////////////////////////////////////////////////

emitStringTable: proc {
  head = stringTable.head
  while head != null do head = head.next {
    print "  CONST_" print head.index print ': db "'
    entry = head.value
    j = 0 while j < length(entry) do j++ {
      ch = asc(entry[j])
      // Note don't have to escape ' (39) because we're using double quotes
      if ch < 32 or ch==34 or ch==37 or ch==92 {
        // unprintable characters (\n, \r, \t, ", %) become ints
        print '", ' print ch print ', "'
      } else {
        print entry[j]
      }
    }
    println '", 0'
  }
}

emitGlobalTable: proc {
  head = globals while head != null do head = head.next {
    print "  _" print head.name print ": "
    print head.varType.dataSize println " 0"
  }
}

parseProgram: proc(self: Parser) {
  emit0("; compiled by " + VERSION)
  emit0("global main")
  emit0("extern exit\n")
  emit0("section .text")
  emit0("main:")

  entry = emit("")
  entryLabel = emit("")
  while self.token.type != TOKEN_EOF {
    parseStmt()
    // newline between statements
    emit0("")
  }
  emit("xor RCX, RCX")
  emit("call exit\n")

  if self.npeCheckNeeded {
    emitNpeCheckProc()
  }
  if self.indexPositiveCheckNeeded {
    emitIndexPositiveCheckProc()
  }
  // if ARGS was added,  ???
  argsSym = lookupGlobal(ARGS_NAME)
  if argsSym != null {
    emitArgsSetup()
    entry.value = "  jmp __args_setup"
    entryLabel.value = "__args_setup_done:"
  }
  emitter_printEntries(self.emitter)

  if stringTable.head != null or globals != null {
    println "section .data"
  }
  if stringTable.head != null {
    emitStringTable()
  }
  emitGlobalTable()
}

procFinder: proc(self: Parser) {
  while self.token.type != TOKEN_EOF {
    if self.token.type == TOKEN_VARIABLE {
      variable = self.token.stringValue
      advanceParser() // eat the variable
      if self.token.type == TOKEN_COLON {
        advanceParser() // eat the colon
        if self.token.type == TOKEN_KEYWORD and
          (self.token.keyword == KW_PROC or self.token.keyword == KW_EXTERN) {

          isExtern = self.token.keyword == KW_EXTERN
          if isExtern {
            expectKeyword(KW_EXTERN)
          }
          expectKeyword(KW_PROC)
          sym = parseProcSignature(variable)
          sym.isExtern = isExtern
        }
        continue
      }
      continue
    }
    advanceParser()
  }
  resetLexer(self.lexer)
  advanceParser()
}

recordFinder: proc(self: Parser) {
  while self.token.type != TOKEN_EOF {
    if self.token.type == TOKEN_VARIABLE {
      variable = self.token.stringValue
      advanceParser() // eat the variable
      if self.token.type == TOKEN_COLON {
        advanceParser() // eat the colon
        if self.token.type == TOKEN_KEYWORD and self.token.keyword == KW_RECORD {
          registerRecordName(variable)
        }
        continue
      }
      continue
    }
    advanceParser()
  }
  resetLexer(self.lexer)
  advanceParser()
}


initParser: proc: Parser {
  parser = new Parser
  text = input
  parser.lexer = newLexer(text)
  parser.emitter = new Emitter
  advanceParser()
  return parser
}

// All these need to be at the end of the file, so that
// various globlas above here are set up top-to-bottom.
recordFinder(parser)
procFinder(parser)
parseProgram(parser)


// I don't love this, that it uses the global "parser"
generalError: proc(type: string, message: string) {
  emitter_printEntries(parser.emitter)
  lineBasedError(type, message, parser.lexer.line)
}

typeError: proc(message: string) {
  generalError("Type", message)
}

parserError: proc(message: string) {
  generalError("Parse", message)
}

