///////////////////////////////////////////////////////////////////////////////
//                           VERSION 1 (WRITTEN IN V0)
///////////////////////////////////////////////////////////////////////////////

// TODO (in no particular order)
//   more type checking
//   records: declare, new, field set, field get
//   array parameters
//   increment, decrement?

debug=false

///////////////////////////////////////////////////////////////////////////////
//                                     LEXER                                 //
///////////////////////////////////////////////////////////////////////////////
TOKEN_EOF=0
TOKEN_PLUS=1
TOKEN_MINUS=2
TOKEN_MULT=3
TOKEN_AND=4 // bit and
TOKEN_OR=5  // bit or
TOKEN_DIV=6
TOKEN_MOD=7
TOKEN_EQEQ=8
TOKEN_NEQ=9
TOKEN_LT=10
TOKEN_GT=11
TOKEN_LEQ=12
TOKEN_GEQ=13
TOKEN_BIT_NOT=14 // bit not
TOKEN_INT=15  // int constant
TOKEN_BOOL=16  // bool constant
TOKEN_STRING=17 // string constant
TOKEN_VARIABLE=18
TOKEN_EQ=19
TOKEN_LPAREN=20
TOKEN_RPAREN=21
TOKEN_LBRACE=22
TOKEN_RBRACE=23
TOKEN_COLON=24
TOKEN_COMMA=25
TOKEN_KEYWORD=26
TOKEN_LBRACKET=27
TOKEN_RBRACKET=28
//TOKEN_DOT=29
//TOKEN_SHIFT_LEFT=30
//TOKEN_SHIFT_RIGHT=31
//TOKEN_XOR=32 // bit xor

KW_PRINT=0
KW_IF=1
KW_ELSE=2
KW_ELIF=3
KW_PROC=4
KW_RETURN=5
KW_WHILE=6
KW_DO=7
KW_BREAK=8
KW_CONTINUE=9
KW_INT=10   // int keyword
KW_BOOL=11  // bool keyword
KW_STRING=12  // string keyword
KW_NULL=13
KW_INPUT=14
KW_LENGTH=15
KW_CHR=16
KW_ASC=17
KW_EXIT=18
KW_AND=19 // boolean and
KW_OR=20  // boolean or
KW_NOT=21 // boolean not
// KW_MAIN=SKIPPED
KW_RECORD=22
KW_NEW=23
KW_DELETE=24
KW_PRINTLN=25

KEYWORDS:string[26]
KEYWORDS[KW_PRINT]="print"
KEYWORDS[KW_IF]="if"
KEYWORDS[KW_ELSE]="else"
KEYWORDS[KW_ELIF]="elif"
KEYWORDS[KW_PROC]="proc"
KEYWORDS[KW_RETURN]="return"
KEYWORDS[KW_WHILE]="while"
KEYWORDS[KW_DO]="do"
KEYWORDS[KW_BREAK]="break"
KEYWORDS[KW_CONTINUE]="continue"
KEYWORDS[KW_INT]="int"
KEYWORDS[KW_BOOL]="bool"
KEYWORDS[KW_STRING]="string"
KEYWORDS[KW_NULL]="null"
KEYWORDS[KW_INPUT]="input"
KEYWORDS[KW_LENGTH]="length"
KEYWORDS[KW_CHR]="chr"
KEYWORDS[KW_ASC]="asc"
KEYWORDS[KW_EXIT]="exit"
KEYWORDS[KW_AND]="and"
KEYWORDS[KW_OR]="or"
KEYWORDS[KW_NOT]="not"
KEYWORDS[KW_PRINTLN]="println"
KEYWORDS[KW_RECORD]="record"
KEYWORDS[KW_NEW]="new"
KEYWORDS[KW_DELETE]="delete"

// Global for lexer:
lexerText=''   // full text
lexerLoc=0     // index/location inside text
lexerCc=0      // current character

newLexer: proc(text: string) {
  lexerText = text
  resetLexer()
}

resetLexer: proc() {
  lexerLoc = 0
  lexerCc = 0
  advanceLex()
}

nextToken: proc(): string {
  // skip unwanted whitespace
  while (lexerCc == 32 or lexerCc == 10 or lexerCc == 9 or lexerCc == 13) {
    advanceLex()
  }
  if lexerCc != 0 {
    if isDigit(lexerCc) {
      return makeIntToken()
    } elif isLetter(lexerCc) {
      // might be string, keyword, boolean constant
      return makeTextToken()
    } else {
      return makeSymbolToken()
    }
  }

  return Token(TOKEN_EOF, "")
}

advanceLex: proc() {
  if lexerLoc < length(lexerText) {
    lexerCc=asc(lexerText[lexerLoc])
  } else {
    // Indicates no more characters
    lexerCc=0
  }
  lexerLoc = lexerLoc + 1
  if debug {
    // print "; Lexer cc " print lexerCc print ":" print chr(lexerCc) print "\n"
  }
}

///////////////////////////////////////////////////////////////////////////////
// Lexer token values, for external consumption
///////////////////////////////////////////////////////////////////////////////

lexTokenType=0
lexTokenString=''
lexTokenInt=0
lexTokenKw=0
lexTokenBool=false

// Bundle the data about the token in a single string of the format
// 't <type> <value>'
Token: proc(type: int, value: string): string {
  lexTokenType = type
  lexTokenString = value
  lexTokenInt = -1
  lexTokenKw = -1
  lexTokenBool = false
  if debug {
    print "; Making token type: " print type print " value: (skipped)\n"
    // print value print "\n"
  }
  return 't ' + toString(type) + ' ' + value
}

// Bundle the data about the token in a single string of the format
// 'i <value>'
IntToken: proc(value: int, valueAsString: string): string {
  lexTokenType = TOKEN_INT
  lexTokenString = valueAsString
  lexTokenInt = value
  lexTokenKw = -1
  lexTokenBool = false
  return 'i ' + valueAsString
}

// Bundle the data about the token in a single string of the format
// 'b true/false'
BoolToken: proc(value: bool, valueAsString: string): string {
  lexTokenType = TOKEN_BOOL
  lexTokenString = valueAsString
  lexTokenInt = -1
  lexTokenKw = -1
  lexTokenBool = value
  return 'b ' + lexTokenString
}

// Bundle the data about the token in a single string of the format
// 'k value'
KeywordToken: proc(value: int, valueAsString: string): string {
  lexTokenType = TOKEN_KEYWORD
  lexTokenString = valueAsString
  lexTokenKw = value
  lexTokenInt = -1
  lexTokenBool = false
  return 'k ' + lexTokenString
}

toString: proc(i: int): string {
  if i == 0 {
    return '0'
  }
  val = ''
  while i > 0 do i = i / 10 {
    val = chr((i % 10) + 48) + val
  }
  return val
}

isLetter: proc(c: int): bool {
  // return (c>=asc('a') and c <= asc('z')) or (c>=asc('A') and c<=asc('Z')) or c==asc('_')
  return (c >= 97 and c <= 122) or (c >= 65 and c <= 90) or c == 95
}

isDigit: proc(c: int): bool {
  // return c>=asc('0') and c <= asc('9')
  return c >= 48 and c <= 57
}

isLetterOrDigit: proc(c: int): bool {
  return isLetter(c) or isDigit(c)
}

makeTextToken: proc(): string {
  value=''
  if lexerCc == 95 {
    // do not allow leading _
    print "ERROR: Cannot start variable with an underscore\n"
    exit
  }
  if isLetter(lexerCc) {
    value=value + chr(lexerCc)
    advanceLex()
  }
  while isLetterOrDigit(lexerCc) {
    value=value + chr(lexerCc)
    advanceLex()
  }

  if value == 'true' or value == 'false' {
    return BoolToken(value == 'true', value)
  }

  i=0 while i < length(KEYWORDS) do i = i + 1 {
    if value == KEYWORDS[i] {
      return KeywordToken(i, value)
    }
  }

  return Token(TOKEN_VARIABLE, value)
}

makeIntToken: proc(): string {
  value=0
  value_as_string = ''

  while isDigit(lexerCc) do advanceLex() {
    // value=value * 10 + lexerCc - asc('0')
    value=value * 10 + lexerCc - 48
    value_as_string = value_as_string + chr(lexerCc)
  }
  return IntToken(value, value_as_string)
}

startsWithSlash: proc(): string {
  advanceLex() // eat the first slash
  if lexerCc == 47 {
    // second slash == comment.
    advanceLex() // eat the second slash
    // Eat characters until newline
    while lexerCc != 10 and lexerCc != 0 do advanceLex() {}
    if lexerCc != 0 {
      advanceLex() // eat the newline
    }
    // TODO figure out if this can be done a different way, maybe with a "comment" token?
    return nextToken()
  }
  return Token(TOKEN_DIV, '/')
}

startsWithBang: proc(): string {
  oc=lexerCc
  advanceLex() // eat the !
  if lexerCc == 61 {
    advanceLex()
    return Token(TOKEN_NEQ, '!=')
  }
  print 'Unknown character:' + chr(lexerCc) + ' ASCII code: ' + toString(lexerCc)
  exit
  // return Token(TOKEN_BIT_NOT, '!'))
}

startsWithGt: proc(): string {
  oc=lexerCc
  advanceLex()
  if lexerCc == 61 {
    advanceLex()
    return Token(TOKEN_GEQ, '>=')
  //} elif lexerCc == '>' {
    // shift right
    //advanceLex()
    //return Token(TOKEN_SHIFT_RIGHT, '>>')
  }
  return Token(TOKEN_GT, '>')
}

startsWithLt: proc(): string {
  oc=lexerCc
  advanceLex()
  if lexerCc == 61 {
    advanceLex()
    return Token(TOKEN_LEQ, '<=')
  //} elif lexerCc == '<' {
    //// shift right
    //advanceLex()
    //return Token(TOKEN_SHIFT_LEFT, '<<')
  }
  return Token(TOKEN_LT, '<')
}

startsWithEq: proc(): string {
  oc=lexerCc
  advanceLex()
  if lexerCc == 61 {
    advanceLex()
    return Token(TOKEN_EQEQ, '==')
  }
  return Token(TOKEN_EQ, '=')
}

makeStringLiteralToken: proc(firstQuote: int): string {
  advanceLex() // eat the tick/quote
  sb=''
  while lexerCc != firstQuote and lexerCc != 0 {
    if lexerCc == 92 { // backslash
      advanceLex()
      if lexerCc == 110 { // backslash - n
        sb=sb + chr(10)  // linefeed
      } elif lexerCc == 92 {
        sb=sb + chr(92)  // literal backslash
      }
    } else {
      sb=sb + chr(lexerCc)
    }
    advanceLex()
  }

  if lexerCc == 0 {
    print 'ERROR: Unclosed string literal ' print sb print "\n"
    exit
  }

  advanceLex() // eat the closing tick/quote
  return Token(TOKEN_STRING, sb)
}

makeSymbolToken: proc(): string {
  oc = lexerCc
  if oc == 61 {
    return startsWithEq()
  } elif oc == 60 {
    return startsWithLt()
  } elif oc == 62 {
    return startsWithGt()
  } elif oc == 43 {
    advanceLex()
    return Token(TOKEN_PLUS, '+')
  } elif oc == 45 {
    advanceLex()
    return Token(TOKEN_MINUS, '-')
  } elif oc == 40 {
    advanceLex()
    return Token(TOKEN_LPAREN, '(')
  } elif oc == 41 {
    advanceLex()
    return Token(TOKEN_RPAREN, ')')
  } elif oc == 42 {
    advanceLex()
    return Token(TOKEN_MULT, '*')
  } elif oc == 47 {
    return startsWithSlash()
  } elif oc == 37 {
    advanceLex()
    return Token(TOKEN_MOD, '%')
  // } elif oc == asc('&') {
  //   advanceLex()
  //   return Token(TOKEN_AND, '&')
  // } elif oc == asc('|') {
  //   advanceLex()
  //   return Token(TOKEN_OR, '|')
  } elif oc == 33 {
    return startsWithBang()
  } elif oc == 123 {
    advanceLex()
    return Token(TOKEN_LBRACE, '{')
  } elif oc == 125 {
    advanceLex()
    return Token(TOKEN_RBRACE, '}')
  } elif oc == 91 {
    advanceLex()
    return Token(TOKEN_LBRACKET, '[')
  } elif oc == 93 {
    advanceLex()
    return Token(TOKEN_RBRACKET, ']')
  } elif oc == 58 {
    advanceLex()
    return Token(TOKEN_COLON, ':')
  } elif oc == 34 or oc == 39 { // double or single quote
    return makeStringLiteralToken(oc)
  } elif oc == 44 {
    advanceLex()
    return Token(TOKEN_COMMA, ',')
  // } elif oc == '.' {
  //   advanceLex()
  //   return Token(TOKEN_DOT, oc)
  } else {
    print 'ERROR: Unknown character:' + chr(lexerCc) + ' ASCII code: ' print lexerCc print "\n"
    exit
  }
}

printToken: proc() {
  if lexTokenType == TOKEN_EOF {
    print 'Token: EOF' print "\n"
  } elif lexTokenType == TOKEN_INT {
    print 'Int token: ' print lexTokenInt print "\n"
  } elif lexTokenType == TOKEN_STRING {
    print 'String token: "' print lexTokenString print '"\n'
  } elif lexTokenType == TOKEN_BOOL {
    if lexTokenBool {
      print 'Bool token: true\n'
    } else {
      print 'Bool token: false\n'
    }
  } elif lexTokenType == TOKEN_KEYWORD {
    print 'Keyword token: ' print lexTokenString print "\n"
  } elif lexTokenType == TOKEN_VARIABLE {
    print 'Variable: ' print lexTokenString print "\n"
  } else {
    print 'Token: ' print lexTokenString print ' type: ' print lexTokenType print '\n'
  }
}

//text = input
//newLexer(text)

//count = 1
//token = nextToken()
//printToken(token)

//while lexTokenType != TOKEN_EOF do count = count + 1 {
  //token = nextToken()
  //printToken(token)
//}

//print 'Total number of tokens: '
//print count print "\n"


///////////////////////////////////////////////////////////////////////////////
//                                    TYPES
///////////////////////////////////////////////////////////////////////////////

TYPE_UNKNOWN=0
TYPE_INT=1
TYPE_BOOL=2
TYPE_STRING=3
TYPE_ARRAY=4  // NOTE THIS IS NOT AN OFFICIAL TYPE
TYPE_INT_ARRAY=5
TYPE_BOOL_ARRAY=6
TYPE_STRING_ARRAY=7
TYPE_VOID=8

TYPE_NAMES:string[9]
TYPE_NAMES[TYPE_UNKNOWN] = "unknown"
TYPE_NAMES[TYPE_INT] = "int"
TYPE_NAMES[TYPE_BOOL] = "bool"
TYPE_NAMES[TYPE_STRING] = "string"
TYPE_NAMES[TYPE_INT_ARRAY] = "int[]"
TYPE_NAMES[TYPE_BOOL_ARRAY] = "bool[]"
TYPE_NAMES[TYPE_STRING_ARRAY] = "string[]"
TYPE_NAMES[TYPE_VOID] = "void"

TYPE_SIZES:int[9]
TYPE_SIZES[TYPE_UNKNOWN] = 0
TYPE_SIZES[TYPE_INT] = 4
TYPE_SIZES[TYPE_BOOL] = 1
TYPE_SIZES[TYPE_STRING] = 8
TYPE_SIZES[TYPE_INT_ARRAY] = 8
TYPE_SIZES[TYPE_BOOL_ARRAY] = 8
TYPE_SIZES[TYPE_STRING_ARRAY] = 8
TYPE_SIZES[TYPE_VOID] = 0


///////////////////////////////////////////////////////////////////////////////
//                                    PARSER                                 //
///////////////////////////////////////////////////////////////////////////////

advanceParser: proc() {
  nextToken()
  if debug {
    //print "; new token is " + lexTokenString print "\n"
  }
}

expectToken: proc(expectedTokenType: int, tokenStr: string) {
  if lexTokenType != expectedTokenType  {
    print "ERROR: expected '" + tokenStr + "' but found: " printToken()
    exit
  }
  advanceParser() // eat the expected token
}

expectKeyword: proc(expectedKwType: int, tokenStr: string) {
  if lexTokenType != TOKEN_KEYWORD or lexTokenKw != expectedKwType {
    print "ERROR: expected '" + tokenStr + "' but found: " printToken()
    exit
  }
  advanceParser() // eat the keyword
}


///////////////////////////////////////////////////////////////////////////////
// SYMBOL TABLES
///////////////////////////////////////////////////////////////////////////////

numStrings = 0
stringTable: string[400]  // v0.d has 311 strings

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
  return numStrings - 1
}

sizeByType: proc(type: int): int {
  if type == TYPE_UNKNOWN {
    print "ERROR: Cannot get size of unknown type\n"
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
    print "ERROR: Cannot register global '" + name + "' with unknown type\n"
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


MAX_NUM_PROCS = 100 // currently v0.d has ~60 procs
numProcs = 0
procNames: string[MAX_NUM_PROCS]
returnTypes: int[MAX_NUM_PROCS]

numParams: int[MAX_NUM_PROCS]
PARAMS_PER_PROC = 4
// These are sparse arrays; the start index for the 0th param of each proc is 4 * proc num
paramNames: string[MAX_NUM_PROCS * PARAMS_PER_PROC]
paramTypes: int[MAX_NUM_PROCS * PARAMS_PER_PROC]
paramOffsets: int[MAX_NUM_PROCS * PARAMS_PER_PROC]

numLocals: int[MAX_NUM_PROCS]
LOCALS_PER_PROC = 10
// These are sparse arrays; the start index for the 0th local of each proc is 10 * proc num
localNames: string[MAX_NUM_PROCS * LOCALS_PER_PROC]
localTypes: int[MAX_NUM_PROCS * LOCALS_PER_PROC]
localOffsets: int[MAX_NUM_PROCS * LOCALS_PER_PROC]

currentProcNum = -1


registerProc: proc(name: string, returnType: int) {
  if returnType == TYPE_UNKNOWN {
    print "ERROR: Cannot have unknown proc return type\n"
    exit
  }
  // TODO: make sure it doesn't exist yet
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
  print "ERROR: Cannot set current proc num for proc '" print name print "'\n"
  exit
}

lookupProcReturnType: proc(name: string): int {
  i = 0 while i < numProcs do i = i + 1 {
    if name == procNames[i] {
      return returnTypes[i]
    }
  }
  print "ERROR: Cannot find proc '" print name print "'\n"
  exit
}

// returns the index of the param in the arrays
lookupParam: proc(name: string): int {
  if currentProcNum == -1 {
    print "ERROR: Cannot lookup parameter " print name print " because not in a proc"
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
    print "ERROR: Cannot lookup local " print name print " because not in a proc"
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

registerParam: proc(name: string, type: int) {
  // TODO: make sure it's not already defined
}

// Returns the offset of this local.
registerLocal: proc(name: string, type: int): int {
  if type == TYPE_UNKNOWN {
    print "ERROR: Cannot register local '" + name + "' with unknown type\n"
    exit
  }
  myLocalCount = numLocals[currentProcNum]
  if myLocalCount == LOCALS_PER_PROC {
    print "ERROR: Too many locals. Max is " print LOCALS_PER_PROC print "\n"
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


///////////////////////////////////////////////////////////////////////////////
// CODEGEN UTILS
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
  print "  add RSP, 0x20\n"
}


///////////////////////////////////////////////////////////////////////////////
// EXPRESSION RULES
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
// composite -> atom | atom [ int ]
// atom -> int constant, bool constant, string constant, variable, '(' expr ')', 'input'


// Each of these returns the type of the expression: TYPE_INT, TYPE_BOOL, TYPE_STRING, etc.
expr: proc(): int {
  return boolOr()
}

boolOr: proc(): int {
  leftType = boolAnd()
  if lexTokenType == TOKEN_KEYWORD and leftType == TYPE_BOOL {
    while lexTokenKw == KW_OR {
      advanceParser() // eat the symbol
      print "  push RAX\n"
      // TODO: check types
      boolAnd()
      print "  pop RBX\n" // pop the left side
      // left = left (op) right
      print "  or BL, AL  ; bool or bool\n"
      print "  mov AL, BL\n"
    }
  }
  return leftType
}

boolAnd: proc(): int {
  leftType = compare()
  if lexTokenType == TOKEN_KEYWORD and leftType == TYPE_BOOL {
    while lexTokenKw == KW_AND {
      advanceParser() // eat the symbol
      print "  push RAX\n"
      // TODO: check types
      compare()
      print "  pop RBX\n" // pop the left side
      // left = left (op) right
      print "  and BL, AL  ; bool and bool\n"
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
    // TODO: check types
    addSub()
    print "  pop RBX\n" // pop the left side
    // left = left (op) right
    // TODO: This is too big for ints, should just use EBX, EAX
    print "  cmp RBX, RAX  ; int " print opstring print " int\n"
    print "  " print OPCODES[op] print " AL\n"
    return TYPE_BOOL
  }
  if leftType == TYPE_STRING and (lexTokenType == TOKEN_EQEQ or lexTokenType == TOKEN_NEQ) {
    op = lexTokenType
    opstring = lexTokenString
    advanceParser() // eat the symbol
    print "  push RAX\n"
    // TODO: check types
    addSub()
    print "  mov RDX, RAX  ; right side\n"
    print "  pop RCX  ; left side\n"
    emitExtern("strcmp")
    print "  cmp RAX, 0\n"
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
    if leftType != rightType {
      print "ERROR: Type mismatch. Left operand is " print TYPE_NAMES[leftType]
      print ", but right operand is " print TYPE_NAMES[rightType] print "\n"
      exit
    }
    print "  pop RBX\n" // pop the left side
    if leftType == TYPE_BOOL {
      print "ERROR: Cannot add or subtract booleans\n"
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
      print "ERROR: Cannot subtract strings\n"
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
    // TODO: check types
    unary()
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

unary: proc(): int {
  if lexTokenType == TOKEN_PLUS {
    advanceParser() // eat the plus
    return unary()
  } elif lexTokenType == TOKEN_MINUS {
    advanceParser() // eat the minus
    type = unary()
    if type == TYPE_INT {
      // TODO: This is too big for ints, should just use EAX
      print "  neg RAX  ; unary minus\n"
      return type
    }
    print "ERROR: cannot codegen negative non-ints yet\n"
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
      // RAX has location or array
      print "  inc RAX               ; skip past # of dimensions\n"
      print "  mov DWORD EAX, [RAX]  ; get length (4 bytes only)\n" // Fun fact: the upper 32 bits are zero-extended
    }
    else {
      print "ERROR: Cannot take LENGTH of " print TYPE_NAMES[type] print "\n"
      exit
    }

    return TYPE_INT
  } elif lexTokenType == TOKEN_KEYWORD and lexTokenKw == KW_ASC {
    advanceParser() // eat the asc
    expectToken(TOKEN_LPAREN, '(')
    type = expr()
    expectToken(TOKEN_RPAREN, ')')
    if type != TYPE_STRING {
      print "ERROR: Cannot take ASC of " print TYPE_NAMES[type] print "\n"
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
    print "  push RAX  ; save the int\n"
    expectToken(TOKEN_RPAREN, ')')

    if type != TYPE_INT {
      print "ERROR: Cannot take CHR of " print TYPE_NAMES[type] print "\n"
      exit
    }

    // allocate a 2-byte string
    print "  mov RCX, 2\n"
    print "  mov RDX, 1\n"
    emitExtern("calloc")
    print "  pop RBX  ; get character\n"
    print "  mov BYTE [RAX], BL  ; store byte\n"
    return TYPE_STRING
  }

  return composite()
}

isArrayType: proc(type: int): bool {
  return type > TYPE_ARRAY and type <= TYPE_STRING_ARRAY
}

toBaseType: proc(arrayType: int): int {
  return arrayType - TYPE_ARRAY
}


// Generate a "get" of foo[int]
// returns the base array type
generateArrayIndex: proc(arrayType: int): int {
  baseType = toBaseType(arrayType)

  print "  push RAX  ; save array base location\n"
  indexType = expr()
  if indexType != TYPE_INT {
    print "ERROR: Array index must be int; was " + TYPE_NAMES[indexType] + "\n"
    exit
  }
  expectToken(TOKEN_RBRACKET, ']')

  // TODO: make sure index > 0 and < length
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
  print "  push RAX  ; save string location\n"
  indexType = expr()
  if indexType != TYPE_INT {
    print "ERROR: String index must be int; was " + TYPE_NAMES[indexType] + "\n"
    exit
  }
  expectToken(TOKEN_RBRACKET, ']')
  print "  push RAX  ; save index\n"

  // TODO: check for index > 0 and index < string length

  // allocate a 2-byte string
  print "  mov RCX, 2\n"
  print "  mov RDX, 1\n"
  emitExtern("calloc")
  print "  pop RBX  ; index\n"
  print "  pop RCX  ; string base\n"
  print "  add RCX, RBX  ; calculate offset to source character\n"
  print "  mov BYTE CL, [RCX]  ; get byte\n"
  print "  mov BYTE [RAX], CL  ; store byte\n"
}

composite: proc(): int {
  leftType = atom()
  if lexTokenType == TOKEN_LBRACKET {
    // array index
    expectToken(TOKEN_LBRACKET, '[')

    if isArrayType(leftType) {
      return generateArrayIndex(leftType)
    } elif leftType == TYPE_STRING {
      generateStringIndex()
      return leftType
    }

    print "ERROR: Cannot take index of " + TYPE_NAMES[leftType] + "\n"
    exit
  }
  return leftType
}

generateGetVariable: proc(variable: string): int {
  varType = lookupGlobal(variable)
  if varType != TYPE_UNKNOWN {
    // TODO: This is too big for ints, should just use EAX;
    // also too big for bools
    print "  mov RAX, [_" print variable print "]  ; get global '" print variable print "'\n"
    return varType
  }
  if currentProcNum == -1 {
    // not in a proc, cannot look up local
    print "ERROR: Cannot find global variable " print variable print "\n"
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
    print "ERROR: Cannot find param" print variable print "\n"
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
  print "  add RSP, " print bytes print "  ; adjust stack for pushed params\n\n"
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

// atom -> constant | variable | variable '(' args ')' | '(' expr ')'
atom: proc(): int {
  if lexTokenType == TOKEN_STRING {
    // string constant
    index = addStringConstant(lexTokenString)
    advanceParser()
    print "  mov RAX, CONST_" print index print "  ; string constant\n"
    return TYPE_STRING

  } elif lexTokenType == TOKEN_INT {
    // int constant
    intval = lexTokenInt
    advanceParser()
    // TODO: This is too big for ints, should just use EAX
    print "  mov RAX, " print intval print "  ; int constant\n"
    return TYPE_INT

  } elif lexTokenType == TOKEN_BOOL {
    // bool constant
    boolval = lexTokenBool
    advanceParser()
    if boolval {
      print "  mov AL, 1  ; bool constant true\n"
    } else {
      print "  xor RAX, RAX  ; bool constant false\n"
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
      print "ERROR: Return type of " print variable print " is void. Cannot assign it to a variable.\n"
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

  print "ERROR: cannot parse token in atom(): " printToken()
  exit
}


///////////////////////////////////////////////////////////////////////////////
// STATEMENT RULES
///////////////////////////////////////////////////////////////////////////////

parseType: proc(): int {
  i = 1 while i <= 3 do i = i + 1 {
    if TYPE_NAMES[i] == lexTokenString {
      advanceParser()
      return i
    }
  }
  // TODO: allow arrays here
  print "ERROR: Unknown type " printToken()
  exit
}


// Array declaration
parseArrayDecl: proc(variable: string) {
  // the next token should be a type.
  baseType = parseType()
  expectToken(TOKEN_LBRACKET, '[')

  sizeType = expr()
  if sizeType != TYPE_INT {
    print "ERROR: Array size must be an int, but was " print TYPE_NAMES[sizeType] print "\n"
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

// Procedure declaration
parseProc: proc(procName: string) {
  expectKeyword(KW_PROC, 'PROC')
  if currentProcNum != -1 {
    print "ERROR: cannot define nested procs\n"
    exit
  }
  setCurrentProcNum(procName)

  // if next token is (, advance parser, read parameters until )
  // TODO: allow skipping parens if no params
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
  currentProcNum = -1

  print "__exit_of_" print procName print ":\n"
  print "  mov RSP, RBP\n"
  print "  pop RBP\n"
  print "  ret\n"
  emitLabel(afterProc)
}

parseProcSignature: proc(procName: string) {
  // if next token is (, advance parser, read parameters until )
  expectToken(TOKEN_LPAREN, '(')

  // Parse params
  offset = 8 // first 8 bytes is for return address
  myProcNum = numProcs
  paramIndex = myProcNum * PARAMS_PER_PROC
  index = 0
  while lexTokenType != TOKEN_RPAREN {
    if lexTokenType != TOKEN_VARIABLE {
      print "ERROR: expected variable but found: " printToken()
      exit
    }
    if numParams[myProcNum] == PARAMS_PER_PROC {
      print "ERROR: More than 4 parameters declared for proc " print procName print "\n"
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

  // if next token is :, read return type
  returnType = TYPE_VOID
  if lexTokenType == TOKEN_COLON {
    advanceParser()  // eat the :
    returnType = parseType()
  }
  registerProc(procName, returnType)
  // print "; procs: " print procNames print "\n"
  // print "; numParams: " print numParams print "\n"
  // print "; return types: " print returnTypes print "\n"
  // print "; params: " print paramNames print "\n"
  // print "; param types: " print paramTypes print "\n"
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
    print "ERROR: Cannot return outside proc\n"
    exit
  }

  currentProcName = procNames[currentProcNum]
  // if we're at the start of an expression, parse it.
  if isAtStartOfExpression() {
    actualType = expr()
    // check that return types match
    expectedType = returnTypes[currentProcNum]
    if actualType != expectedType {
      print "ERROR: Incorrect return type of '" print currentProcName print "'. Expected "
      print TYPE_NAMES[expectedType]
      print " but found " print TYPE_NAMES[actualType] print "\n"
      exit
    }
  }
  print "  jmp __exit_of_" print currentProcName print "\n"
}

generateAssignment: proc(variable: string, exprType: int): int {
  varType = lookupGlobal(variable)
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
  // TODO: make sure 'variable' is an array
  expectToken(TOKEN_LBRACKET, '[')
  indexType = expr()
  if indexType != TYPE_INT {
    print "ERROR: Array index must be int; was " + TYPE_NAMES[indexType] + "\n"
    exit
  }
  print "  mov RBX, RAX\n"
  print "  shl RBX, 3  ; bytes per element TEMPORARY\n"
  print "  add RBX, 5  ; header\n"
  generateGetVariable(variable)
  print "  add RBX, RAX\n"
  print "  push RBX\n"
  expectToken(TOKEN_RBRACKET, ']')
  expectToken(TOKEN_EQ, '=')

  expr()

  // TODO: make sure exprType matches baseType of the array
  print "  pop RBX\n"
  print "  mov [RBX], RAX  ; array set\n\n"
}

// variable=expression, procname: proc(), procname(), arrayname:type[intexpr]
parseStartsWithVariable: proc() {
  variable = lexTokenString
  advanceParser()  // eat the variable
  if lexTokenType == TOKEN_EQ {
    advanceParser()  // eat the eq

    exprType = expr()
    varType = generateAssignment(variable, exprType)

    if varType != exprType {
      print "ERROR: Type mismatch: '" print variable print "' is " print TYPE_NAMES[varType]
      print " but expression is " print TYPE_NAMES[exprType] print "\n"
      exit
    }

    return
  } elif lexTokenType == TOKEN_COLON {
    advanceParser() // eat the colon
    if lexTokenType == TOKEN_KEYWORD and lexTokenKw == KW_PROC {
      parseProc(variable)
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
  }
  print "ERROR: expected one of '=' ':' '(' '[' but found: " printToken()
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
    print "ERROR: Expected boolean condition in if but found " print TYPE_NAMES[condType] print "\n"
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
      print "ERROR: Expected boolean condition in elif but found " print TYPE_NAMES[condType] print "\n"
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
    print "ERROR: Cannot have break outside while loop\n"
    exit
  }
  print "  jmp " print whileBreakLabels[numWhiles - 1] print "\n"
}

parseContinue: proc() {
  if numWhiles == 0 {
    print "ERROR: Cannot have continue outside while loop\n"
    exit
  }
  print "  jmp " print whileContinueLabels[numWhiles - 1] print "\n"
}

parseWhile: proc() {
  continueLabel = nextLabel("while_continue")
  emitLabel(continueLabel)

  condType = expr()
  if condType != TYPE_BOOL {
    print "ERROR: Expected boolean as 'while' condition, but found " print TYPE_NAMES[condType] print "\n"
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
  if exprType == TYPE_STRING {
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
  }
  print "  sub RSP, 0x20\n"
  print "  extern printf\n"
  print "  call printf  ; print " print TYPE_NAMES[exprType] print "\n"
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

  print "ERROR: Cannot parse start of statement token: "  printToken()
  exit
}


///////////////////////////////////////////////////////////////////////////////
// MAIN LOOP & OUTPUT ROUTINES
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
  lastVariable=''
  while lexTokenType != TOKEN_EOF {
    if lexTokenType == TOKEN_VARIABLE {
      variable = lexTokenString
      advanceParser() // eat the variable
      if lexTokenType == TOKEN_COLON {
        advanceParser() // eat th ecolon
        if lexTokenKw == KW_PROC {
          advanceParser() // eat "proc"
          parseProcSignature(variable)
          continue
        }
      }
    }
    advanceParser()
  }
}

initParser: proc() {
  text = input
  if debug {
//    print "text = " print text print "\n"
  }
  newLexer(text)
  advanceParser()
}


initParser()
procFinder()
resetLexer()
advanceParser()
parseProgram()
