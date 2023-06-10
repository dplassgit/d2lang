///////////////////////////////////////////////////////////////////////////////
//                                    PARSER                                 //
//                           VERSION 3 (COMPILED BY V2)                      //
///////////////////////////////////////////////////////////////////////////////

lexer=initParser() // global, temporarily.

advanceParser: proc {
  nextToken(lexer)
  if debug {
    //println "; new token is " + lexTokenString
  }
}

expectToken: proc(expectedTokenType: int, tokenStr: string) {
  if lexTokenType != expectedTokenType  {
    parserError("Unexpected token. Expected: " + tokenStr + ", actual: '" + lexTokenString + "'")
    exit
  }
  advanceParser() // eat the expected token
}

expectKeyword: proc(expectedKwType: int) {
  if lexTokenType != TOKEN_KEYWORD or lexTokenKw != expectedKwType {
    parserError("Unexpected token. Expected: " + KEYWORDS[expectedKwType] + ", actual: '" + lexTokenString + "'")
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
  print "\n" print label println ":"
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
  println "  extern " + name
  println "  sub RSP, 0x20"
  println "  call " + name
  println "  add RSP, 0x20\n"
}

npeCheckNeeded = false

emitNpeCheck: proc {
  afterProc = nextLabel("after_npe")
  println "  jmp " + afterProc
  println "\n__npe_check_proc:"
  println "  push RBP"
  println "  mov RBP, RSP"

  println "  cmp RAX, 0"
  okLabel = nextLabel("not_null")
  print "  jne " println okLabel

  npeMessageIndex = addStringConstant("\nNull pointer error at line %d.\n")
  print "  mov RCX, CONST_" println npeMessageIndex
  println "  mov RDX, [RBP + 16]  ; line #"
  println "  sub RSP, 0x20"
  println "  extern printf"
  println "  call printf"
  println "  extern _flushall"
  println "  call _flushall"
  println "  add RSP, 0x20"
  println "  mov RCX, -1"
  println "  extern exit"
  println "  call exit"

  emitLabel(okLabel)
  println "  mov RSP, RBP"
  println "  pop RBP"
  println "  ret"

  emitLabel(afterProc)
}

generateNpeCheck: proc {
  npeCheckNeeded = true

  println ""
  print   "  mov RCX, " println lexer.line
  println "  push RCX"
  println "  call __npe_check_proc"
  println "  add RSP, 8\n"
}

indexPositiveCheckNeeded = false

emitIndexPositiveCheck: proc {
  afterProc = nextLabel("after_index_check")
  println "  jmp " + afterProc
  println "\n__index_positive_check:"
  println "  push RBP"
  println "  mov RBP, RSP"

  okLabel = nextLabel("index_ge0")
  println "  cmp EAX, 0"
  print "  jge " println okLabel

  errorindex = addStringConstant(
    "\nInvalid index error at line %d: must not be negative; was %d\n")
  print "  mov RCX, CONST_" println errorindex
  println "  mov RDX, [RBP + 16]  ; line #"
  println "  mov R8D, EAX" // actual index
  println "  sub RSP, 0x20"
  println "  extern printf"
  println "  call printf"
  println "  extern _flushall"
  println "  call _flushall"
  println "  add RSP, 0x20"
  println "  mov RCX, -1"
  println "  extern exit"
  println "  call exit"

  emitLabel(okLabel)
  println "  mov RSP, RBP"
  println "  pop RBP"
  println "  ret"

  emitLabel(afterProc)
}

// Makes sure RAX is >= 0. 'type' is either STRING or ARRAY
generateIndexPositiveCheck: proc {
  indexPositiveCheckNeeded = true

  println ""
  print   "  mov RCX, " println lexer.line
  println "  push RCX"
  println "  call __index_positive_check"
  println "  add RSP, 8\n"
}

// RBX has the array location
// RAX has the index.
// If EAX is < 0 or > the length, shows an error.
// Makes sure EAX is < the length of the array.
// TODO: Make this a subroutine
generateArrayIndexInRangeCheck: proc {

  generateIndexPositiveCheck()

  inRangeLabel = nextLabel("index_inRange")
  println "  mov DWORD R8d, [RBX + 1]" // 1 for dimension
  println "  cmp EAX, R8d"
  println "  jl " + inRangeLabel

  errorindex = addStringConstant(
      "\nInvalid index error at line %d: ARRAY index out of bounds (length %d); was %d\n")
  print "  mov RCX, CONST_" println errorindex
  print "  mov RDX, " println lexer.line
  println "  mov R9d, EAX" // actual index
  println "  sub RSP, 0x20"
  println "  extern printf"
  println "  call printf"
  println "  extern _flushall"
  println "  call _flushall"
  println "  add RSP, 0x20"
  println "  mov RCX, -1"
  println "  extern exit"
  println "  call exit"

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
// atom -> int constant, bool constant, string constant, variable, '(' expr ')', 'input'


// Each of these returns the type of the expression
expr: proc: VarType {
  return boolOr()
}

boolOr: proc: VarType {
  leftType = boolAnd()
  // TODO: int/TOKEN_OR
  if lexTokenType == TOKEN_KEYWORD and leftType == TYPE_BOOL {
    while lexTokenKw == KW_OR {
      advanceParser() // eat the symbol
      println "  push RAX"

      rightType = boolAnd()
      checkTypes(leftType, rightType)

      println "  pop RBX" // pop the left side
      // left = left (bool) OR right
      println "  or BL, AL"
      println "  mov AL, BL"
    }
  }
  return leftType
}

boolAnd: proc: VarType {
  leftType = compare()
  // TODO: int/TOKEN_AND
  if lexTokenType == TOKEN_KEYWORD and leftType == TYPE_BOOL {
    while lexTokenKw == KW_AND {
      advanceParser() // eat the symbol
      println "  push RAX"

      rightType = compare()
      checkTypes(leftType, rightType)

      println "  pop RBX" // pop the left side
      // left = left and right
      println "  and BL, AL"
      println "  mov AL, BL"
    }
  }
  return leftType
}

compare: proc: VarType {
  leftType = addSub()
  if leftType == TYPE_INT and (lexTokenType >= TOKEN_EQEQ and lexTokenType <= TOKEN_GEQ) {
    op = lexTokenType
    opstring = lexTokenString
    advanceParser() // eat the symbol
    println "  push RAX"

    rightType = addSub()
    checkTypes(leftType, rightType)

    println "  pop RBX" // pop the left side

    // left = left (op) right
    print "  cmp EBX, EAX  ; int " print opstring println " int"
    print "  " print OPCODES[op] println " AL"
    return TYPE_BOOL
  } elif leftType == TYPE_STRING and (lexTokenType >= TOKEN_EQEQ and lexTokenType <= TOKEN_GEQ) {

    op = lexTokenType

    advanceParser() // eat the symbol
    println "  push RAX"

    rightType = addSub()
    checkTypes(leftType, rightType)

    println "  mov RDX, RAX" // right side
    println "  pop RCX" // left side

    // TODO: this can NPE
    emitExtern("strcmp")
    println "  cmp RAX, 0"
    print "  " print OPCODES[op] println " AL"
    return TYPE_BOOL

  } elif leftType == TYPE_BOOL and (lexTokenType >= TOKEN_EQEQ and lexTokenType <= TOKEN_GEQ) {
    op = lexTokenType
    opstring = lexTokenString
    advanceParser() // eat the symbol
    println "  push RAX"

    rightType = addSub()
    checkTypes(leftType, rightType)

    println "  pop RBX" // pop the left side
    print "  cmp BL, AL  ; bool " print opstring println " bool"
    print "  " print OPCODES[op] println " AL"
    return TYPE_BOOL
  } elif (leftType.isRecord or leftType == TYPE_NULL) and
         (lexTokenType == TOKEN_EQEQ or lexTokenType == TOKEN_NEQ) {
    op = lexTokenType
    opstring = lexTokenString
    advanceParser() // eat the symbol
    println "  push RAX"

    rightType = addSub()
    checkTypes(leftType, rightType)

    println "  pop RBX" // pop the left side
    // left = left (op) right
    print "  cmp RBX, RAX  ; record " print opstring println " record"
    print "  " print OPCODES[op] println " AL"
    return TYPE_BOOL
  }
  return leftType
}

addSub: proc: VarType {
  leftType = mulDiv()
  while lexTokenType == TOKEN_PLUS or lexTokenType == TOKEN_MINUS {
    op = lexTokenType
    opstring = lexTokenString
    advanceParser() // eat the symbol
    println "  push RAX"

    rightType = mulDiv()
    checkTypes(leftType, rightType)

    println "  pop RBX" // pop the left side
    if leftType == TYPE_BOOL {
      typeError("Cannot add or subtract booleans")
      exit
    }
    if leftType == TYPE_STRING {
      // 1. mov rsi, rax. get length of rsi
      println "  mov RSI, RAX  ; lhs in RSI"
      println "  mov RCX, RSI"
      emitExtern("strlen")
      println "  mov RDI, RAX  ; RHS length in RDI"
      // 2. get length of rbx
      println "  mov RCX, RBX"
      emitExtern("strlen")
      println "  mov RCX, RAX  ; LHS length in RCX"
      // 3. add them
      println "  add RCX, RDI  ; total length in RCX"
      println "  inc RCX       ; plus one byte for null"
      // 4. allocate new string of new size, result in rax
      println "   ; new string location in RAX"
      emitExtern("malloc")
      // 5. strcpy rbx to new location
      println "  push RAX      ; save new location for later"
      println "  mov RCX, RAX  ; dest (new location)"
      println "  mov RDX, RBX  ; source"
      println "  ; copy LHS to new location"
      emitExtern("strcpy")
      // 6. strcat rsi to rax
      println "  pop RCX       ; get new location back into RCX as dest"
      println "  mov RDX, RSI  ; source"
      println "  ; concatenate RHS at new location"
      emitExtern("strcat")
      print "\n"
      continue
    }
    if leftType == TYPE_STRING and lexTokenType == TOKEN_MINUS {
      typeError("Cannot subtract strings")
      exit
    }

    if leftType == TYPE_INT {
      // TODO: This is too big for ints
      // left = left (op) right
      // TODO: If plus, can just do add rax, rbx instead of two lines
      print "  " print OPCODES[op] print " RBX, RAX  ; int " print opstring println " int"
    }
    println "  mov RAX, RBX"
  }
  return leftType
}

mulDiv: proc: VarType {
  leftType = unary()
  while leftType == TYPE_INT and
      (lexTokenType == TOKEN_MULT or lexTokenType == TOKEN_DIV or lexTokenType == TOKEN_MOD) {
    op = lexTokenType
    advanceParser() // eat the symbol
    println "  push RAX"

    rightType = unary()
    checkTypes(leftType, rightType)

    println "  pop RBX" // pop the left side

    // rax = rax (op) rbx
    if op == TOKEN_DIV or op == TOKEN_MOD {
      println "  xchg RAX, RBX  ; put numerator in RAX, denominator in EBX"
      // TODO: This is too big for ints
      println "  cqo  ; sign extend rax to rdx"
      println "  idiv RBX  ; rax=rax/rbx"
      if op == TOKEN_MOD {
        // modulo is in rdx
        println "  mov RAX, RDX  ; modulo is in rdx"
      }
    } else {
      // TODO: This is too big for ints
      println "  imul RAX, RBX  ; int * int"
    }
  }
  return leftType
}

// -unary | +unary | length(expr) | asc(expr) | chr(expr) | not expr
unary: proc: VarType {
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
      // TODO: This is too big for ints
      println "  neg RAX  ; unary minus"
      return type
    }
    typeError("Cannot codegen negative non-ints")
    exit
  } elif lexTokenType == TOKEN_KEYWORD and lexTokenKw == KW_LENGTH {
    advanceParser() // eat the length
    expectToken(TOKEN_LPAREN, '(')
    varType = expr()
    expectToken(TOKEN_RPAREN, ')')
    if varType == TYPE_STRING {
      generateNpeCheck()

      println "  mov RCX, RAX"
      emitExtern("strlen")
    } elif varType.isArray {
      // RAX has location of array
      println "  inc RAX               ; skip past # of dimensions"
      println "  mov DWORD EAX, [RAX]  ; get length (4 bytes only)" // Fun fact: the upper 32 bits are zero-extended
    }
    else {
      typeError("Cannot take LENGTH of " + type.name)
      exit
    }
    return TYPE_INT
  } elif lexTokenType == TOKEN_KEYWORD and lexTokenKw == KW_ASC {
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
    println "  mov BYTE AL, [RAX]"
    // clear out the high bytes
    println "  and RAX, 255"
    return TYPE_INT

  } elif lexTokenType == TOKEN_KEYWORD and lexTokenKw == KW_CHR {
    advanceParser() // eat the chr

    expectToken(TOKEN_LPAREN, '(')
    type = expr()
    println "  push RAX" // save the int to be made into a string
    expectToken(TOKEN_RPAREN, ')')

    if type != TYPE_INT {
      typeError("Cannot take CHR of " + type.name)
      exit
    }

    // allocate a 2-byte string
    println "  mov RCX, 2"
    println "  mov RDX, 1"
    emitExtern("calloc")
    println "  pop RBX"
    println "  mov BYTE [RAX], BL  ; store byte"
    return TYPE_STRING
  } elif lexTokenType == TOKEN_KEYWORD and lexTokenKw == KW_NOT {
    advanceParser() // eat the NOT

    type = expr()

    if type != TYPE_BOOL {
      typeError("Cannot apply NOT to " + type.name)
      exit
    }

    println "  xor AL, 0x01  ; NOT"
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
    recVarType = lookupRecord(recordName)
    if recVarType == null {
      typeError("Record '" + recordName + "' not defined")
      exit
    }
    size = recVarType.recordType.size
    print "  mov RCX, " print size print " ; size of record type " println recordName
    println "  mov RDX, 1"
    emitExtern("calloc")
    return recVarType
  }

  return composite()
}


// Generate a "get" of foo[int]
// returns the base array type
generateArrayIndex: proc(arrayType: VarType): VarType {
  baseType = arrayType.baseType // VarType

  println "  push RAX  ; save array base location"
  indexType = expr()
  if indexType != TYPE_INT {
    typeError("Array index must be int; was " + indexType.name)
    exit
  }
  expectToken(TOKEN_RBRACKET, ']')

  println "  pop RBX  ; base location"
  // RBX has location, RAX has index
  generateArrayIndexInRangeCheck()

  // 1. multiply index by size
  print "  imul RAX, " print baseType.size println "  ; bytes per entry (temporarily 8)"
  // 2. add 5
  println "  add RAX, 5  ; skip header"
  // 3. add to base
  println "  add RAX, RBX  ; add to base location"
  // 4. get value
  // TODO: This is too big for ints or bools
  println "  mov RAX, [RAX]  ; get array slot value"

  return baseType
}

// Generate a "get" of foo[int]
generateStringIndex: proc {
  println "  push RAX  ; string location"
  indexType = expr()
  if indexType != TYPE_INT {
    typeError("String index must be int; was " + indexType.name)
    exit
  }
  expectToken(TOKEN_RBRACKET, ']')
  println "  push RAX"

  generateIndexPositiveCheck()

  // Check that index < string length

  println "  mov RBX, RAX" // save index
  println "  mov RCX, [RSP+8]" // string
  emitExtern("strlen") // rax=strlen
  println"  cmp RBX, RAX"
  goodIndexLabel = nextLabel("good_index")
  println"  jl " + goodIndexLabel

  // out of range exception
  error = addStringConstant(
      "\nInvalid index error at line %d: out of bounds (length %d); was %d\n")
  print "  mov RCX, CONST_" println error
  print "  mov RDX, " println lexer.line
  println "  mov R8d, EAX" // length
  println "  mov R9d, EBX" // index
  println "  sub RSP, 0x20"
  println "  extern printf"
  println "  call printf"
  println "  extern _flushall"
  println "  call _flushall"
  println "  add RSP, 0x20"
  println "  mov RCX, -1"
  println "  extern exit"
  println "  call exit"

  // continue:
  emitLabel(goodIndexLabel)
  // allocate a 2-byte string
  println "  mov RCX, 2"
  println "  mov RDX, 1"
  emitExtern("calloc")
  println "  pop RBX" // index
  println "  pop RCX" // string
  println "  add RCX, RBX  ; base+index"
  println "  mov BYTE CL, [RCX]  ; get byte from string index"
  println "  mov BYTE [RAX], CL  ; store one-char string"
}

// atom | atom [ index ] | atom . fieldname
composite: proc: VarType {
  leftType = atom()
  while lexTokenType == TOKEN_LBRACKET or lexTokenType == TOKEN_DOT {
    generateNpeCheck()

    if lexTokenType == TOKEN_LBRACKET {
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
    } elif lexTokenType == TOKEN_DOT {
      // field
      expectToken(TOKEN_DOT, '.')
      if leftType.isRecord {
        if lexTokenType != TOKEN_VARIABLE {
          typeError("Expected field name, but found: " + lexTokenString)
          exit
        }

        // TODO: This must check that the record type exists!
        recType = leftType
        recSym = recType.recordType

        fieldName = lexTokenString

        advanceParser() // eat the field name

        fldSym = lookupField(recSym, fieldName)
        if fldSym == null {
          typeError("Unknown field '" + fieldName + "' of record type " + recSym.name)
          exit
        }

        if fldSym.offset > 0 {
          print "  add RAX, " println fldSym.offset
        }

        // TODO: this is too big for ints
        print "  mov RAX, [RAX]  ; get record." println fieldName

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
  // TODO: this is too big for ints and bools
  print "  mov RAX, " println ref
  return symbol.varType
}


REGISTERS:string[4]
REGISTERS[0]='RCX'
REGISTERS[1]='RDX'
REGISTERS[2]='R8'
REGISTERS[3]='R9'

generateProcCall: proc(procName: string) {
  procSym = lookupProc(procName)
  expectToken(TOKEN_LPAREN, '(')

  numArgs = 0
  param = procSym.params
  while lexTokenType != TOKEN_RPAREN and lexTokenType != TOKEN_EOF {
    numArgs = numArgs + 1
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
    if procSym.isExtern {
      if numArgs > 4 {
        generalError("Internal", "too many args to extern " + procSym.name)
        exit
      }
      // if proc is an xtern, put into RCX, RDX, R8, R9 instead
      // of stack-based params
      print "  mov " + REGISTERS[numArgs-1] + ", RAX\n"
    } else {
      // NOTE: Always using 8 bytes per arg.
      println "  push RAX"
    }
    if lexTokenType == TOKEN_COMMA {
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
    println "  ; swap args on stack"
    // NOTE: Always using 8 bytes per arg.
    destOffset = (numArgs - 1) * 8
    sourceOffset = 0 i = 0 while i < numArgs do i = i + 2 {
      // swap arg i with arg numArgs - i
      print "  mov RBX, [RSP + " print sourceOffset println "]"
      print "  xchg RBX, [RSP + " print destOffset println "]"
      print "  mov [RSP + " print sourceOffset println "], RBX"
      sourceOffset = sourceOffset + 8
      destOffset = destOffset - 8
    }
  }

  expectToken(TOKEN_RPAREN, ')')

  // emit call; the return value will be in RAX, EAX, AL
  if procSym.isExtern {
    println "extern " + procName
    println "  call " + procName
  } else {
    println "  call _" + procName
  }

  // # of bytes we have to adjust the stack (pseudo-pop)
  bytes = 8 * numArgs
  if bytes > 0 and not procSym.isExtern {
    print "  add RSP, " print bytes println "  ; adjust stack for pushed params\n"
  }
}

generateInput: proc {
  emitExtern("_flushall")

  // 1. calloc 1mb
  println "  mov RCX, 1048576 ; allocate 1mb"
  println "  mov RDX, 1"
  emitExtern("calloc")
  println "  push RAX"

  // 3. _read up to 1mb
  println "  xor RCX, RCX     ; 0=stdin"
  println "  mov RDX, RAX     ; destination"
  println "  mov R8, 1048576  ; count (1 mb)"
  emitExtern("_read")

  // TODO: create a smaller buffer with just the right size, then copy to it,
  // then free the original 1mb buffer.
  println "  pop RAX  ; calloc'ed buffer"
}

// atom -> constant | variable | variable '(' args ')' | '(' expr ')' | null
atom: proc: VarType {
  if lexTokenType == TOKEN_KEYWORD and lexTokenKw == KW_NULL {
    advanceParser()
    println "  xor RAX, RAX"
    return TYPE_NULL
  } elif lexTokenType == TOKEN_STRING {
    // string constant
    index = addStringConstant(lexTokenString)
    advanceParser()
    print "  mov RAX, CONST_" println index
    return TYPE_STRING

  } elif lexTokenType == TOKEN_INT {
    // int constant
    intval = lexTokenInt
    advanceParser()
    // TODO: This is too big for ints
    if intval != 0 {
      print "  mov RAX, " println intval
    } else {
      println "  xor RAX, RAX"
    }
    return TYPE_INT

  } elif lexTokenType == TOKEN_BOOL {
    // bool constant
    boolval = lexTokenBool
    advanceParser()
    if boolval {
      println "  mov AL, 1"
    } else {
      println "  xor RAX, RAX"
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

    returnType = lookupProc(variable).returnType
    if returnType == TYPE_VOID {
      typeError("Return type of " + variable + " is void. Cannot assign it to a variable.")
      exit
    }

    return returnType

  } elif lexTokenType == TOKEN_LPAREN {

    // (expr)
    expectToken(TOKEN_LPAREN, '(')
    exprType = expr()
    expectToken(TOKEN_RPAREN, ')')
    return exprType

  } elif lexTokenType == TOKEN_KEYWORD and lexTokenKw == KW_INPUT {

    expectKeyword(KW_INPUT)
    generateInput()
    return TYPE_STRING

  } elif lexTokenType == TOKEN_LBRACKET {
    // array literal
    return parseArrayLiteral()
  }

  parserError("Unexpected token: " + lexTokenString)
  exit
}


///////////////////////////////////////////////////////////////////////////////
//                               STATEMENT RULES                             //
///////////////////////////////////////////////////////////////////////////////

// Parses an arg or field.
parseType: proc: VarType {
  head = types while head != null do head = head.next {
    if head.name == lexTokenString {
      if head == TYPE_VOID or head == TYPE_NULL or head == TYPE_UNKNOWN {
        parserError("Illegal type for array declaration: " + lexTokenString)
      }
      advanceParser()
      // allow arrays here
      if lexTokenType == TOKEN_LBRACKET {
        expectToken(TOKEN_LBRACKET, '[')
        expectToken(TOKEN_RBRACKET, ']')
        return makeArrayType(head)
      }
      return head
    }
  }

  parserError("Unknown type " + lexTokenString)
  exit
}

// Skips an arg or field.
skipType: proc {
  head = types while head != null do head = head.next {
    if head.name == lexTokenString {
      if head == TYPE_VOID or head == TYPE_NULL or head == TYPE_UNKNOWN {
        parserError("Illegal type for skipped array declaration: " + lexTokenString)
      }
      advanceParser()
      return
    }
  }

  // Not all records may be defined yet
  expectToken(TOKEN_VARIABLE, 'record type')
  // TODO: allow arrays here
}


parseBaseType: proc: VarType {
  head = types while head != null do head = head.next {
    if head.name == lexTokenString and not head.isArray {
      if head == TYPE_VOID or head == TYPE_NULL or head == TYPE_UNKNOWN {
        parserError("Illegal type for array declaration: " + lexTokenString)
        exit
      }
      advanceParser()
      return head
    }
  }

  parserError("Illegal type for array base type: " + lexTokenString)
  exit
}

// Array declaration: type[int]
parseArrayDecl: proc(variable: string) {
  baseType = parseBaseType()
  expectToken(TOKEN_LBRACKET, '[')

  sizeType = expr()
  if sizeType != TYPE_INT {
    typeError("Array size must be an int, but was " + sizeType.name)
    exit
  }
  expectToken(TOKEN_RBRACKET, ']')

  // allocate "RAX * sizebyType + 5" bytes:
  println "  ; allocate array declaration"
  println "  mov EBX, EAX  ; save # of items"
  print "  imul RAX, " print baseType.size println "  ; bytes per entry"
  println "  add RAX, 5    ; 5 more bytes for dimensions & # entries"
  println "  mov RCX, RAX  ; num items"
  println "  mov RDX, 1"
  emitExtern("calloc")

  // byte 1 is for # of dimensions, byte 2 is # of entries
  println "  ; set dimensions & # of entries"
  println "  mov BYTE [RAX], 1"
  println "  mov DWORD [RAX + 1], EBX"

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
  print "\n  jmp " print afterProc println "; guard around proc"

  // start of proc
  print "\n  ; " print procName println ": proc {"
  emitLabel("_" + procName)
  println "  push RBP"
  println "  mov RBP, RSP"
  // int bytes = 16 * (op.localBytes() / 16 + 1);
  // assume localbytes = 10*8=80, bytes = 96
  println "  sub RSP, 96  ; space for up to 10 8-byte locals\n"

  parseBlock()
  if returnType != TYPE_VOID {
    if not hasReturn {
      parserError("No RETURN statement from non-void proc: " + procName)
      exit
    }
  }
  clearCurrentProc()

  print "__exit_of_" print procName println ":"
  println "  mov RSP, RBP"
  println "  pop RBP"
  println "  ret"
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
  if lexTokenType == TOKEN_COLON {
    advanceParser()  // eat the :
    parseType()
  }

  clearCurrentProc()
}

parseProcSignature: proc(procName: string): ProcSymbol {
  procSymbol = registerProc(procName)
  setCurrentProc(procName)

  if lexTokenType == TOKEN_LPAREN {
    // if next token is (, advance parser, read parameters until )
    expectToken(TOKEN_LPAREN, '(')

    // Parse params
    index = 0
    while lexTokenType != TOKEN_RPAREN {
      if lexTokenType != TOKEN_VARIABLE {
        expectToken(TOKEN_VARIABLE, "variable")
        exit
      }
      paramName = lexTokenString
      advanceParser() // eat the param name

      expectToken(TOKEN_COLON, ':')

      type = parseType()

      // Store the name and type of the parameter
      registerParam(paramName, type)

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
  procSymbol.returnType = returnType
  clearCurrentProc()
  return procSymbol
}

isAtStartOfExpression: proc: bool {
  if lexTokenType == TOKEN_KEYWORD {
    return
      lexTokenKw == KW_ASC or
      lexTokenKw == KW_CHR or
      lexTokenKw == KW_INPUT or
      lexTokenKw == KW_LENGTH or
      lexTokenKw == KW_NEW or
      lexTokenKw == KW_NOT or
      lexTokenKw == KW_NULL
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
  print "  jmp __exit_of_" println currentProcName
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
  // TODO: This is too big for ints or bools
  print "  mov " print ref println ", RAX"
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
  println "  push RAX"
  generateGetVariable(variable)
  println "  mov RBX, RAX"
  println "  pop RAX"

  // now RBX has array, RAX has index
  generateArrayIndexInRangeCheck()

  print "  imul RAX, " print varType.baseType.size println "  ; bytes per entry"
  println "  add RAX, 5  ; header"
  println "  add RBX, RAX"
  println "  push RBX" // destination location
  expectToken(TOKEN_RBRACKET, ']')
  expectToken(TOKEN_EQ, '=')

  rightType = expr()

  checkTypes(varType.baseType, rightType)

  println "  pop RBX"
  println "  mov [RBX], RAX  ; array set"
}

registerRecordName: proc(recordName: string) {
  registerRecord(recordName)

  // Skips the rest of the record now that the name is registered.
  expectKeyword(KW_RECORD)
  expectToken(TOKEN_LBRACE, "{")
  while lexTokenType != TOKEN_RBRACE and lexTokenType != TOKEN_EOF {
    expectToken(TOKEN_VARIABLE, 'field')
    expectToken(TOKEN_COLON, ':')
    skipType()
    if lexTokenType == TOKEN_LBRACKET {
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
  while lexTokenType != TOKEN_RBRACE and lexTokenType != TOKEN_EOF {
    // record the field names and types
    if lexTokenType != TOKEN_VARIABLE {
      expectToken(TOKEN_VARIABLE, 'variable')
      exit
    }
    fieldName = lexTokenString
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

  println "  push RAX"  // base of record in memory

  expectToken(TOKEN_DOT, '.')
  recType = lookupType(variable)

  if lexTokenType != TOKEN_VARIABLE {
    typeError("Expected field name, but found: " + lexTokenString)
    exit
  }
  fieldName = lexTokenString

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

  println "  pop RBX"
  if fldSym.offset > 0 {
    print "  add RBX, " println fldSym.offset
  }

  // TODO: this is too big for ints, bytes and bools
  print "  mov [RBX], RAX  ; set " print variable print "." println fieldName
}


generateIncDec: proc(variable: string, inc: bool) {
  advanceParser() // eat the ++ or --

  op = "dec"
  if inc { op = "inc" }

  symbol = lookupVariable(variable)
  if symbol.varType != TYPE_INT {
    typeError("Cannot " + op + "rement variable '" + variable +
        "'; already declared as " + symbol.varType.name)
    exit
  }
  ref = toReference(symbol)
  // TODO: This is too big for ints and bytes
  print "  " print op print " DWORD " println ref
}


// Allocates and populates an array literal. Returns the array type.
parseArrayLiteral: proc: VarType {
  expectToken(TOKEN_LBRACKET, '[')

  slotType = TYPE_UNKNOWN
  slotCount = 0
  println "  ; collect array literal entries"
  while lexTokenType != TOKEN_RBRACKET and lexTokenType != TOKEN_EOF {
    thisSlotType = expr()
    println "  push RAX"
    slotCount = slotCount + 1
    if slotType == TYPE_UNKNOWN {
      slotType = thisSlotType
    } else {
      checkTypes(slotType, thisSlotType)
    }
    if lexTokenType == TOKEN_COMMA {
      expectToken(TOKEN_COMMA, ',')
    } else {
      break
    }
  }
  expectToken(TOKEN_RBRACKET, ']')
  arrayType = makeArrayType(slotType)

  print "  mov RCX, " println slotCount * slotType.size + 5
  println "  mov RDX, 1"
  emitExtern("calloc")
  println "  mov BYTE [RAX], 1"
  print "  mov DWORD [RAX + 1], " println slotCount

  println "\n  ; populate array literal"
  offset = 5 + slotType.size * (slotCount - 1)
  // Could do a runtime loop instead, shrug.
  i = 0 while i < slotCount do i = i + 1 {
    println "  pop RBX"
    // TODO: This is too big for ints, bytes or bools
    print "  mov [RAX + " print offset println "], RBX"
    offset = offset - slotType.size
  }
  println ""
  return arrayType
}


// variable=expression, procname: proc, procname(), arrayname:type[intexpr]
parseStartsWithVariable: proc {
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
    } elif lexTokenType == TOKEN_KEYWORD and lexTokenKw == KW_EXTERN {
      parseExtern(variable)
    } elif lexTokenType == TOKEN_KEYWORD and lexTokenKw == KW_RECORD {
      parseRecordDecl(variable)
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
  } elif lexTokenType == TOKEN_INC or lexTokenType == TOKEN_DEC {
    generateIncDec(variable, lexTokenType == TOKEN_INC)
    return
  }

  parserError("Expected one of '=' ':' '(' '[' '.' but found: " + lexTokenString)
  exit
}

// expect {, parse statements until }
parseBlock: proc {
  expectToken(TOKEN_LBRACE, '{')
  while lexTokenType != TOKEN_RBRACE and lexTokenType != TOKEN_EOF {
    parseStmt()
    // newline between statements
    print "\n"
  }
  expectToken(TOKEN_RBRACE, '}')
}

parseIf: proc {
  // 1. calculate the condition
  println "  ; 'if' condition"
  condType = expr()

  if condType != TYPE_BOOL {
    typeError("Incorrect type of condition in 'if'. Expected: bool, actual: " + condType.name)
    exit
  }

  // 2. if false, go to else
  elseLabel = nextLabel('else')
  println "  cmp AL, 0"
  print "  jz " println elseLabel

  // 3. { parse statements until }
  println "  ; 'if' block"
  parseBlock()

  // this may not be necessary if there are no elses or elifs
  // after the successful "if" block, jump down to the end.
  afterAllLabel = nextLabel('afterIf')
  print "  jmp " println afterAllLabel

  // 4. elseLabel: while token = elif
  emitLabel(elseLabel)
  while lexTokenType == TOKEN_KEYWORD and lexTokenKw == KW_ELIF {
    println "  ; 'elif' block"
    advanceParser() // eat the elif
    condType = expr()
    if condType != TYPE_BOOL {
      typeError("Incorrect type of condition in 'elif'. Expected: bool, actual: " + condType.name)
      exit
    }

    elseLabel = nextLabel('afterElif')
    println "  cmp AL, 0"
    // if the condition is false, go to our local elseLabel
    print "  jz " println elseLabel
    parseBlock()
    // after successful "elif" block, jump to the end
    print "  jmp " println afterAllLabel
    emitLabel(elseLabel)
  }

  // 5. else:
  if lexTokenType == TOKEN_KEYWORD and lexTokenKw == KW_ELSE {
    println "  ; 'else' block"
    advanceParser() // eat the "else"
    parseBlock()
  }

  // 6. afterAll label
  emitLabel(afterAllLabel)
}

numWhiles=0
whileBreakLabels:string[100]
whileContinueLabels:string[100]

parseBreak: proc {
  if numWhiles == 0 {
    parserError("Cannot have 'break' outside 'while' loop")
    exit
  }
  print "  jmp " println whileBreakLabels[numWhiles - 1]
}

parseContinue: proc {
  if numWhiles == 0 {
    parserError("Cannot have continue outside loop")
    exit
  }
  print "  jmp " println whileContinueLabels[numWhiles - 1]
}

parseWhile: proc {
  continueLabel = nextLabel("while_continue")
  emitLabel(continueLabel)

  condType = expr()
  if condType != TYPE_BOOL {
    typeError("Incorrect type of condition in 'while'. Expected: bool, actual: " + condType.name)
    exit
  }
  println "  cmp AL, 0"
  afterLabel = nextLabel("after_while")
  print "  jz " println afterLabel
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
    print "  jmp " println whileBlockLabel

    doStmtLabel = nextLabel("do_stmt")
    // do_stmt:
    emitLabel(doStmtLabel)
    //    emit do code
    parseStmt()
    //    jmp while_continue
    print "  jmp " println continueLabel
  }

  // whileBlock:
  println "  ; while block"
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
    print "  jmp " println doStmtLabel
  }  else {
    print "  jmp " println continueLabel
  }
  emitLabel(afterLabel)

  // Pop the while label stack
  numWhiles = numWhiles - 1
}

parsePrint: proc(isPrintln: bool) {
  exprType = expr()  // puts result in RAX
  if exprType == TYPE_NULL {
    nullindex = addStringConstant("null")
    print "  mov RCX, CONST_" println nullindex
  } elif exprType == TYPE_STRING {

    println "  cmp RAX, 0"
    nullindex = addStringConstant("null")
    print   "  mov RCX, CONST_" println nullindex // default if null
    println "  cmovnz RCX, RAX" // copy RAX to RCX if not null

  } elif exprType == TYPE_BOOL {
    trueindex = addStringConstant("true")
    falseindex = addStringConstant("false")
    println "  cmp AL, 1"
    print "  mov RCX, CONST_" println falseindex
    print "  mov RDX, CONST_" println trueindex
    println "  cmovz RCX, RDX"
  } elif exprType == TYPE_INT {
    index = addStringConstant("%d")
    print "  mov RCX, CONST_" println index
    // TODO: This is too big for ints
    println "  mov RDX, RAX"
  } else {
    parserError("Cannot generate printing " + exprType.name)
    exit
  }
  println "  sub RSP, 0x20"
  println "  extern printf"
  println "  call printf"
  if isPrintln {
    // char 10=newline
    println "  mov RCX, 10"
    println "  extern putchar"
    println "  call putchar"
  }
  println "  extern _flushall"
  println "  call _flushall"
  println "  add RSP, 0x20"
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
    print "  mov RCX, CONST_" println messageIndex
    println "  mov RDX, RAX"
    println "  sub RSP, 0x20"
    println "  extern printf"
    println "  call printf"
    println "  extern _flushall"
    println "  call _flushall"
    println "  add RSP, 0x20\n"
  }

  println "  mov RCX, -1"
  println "  call exit"
}

parseStmt: proc {
  if lexTokenType == TOKEN_EOF {
    return
  } elif lexTokenType == TOKEN_KEYWORD {
    kw = lexTokenKw
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


emitStringTable: proc {
  head = stringTable.head
  while head != null do head = head.next {
    print "  CONST_" print head.index print ': db "'
    entry = head.value
    j = 0 while j < length(entry) do j = j + 1 {
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
    if head.varType == TYPE_STRING {
      println "dq 0"
    } else {
      // TODO: this is 64 bits, which is too many for a 32-bit int or a boolean
      println "dq 0"
    }
  }
}

parseProgram: proc {
  println "; compiled by " + VERSION
  println "global main"
  println "extern exit\n"
  println "section .text"
  println "main:"
  while lexTokenType != TOKEN_EOF {
    parseStmt()
    // newline between statements
    print "\n"
  }
  println "  xor RCX, RCX"
  println "  call exit\n"

  if npeCheckNeeded {
    emitNpeCheck()
  }
  if indexPositiveCheckNeeded {
    emitIndexPositiveCheck()
  }

  if stringTable.head != null or numGlobals > 0 {
    println "section .data"
  }
  if stringTable.head != null {
    emitStringTable()
  }
  if numGlobals > 0 {
    emitGlobalTable()
  }
}

procFinder: proc {
  while lexTokenType != TOKEN_EOF {
    if lexTokenType == TOKEN_VARIABLE {
      variable = lexTokenString
      advanceParser() // eat the variable
      if lexTokenType == TOKEN_COLON {
        advanceParser() // eat the colon
        if lexTokenType == TOKEN_KEYWORD and
          (lexTokenKw == KW_PROC or lexTokenKw == KW_EXTERN) {

          isExtern = lexTokenKw == KW_EXTERN
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
  resetLexer(lexer)
  advanceParser()
}

recordFinder: proc {
  while lexTokenType != TOKEN_EOF {
    if lexTokenType == TOKEN_VARIABLE {
      variable = lexTokenString
      advanceParser() // eat the variable
      if lexTokenType == TOKEN_COLON {
        advanceParser() // eat the colon
        if lexTokenType == TOKEN_KEYWORD and lexTokenKw == KW_RECORD {
          registerRecordName(variable)
        }
        continue
      }
      continue
    }
    advanceParser()
  }
  resetLexer(lexer)
  advanceParser()
}

initParser: proc: Lexer {
  text = input
//    print "text = " println text
  if debug {
  }
  lexer = newLexer(text)
  advanceParser()
  return lexer
}


recordFinder()
procFinder()
parseProgram()


// I don't love this, that it uses the parser global "lexer"
generalError: proc(type: string, message: string) {
  print type + " error at line " print lexer.line
  println ": " + message
  exit
}

typeError: proc(message: string) {
  generalError("Type", message)
}

parserError: proc(message: string) {
  generalError("Parse", message)
}

