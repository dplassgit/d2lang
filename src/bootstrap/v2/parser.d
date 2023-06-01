///////////////////////////////////////////////////////////////////////////////
//                                    PARSER                                 //
//                           VERSION 2 (COMPILED BY V1)                      //
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

// TODO: make this a procedure?
generateNpeTest: proc {
  oklabel = nextLabel("not_null")
  println "  cmp RAX, 0"
  print "  jne " println oklabel

  npeMessageIndex = addStringConstant("\nNull pointer error at line %d.\n")
  print "  mov RCX, CONST_" println npeMessageIndex
  print "  mov RDX, " print lexer.line println "  ; line #"
  println "  sub RSP, 0x20"
  println "  extern printf"
  println "  call printf"
  println "  extern _flushall"
  println "  call _flushall"
  println "  add RSP, 0x20"
  println "  mov RCX, -1"
  println "  extern exit"
  println "  call exit\n"

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
expr: proc: int {
  return boolOr()
}

boolOr: proc: int {
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

boolAnd: proc: int {
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

compare: proc: int {
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
    // TODO: This is too big for ints, should just use EBX, EAX
    print "  cmp RBX, RAX  ; int " print opstring println " int"
    print "  " print OPCODES[op] println " AL"
    return TYPE_BOOL
  } elif leftType == TYPE_STRING and (lexTokenType == TOKEN_EQEQ or lexTokenType == TOKEN_NEQ) {
    op = lexTokenType
    advanceParser() // eat the symbol
    println "  push RAX"

    rightType = addSub()
    checkTypes(leftType, rightType)

    println "  mov RDX, RAX  ; right side"
    println "  pop RCX  ; left side"
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
  } elif (isRecordType(leftType) or leftType == TYPE_NULL) and
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

addSub: proc: int {
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
      // TODO: This is too big for ints, should just use EBX, EAX
      // left = left (op) right
      // TODO: If plus, can just do add rax, rbx instead of two lines
      print "  " print OPCODES[op] print " RBX, RAX  ; int " print opstring println " int"
    }
    println "  mov RAX, RBX"
  }
  return leftType
}

mulDiv: proc: int {
  leftType = unary()
  while leftType == TYPE_INT and
      (lexTokenType == TOKEN_MULT or lexTokenType == TOKEN_DIV or lexTokenType == TOKEN_MOD) {
    op = lexTokenType
    advanceParser() // eat the symbol
    println "  push RAX"

    rightType = unary()
    checkTypes(leftType, rightType)

    println "  pop RBX" // pop the left side

    // TODO: This is too big for ints, should just use EAX & EBX
    // rax = rax (op) rbx
    if op == TOKEN_DIV or op == TOKEN_MOD {
      println "  xchg RAX, RBX  ; put numerator in RAX, denominator in EBX"
      // TODO: THIS IS TOO BIG FOR INTS
      println "  cqo  ; sign extend rax to rdx"
      println "  idiv RBX  ; rax=rax/rbx"
      if op == TOKEN_MOD {
        // modulo is in rdx
        println "  mov RAX, RDX  ; modulo is in rdx"
      }
    } else {
      println "  imul RAX, RBX  ; int * int"
    }
  }
  return leftType
}

// -unary | +unary | length(expr) | asc(expr) | chr(expr) | not expr
unary: proc: int {
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
      println "  neg RAX  ; unary minus"
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
      println "  mov RCX, RAX"
      emitExtern("strlen")
    } elif isArrayType(type) {
      // RAX has location of array
      println "  inc RAX               ; skip past # of dimensions"
      println "  mov DWORD EAX, [RAX]  ; get length (4 bytes only)" // Fun fact: the upper 32 bits are zero-extended
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
      typeError("Cannot take CHR of " + typeName(type))
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
      typeError("Cannot apply NOT to " + typeName(type))
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
    rec = lookupRecord(recordName)
    //index = lookupRecord(recordName)
    //if index == -1 {
    if rec == null {
      typeError("Record '" + recordName + "' not defined")
      exit
    }
    // size = recordSizes[index]
    size = rec.size
    print "  mov RCX, " print size print " ; size of record type " println recordName
    println "  mov RDX, 1"
    emitExtern("calloc")
    return rec.oldType // TYPE_RECORD_BASE + index
  }

  return composite()
}


// Makes sure RAX is >= 0. 'type' is either STRING or ARRAY
// TODO: Make this a subroutine
generateIndexPositiveCheck: proc(type: string) {
  oklabel = nextLabel("index_ge0")
  println "  cmp RAX, 0"
  print "  jge " println oklabel

  errorindex = addStringConstant(
    "\nInvalid index error at line %d: " + type
    + " index must be non-negative; was %d\n")
  print "  mov RCX, CONST_" println errorindex
  print "  mov RDX, " print lexer.line println "  ; line #"
  println "  mov R8, RAX" // actual index
  println "  sub RSP, 0x20"
  println "  extern printf"
  println "  call printf"
  println "  extern _flushall"
  println "  call _flushall"
  println "  add RSP, 0x20"
  println "  mov RCX, -1"
  println "  extern exit"
  println "  call exit"

  emitLabel(oklabel)
}


// Generate a "get" of foo[int]
// returns the base array type
generateArrayIndex: proc(arrayType: int): int {
  baseType = toBaseType(arrayType)

  println "  push RAX  ; save array base location"
  indexType = expr()
  if indexType != TYPE_INT {
    typeError("Array index must be int; was " + typeName(indexType))
    exit
  }
  expectToken(TOKEN_RBRACKET, ']')

  generateIndexPositiveCheck('ARRAY')
  // TODO: make sure index < length

  // 1. multiply index by size
  print "  imul RAX, " print sizeByType(baseType) println "  ; bytes per entry (temporarily 8)"
  // 2. add 5
  println "  add RAX, 5  ; skip header"
  // 3. add to base
  println "  pop RBX  ; base location"
  println "  add RAX, RBX  ; add to base location"
  // 4. get value
  // TODO: This is too big for ints or bools
  println "  mov RAX, [RAX]  ; get array slot value"

  return baseType
}

// Generate a "get" of foo[int]
generateStringIndex: proc {
  println "  push RAX"
  indexType = expr()
  if indexType != TYPE_INT {
    typeError("String index must be int; was " + typeName(indexType))
    exit
  }
  expectToken(TOKEN_RBRACKET, ']')
  println "  push RAX"

  generateIndexPositiveCheck('STRING')
  // TODO: check that index < string length

  // allocate a 2-byte string
  println "  mov RCX, 2"
  println "  mov RDX, 1"
  emitExtern("calloc")
  println "  pop RBX"
  println "  pop RCX"
  println "  add RCX, RBX  ; base+index"
  println "  mov BYTE CL, [RCX]  ; get byte"
  println "  mov BYTE [RAX], CL  ; store byte"
}

// atom | atom [ index ] | atom . fieldname
composite: proc: int {
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

        // TODO: This must check that the record type exists!
        recType = findTypeById(leftType)
        if recType == null {
        }
        recSym = recType.recordType
        if recSym == null {
          print "  ; in field get recordtypename is " print recSym.name println " but it was not found!"
          exit  
        } else {
          print "  ; in field get recordtype is " println recSym.name
        } 
        
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
        leftType = fldSym.type.oldType
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
    print "  mov RAX, [_" print variable println "]"
    return varType
  }

  if currentProc == null {
    // Not in a proc, cannot look up local.
    typeError("Cannot find global variable " + variable)
    exit
  }

  sym = lookupLocal(variable)
  if sym != null {
    offset = sym.offset
    // TODO: This is too big for ints, should just use EAX;
    // also too big for bools
    print "  mov RAX, [RBP" print offset print "]  ; get local " println variable
    return sym.type
  }

  symbol = lookupParam(variable)
  if symbol == null {
    typeError("Cannot find variable " + variable)
    exit
  }
  offset = symbol.offset
  // TODO: This is too big for ints, should just use EAX;
  // also too big for bools
  print "  mov RAX, [RBP+" print offset print "]  ; get param " println variable
  return symbol.type
}

generateProcCall: proc(procName: string) {
  procSym = lookupProc(procName)
  expectToken(TOKEN_LPAREN, '(')

  numArgs = 0
  param = procSym.params
  while lexTokenType != TOKEN_RPAREN and lexTokenType != TOKEN_EOF {
    numArgs = numArgs + 1
    argType = expr()
    if numArgs > procSym.numParams {
      typeError("Too many args to proc '" + procName + "'")
    }
    expectedType = param.type
    param = param.next
    // check arg type
    if argType != expectedType {
      // TODO: print the index and/or the arg name
      print "expected " print typeName(expectedType) print "(" print expectedType println ")"
      print "actual " print typeName(argType) print "(" print argType println ")"
      typeError("Incorrect arg type to proc '" + procName +
        "'. Expected: " + typeName(expectedType) + ", actual: " + typeName(argType))
    }
    println "  push RAX"
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
  if numArgs > 1 {
    println "  ; swap args on stack"
    // TODO: will we always push 8 bytes per arg?!
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
  print "  call _" println procName

  // # of bytes we have to adjust the stack (pseudo-pop)
  bytes = 8 * numArgs
  if bytes > 0 {
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
atom: proc: int {
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
    // TODO: This is too big for ints, should just use EAX
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

    type = lookupProc(variable).returnType
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

  parserError("Unexpected token: " + lexTokenString)
  exit
}


///////////////////////////////////////////////////////////////////////////////
//                               STATEMENT RULES                             //
///////////////////////////////////////////////////////////////////////////////

// This parses an arg or field. Not arrays (yet)
parseType: proc: int {
  i = 1 while i <= 3 do i = i + 1 {
    if TYPE_NAMES[i] == lexTokenString {
      advanceParser()
      // TODO: allow arrays here
      return i
    }
  }
  // Lookup record here
  recordType = lookupRecord(lexTokenString)
  if recordType != null {
    advanceParser()
    // TODO: allow arrays here
    return recordType.oldType
  }

  parserError("Unknown type " + lexTokenString)
  exit
}

skipType: proc {
  i = 1 while i <= 3 do i = i + 1 {
    if TYPE_NAMES[i] == lexTokenString {
      advanceParser()
      // TODO: allow arrays here
      return
    }
  }
  expectToken(TOKEN_VARIABLE, 'record type')
  // TODO: allow arrays here
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
  println "  mov EBX, EAX  ; save size"
  print "  imul RAX, " print sizeByType(baseType) println "  ; bytes per entry"
  println "  add RAX, 5    ; 5 more bytes for dimensions & # entries"
  println "  mov RCX, RAX  ; num items"
  println "  mov RDX, 1    ; bytes per item"
  emitExtern("calloc")

  // byte 1 is for # of dimensions, byte 2 is # of entries
  println "  mov BYTE [RAX], 1  ; # of dimensions"
  println "  mov DWORD [RAX+1], EBX  ; # of entries"

  arrayType = baseType + TYPE_ARRAY_BASE
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

parseProcSignature: proc(procName: string) {
  myProcNum = numProcs
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
    typeError("Incorrect return type of '" + currentProcName + "'. Expected "
      + typeName(expectedType) + " but found " + typeName(actualType))
    exit
  }
  print "  jmp __exit_of_" println currentProcName
}

generateAssignment: proc(variable: string, exprType: int): int {
  varType = lookupGlobal(variable)
  isGlobal = varType != TYPE_UNKNOWN or currentProc == null
  if isGlobal {
    if varType == TYPE_UNKNOWN {
      registerGlobal(variable, exprType)
      varType = exprType
    }

    // TODO: This is too big for ints, should just use EAX for int, AL for bool
    print "  mov [_" print variable println "], RAX"
    return varType
  }

  // not global; try param or local
  symbol = lookupParam(variable)
  offset = 0
  if symbol != null {
    // found it
    offset = symbol.offset ///paramOffsets[index]
    varType = symbol.type // paramTypes[index]
    print "  mov [RBP+" print offset print "], RAX" print "  ; set param " println variable
    return varType
  }

  sym = lookupLocal(variable)
  if sym != null {
    offset = sym.offset
    varType = sym.type
  } else {
    offset = registerLocal(variable, exprType)
    varType = exprType
  }
  print "  mov [RBP" print offset print "], RAX" print"  ; set local " println variable
  return varType
}

generateArraySet: proc(variable: string) {
  type = lookupType(variable)

  // Make sure 'variable' is an array
  if isArrayType(type) { // TODO: flip this logic: if not isArrayType(type)...
    expectToken(TOKEN_LBRACKET, '[')
    indexType = expr()
    if indexType != TYPE_INT {
      typeError("Incorrect type for array index. Expected: int, actual: " + typeName(indexType))
      exit
    }

    // TODO: make sure index is not negative 
    
    println "  mov RBX, RAX"
    // TODO: FIX ME
    println "  shl RBX, 3  ; bytes per element TEMPORARY"
    println "  add RBX, 5  ; header"
    generateGetVariable(variable)
    println "  add RBX, RAX"
    println "  push RBX"
    expectToken(TOKEN_RBRACKET, ']')
    expectToken(TOKEN_EQ, '=')

    rightType = expr()

    checkTypes(toBaseType(type), rightType)

    println "  pop RBX"
    println "  mov [RBX], RAX  ; array set"
  } else {
    typeError("Cannot take index of non-array variable " + variable)
    exit
  }
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
    // TODO: This isn't completely implemented but I fear I'll forget it...
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

    type = parseType()

    // store the name and type of the field
    vt = findTypeById(type)
    registerField(recSym, fieldName, vt)
  }
  expectToken(TOKEN_RBRACE, "}")
}

generateFieldSet: proc(variable: string) {
  generateGetVariable(variable)
  generateNpeTest()

  println "  push RAX"  // base of record in memory

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

  recType = findTypeById(type)
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

  checkTypes(fldSym.type.oldType, rhsType)

  println "  pop RBX"
  if fldSym.offset > 0 {
    print "  add RBX, " println fldSym.offset
  }

  // TODO: this is too big for ints
  print "  mov [RBX], RAX  ; set " print variable print "." println fieldName
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
    typeError("Incorrect type of condition in 'if'. Expected: bool, actual: " + typeName(condType))
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
      typeError("Incorrect type of condition in 'elif'. Expected: bool, actual: " + typeName(condType))
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
    typeError("Incorrect type of condition in 'while'. Expected: bool, actual: " + typeName(condType))
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
    println "  mov RCX, RAX"
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
    // TODO: This is too big for ints, should just use EDX, EAX
    println "  mov RDX, RAX"
  } else {
    parserError("Cannot generate printing " + typeName(exprType))
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
      typeError("Incorrect type of 'exit' expression. Expected: string, actual: " + typeName(exitExprType))
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
      if ch == 10 or ch == 13 or ch == 34 or ch == 37 {
        // unprintable characters (\n, \r, " or %) become ints
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
    if head.type == TYPE_STRING {
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

