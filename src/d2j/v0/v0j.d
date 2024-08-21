debug=false

///////////////////////////////////////////////////////////////////////////////
//                                     LEXER                                 //
///////////////////////////////////////////////////////////////////////////////
TOKEN_EOF=0
TOKEN_PLUS=1
TOKEN_MINUS=2
TOKEN_MULT=3
//TOKEN_BIT_AND=4 // bit and
//TOKEN_BIT_OR=5  // bit or
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
//TOKEN_BIT_XOR=32 // bit xor

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
KW_AND=19 // boolean and keyword
KW_OR=20  // boolean or keyword
KW_NOT=21 // boolean not keyword
KW_RECORD=22
KW_NEW=23
KW_PRINTLN=24

KEYWORDS:string[25]
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
KEYWORDS[KW_RECORD]="record"
KEYWORDS[KW_NEW]="new"
KEYWORDS[KW_PRINTLN]="println"

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
  // TODO: do not allow leading _
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
  advanceLex() // eat the !
  if lexerCc == 61 {
    advanceLex()
    return Token(TOKEN_NEQ, '!=')
  }
  print 'Unknown character:' print chr(lexerCc) print ' ASCII code: ' print lexerCc
  print " @ " print lexerLoc print "\n"
  exit
  return ""
}

startsWithGt: proc(): string {
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
    print " @ " print lexerLoc print "\n"
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
  }

  print 'ERROR: Unknown character:' print chr(lexerCc) print ' ASCII code: ' print lexerCc
  print " @ " print lexerLoc print "\n"
  exit
  return ""
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

D_TYPE_NAMES:string[10]
D_TYPE_NAMES[TYPE_UNKNOWN] = "unknown"
D_TYPE_NAMES[TYPE_INT] = "int"
D_TYPE_NAMES[TYPE_BOOL] = "bool"
D_TYPE_NAMES[TYPE_STRING] = "string"
D_TYPE_NAMES[TYPE_INT_ARRAY] = "int[]"
D_TYPE_NAMES[TYPE_BOOL_ARRAY] = "bool[]"
D_TYPE_NAMES[TYPE_STRING_ARRAY] = "string[]"
D_TYPE_NAMES[TYPE_VOID] = "void"

TYPE_NAMES:string[10]
TYPE_NAMES[TYPE_UNKNOWN] = "unknown"
TYPE_NAMES[TYPE_INT] = "int"
TYPE_NAMES[TYPE_BOOL] = "boolean"
TYPE_NAMES[TYPE_STRING] = "String"
TYPE_NAMES[TYPE_INT_ARRAY] = "int[]"
TYPE_NAMES[TYPE_BOOL_ARRAY] = "boolean[]"
TYPE_NAMES[TYPE_STRING_ARRAY] = "String[]"
TYPE_NAMES[TYPE_VOID] = "void"

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
    print "@ " print lexerLoc
    exit
  }
  advanceParser() // eat the expected token
}

expectKeyword: proc(expectedKwType: int, tokenStr: string) {
  if lexTokenType != TOKEN_KEYWORD or lexTokenKw != expectedKwType {
    print "ERROR: expected '" print tokenStr print "' but found: " printToken()
    print "@ " print lexerLoc
    exit
  }
  advanceParser() // eat the keyword
}


///////////////////////////////////////////////////////////////////////////////
// SYMBOL TABLES
///////////////////////////////////////////////////////////////////////////////

numGlobals = 0
MAX_GLOBALS=200 // currently v0.d has > 100 globals
globalNames: string[MAX_GLOBALS]
globalTypes: int[MAX_GLOBALS]

registerGlobal: proc(name: string, type: int) {
  if type == TYPE_UNKNOWN {
    print "ERROR: Cannot register global '"  print name print "' with unknown type\n"
    print " @ " print lexerLoc print "\n"
    exit
  }
  i = 0 while i < numGlobals do i = i + 1 {
    if globalNames[i] == name {
      return
    }
  }
  if debug {
    print "// Adding global name " + name print "\n"
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


// Stores global statements (that will live in main)
// This file maxes out at about 1200
globBuffer: string[2000]

// Stores procedure definitions
procBufferIndex = 0
// This file maxes out at around 12k.
procBuffer: string[20000]

bufferIndex = 0
emitBuffer: string[]
emitBuffer=globBuffer

MAX_NUM_PROCS = 100 // currently v0.d has ~60 procs
numProcs = 0
procNames: string[MAX_NUM_PROCS]
returnTypes: int[MAX_NUM_PROCS]

numParams: int[MAX_NUM_PROCS]
PARAMS_PER_PROC = 4
// These are sparse arrays; the start index for the 0th param of each proc is 4 * proc num
paramNames: string[MAX_NUM_PROCS * PARAMS_PER_PROC]
paramTypes: int[MAX_NUM_PROCS * PARAMS_PER_PROC]

numLocals: int[MAX_NUM_PROCS]
LOCALS_PER_PROC = 10
// These are sparse arrays; the start index for the 0th local of each proc is 10 * proc num
localNames: string[MAX_NUM_PROCS * LOCALS_PER_PROC]
localTypes: int[MAX_NUM_PROCS * LOCALS_PER_PROC]

currentProcNum = -1


// WEIRD If this is above, it doesn't get read (?!)
emit: proc(line: string) {
  // print "// " print line print "\n"
  emitBuffer[bufferIndex] = line
  bufferIndex = bufferIndex+1
}

spoolBuffer: proc(buffer:string[], start:int, end:int) {
  i=start while i < end do i=i+1 {
    emit(buffer[i])
  }
}

registerProc: proc(name: string, returnType: int) {
  if returnType == TYPE_UNKNOWN {
    print "ERROR: Cannot have unknown proc return type\n"
    print " @ " print lexerLoc print "\n"
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
  print " @ " print lexerLoc print "\n"
  exit
}

lookupProcReturnType: proc(name: string): int {
  i = 0 while i < numProcs do i = i + 1 {
    if name == procNames[i] {
      return returnTypes[i]
    }
  }
  print "ERROR: Cannot find proc '" print name print "'\n"
  print " @ " print lexerLoc print "\n"
  exit
  return -1
}

// returns the index of the param in the arrays
lookupParam: proc(name: string): int {
  if currentProcNum == -1 {
    print "ERROR: Cannot lookup parameter " print name print " because not in a proc"
    print " @ " print lexerLoc print "\n"
    exit
    return -1
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
    print " @ " print lexerLoc print "\n"
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


///////////////////////////////////////////////////////////////////////////////
// CODEGEN UTILS
///////////////////////////////////////////////////////////////////////////////


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
      emit(" || ")
      boolAnd()
    }
  }
  return leftType
}

boolAnd: proc(): int {
  leftType = compare()
  if lexTokenType == TOKEN_KEYWORD and leftType == TYPE_BOOL {
    while lexTokenKw == KW_AND {
      advanceParser() // eat the symbol
      emit(" && ")
      compare()
    }
  }
  return leftType
}

compare: proc(): int {
  leftType = addSub()
  opstring = lexTokenString
  if leftType == TYPE_INT and (lexTokenType >= TOKEN_EQEQ and lexTokenType <= TOKEN_GEQ) {
    advanceParser() // eat the symbol
    emit(" ") emit(opstring) emit(" ")
    addSub()
    // TODO: check types
    return TYPE_BOOL
  }
  if leftType == TYPE_STRING and (lexTokenType == TOKEN_EQEQ or lexTokenType == TOKEN_NEQ) {
    advanceParser() // eat the symbol

    addSubBuffer:string[100] // yes just 100

    oldEmitBuffer=emitBuffer
    emitBuffer=addSubBuffer
    oldBufferIndex = bufferIndex
    bufferIndex=0

    rightType = addSub()

    count=bufferIndex
    emitBuffer=oldEmitBuffer
    bufferIndex=oldBufferIndex

    if rightType == TYPE_VOID {
      // use primitive comparison
      emit(" ") emit(opstring) emit(" null")
    } else {
      // TODO: check types
      emit(".compareTo(")
      spoolBuffer(addSubBuffer, 0, count)
      emit(") ")
      // if EQEQ, becomes compareTo(foo) == 0
      emit(opstring) emit(" 0")
    }
    return TYPE_BOOL
  }
  return leftType
}

addSub: proc(): int {
  leftType = mulDiv()
  while lexTokenType == TOKEN_PLUS or lexTokenType == TOKEN_MINUS {
    opstring = lexTokenString
    advanceParser() // eat the symbol
    emit(" ") emit(opstring) emit(" ") 
    rightType = mulDiv()
    if leftType != rightType {
      print "ERROR: Type mismatch. Left operand is " print TYPE_NAMES[leftType]
      print ", but right operand is " print TYPE_NAMES[rightType] print "\n"
      print "@ " print lexerLoc print "\n"
      exit
    }
    if leftType == TYPE_BOOL {
      print "ERROR: Cannot add or subtract booleans\n"
      print "@ " print lexerLoc print "\n"
      exit
    }
    if leftType == TYPE_STRING and lexTokenType == TOKEN_MINUS {
      print "ERROR: Cannot subtract strings\n"
      print "@ " print lexerLoc print "\n"
      exit
    }
  }
  return leftType
}

mulDiv: proc(): int {
  leftType = unary()
  while leftType == TYPE_INT and
      (lexTokenType == TOKEN_MULT or lexTokenType == TOKEN_DIV or lexTokenType == TOKEN_MOD) {
    opstring = lexTokenString
    advanceParser() // eat the symbol
    emit(" ") emit(opstring) emit(" ") 
    unary()
  }
  return leftType
}

unary: proc(): int {
  type: int
  if lexTokenType == TOKEN_PLUS {
    advanceParser() // eat the plus
    return unary()
  } elif lexTokenType == TOKEN_MINUS {
    advanceParser() // eat the minus
    // change it

    unaryBuffer:string[100] // yes just 100

    oldEmitBuffer=emitBuffer
    emitBuffer=unaryBuffer
    oldBufferIndex = bufferIndex
    bufferIndex=0

    type=unary()

    count=bufferIndex
    emitBuffer=oldEmitBuffer
    bufferIndex=oldBufferIndex

    if type == TYPE_INT {
      emit("-")
      spoolBuffer(unaryBuffer, 0, count)
      return type
    }
    print "ERROR: cannot codegen negative non-ints yet\n"
    print "@ " print lexerLoc print "\n"
    exit
  } elif lexTokenType == TOKEN_KEYWORD and lexTokenKw == KW_LENGTH {
    advanceParser() // eat the length
    expectToken(TOKEN_LPAREN, '(')
    type = expr()
    expectToken(TOKEN_RPAREN, ')')
    if type == TYPE_STRING {
      emit(".length()")
    } elif isArrayType(type) {
      emit(".length")
    } else {
      print "ERROR: Cannot take LENGTH of " print TYPE_NAMES[type] print "\n"
      print " @ " print lexerLoc print "\n"
      exit
    }

    return TYPE_INT
  } elif lexTokenType == TOKEN_KEYWORD and lexTokenKw == KW_ASC {
    advanceParser() // eat the asc
    expectToken(TOKEN_LPAREN, '(')
    type = expr()
    emit(".charAt(0)") // fun fact, it will automatically convert ot an int
    expectToken(TOKEN_RPAREN, ')')
    if type != TYPE_STRING {
      print "ERROR: Cannot take ASC of " print TYPE_NAMES[type] print "\n"
      print " @ " print lexerLoc print "\n"
      exit
    }

    return TYPE_INT
  } elif lexTokenType == TOKEN_KEYWORD and lexTokenKw == KW_CHR {
    advanceParser() // eat the chr

    expectToken(TOKEN_LPAREN, '(')
    emit('Character.toString(')
    type = expr()
    emit(')')
    expectToken(TOKEN_RPAREN, ')')

    if type != TYPE_INT {
      print "ERROR: Cannot take CHR of " print TYPE_NAMES[type] print "\n"
      print " @ " print lexerLoc print "\n"
      exit
    }

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

  emit("[")
  indexType = expr()
  if indexType != TYPE_INT {
    print "ERROR: Array index must be int; was " print TYPE_NAMES[indexType] print "\n"
    print " @ " print lexerLoc print "\n"
    exit
  }
  expectToken(TOKEN_RBRACKET, ']')
  emit("]")

  // TODO: make sure index > 0 and < length

  return baseType
}

// Generate a "get" of foo[int] for a string
generateStringIndex: proc() {
  // we've already emitted the variable name

  emit(".substring(")

  exprBuffer:string[100]
  oldEmitBuffer=emitBuffer
  emitBuffer=exprBuffer
  oldBufferIndex = bufferIndex
  bufferIndex=0

  indexType = expr()
  expectToken(TOKEN_RBRACKET, ']')

  count=bufferIndex
  emitBuffer=oldEmitBuffer
  bufferIndex = oldBufferIndex

  if indexType != TYPE_INT {
    print "ERROR: String index must be int; was " print TYPE_NAMES[indexType] print "\n"
    print " @ " print lexerLoc print "\n"
    exit
  }
  spoolBuffer(exprBuffer, 0, count)
  emit(", ")
  spoolBuffer(exprBuffer, 0, count)
  emit(" + 1)")
  // TODO: check for index > 0 and index < string length
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

    print "ERROR: Cannot take index of " print TYPE_NAMES[leftType] print "\n"
    print " @ " print lexerLoc print "\n"
    exit
  }
  return leftType
}

generateGetVariable: proc(variable: string): int {
  varType = lookupGlobal(variable)
  if varType != TYPE_UNKNOWN {
    emit(variable)
    return varType
  }
  if currentProcNum == -1 {
    print "ERROR: Cannot find global variable " print variable print "\n"
    print " @ " print lexerLoc print "\n"
    exit
  }
  index = lookupLocal(variable)
  if index != -1 {             
    emit(variable)
    return localTypes[index]   
  }                            
                               
  index = lookupParam(variable)
  if index == -1 {             
    print "ERROR: Cannot find param " print variable print "\n"
    print " @ " print lexerLoc print "\n"
    exit                                                     
  }                                                          
  emit(variable)
  return paramTypes[index]                                   
}              

generateProcCall: proc(procname: string) {
  emit(procname)
  expectToken(TOKEN_LPAREN, '(')
  emit("(")

  numArgs = 0
  while lexTokenType != TOKEN_RPAREN and lexTokenType != TOKEN_EOF {
    numArgs = numArgs + 1
    argType = expr()
    // TODO: check types
    if lexTokenType == TOKEN_COMMA {
      emit(",")
      advanceParser() // eat the comma
    }
  }

  expectToken(TOKEN_RPAREN, ')')
  emit(")")
}

needsInput=false

generateInput: proc() {
  emit("__d2_input()")
  needsInput=true
}

outputInput: proc() {
  print '\r
  private static String __d2_input() {\r
    String input = "";\r
    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));\r
    try {\r
      String line = reader.readLine();\r
      while (line != null) {\r
        input += line + "\\n";\r
        line = reader.readLine();\r
      }\r
    } catch (IOException e) {\r
      throw new RuntimeException("Could not read standard in", e);\r
    }\r
    return input;\r
  }\n'
}


// atom -> constant | variable | variable '(' args ')' | '(' expr ')'
atom: proc(): int {
  if lexTokenType == TOKEN_STRING {
    // string constant
    // need to escape it
    emit('"') 
    s=lexTokenString
    i=0 while i < length(s) do i=i+1 {
      c=s[i]
      if c == '\n' {
        emit('\\n')
        continue
      } elif c == '\r' {
        emit('\\r')
        continue
      } elif c == '"' {
        emit('\\"')
        continue
      } elif c == '\\' {
        emit('\\\\')
        continue
      }
      emit(c)
    }
    emit('"')
    advanceParser()
    return TYPE_STRING

  } elif lexTokenType == TOKEN_INT {
    // int constant
    intval = lexTokenString
    advanceParser()
    emit(intval)
    return TYPE_INT

  } elif lexTokenType == TOKEN_BOOL {
    // bool constant
    boolval = lexTokenBool
    advanceParser()
    if boolval {
      emit("true")
    } else {
      emit("false")
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
      print " @ " print lexerLoc print "\n"
      exit
    }

    return type

  } elif lexTokenType == TOKEN_LPAREN {

    // (expr)
    expectToken(TOKEN_LPAREN, '(')
    emit("(")
    exprType = expr()
    expectToken(TOKEN_RPAREN, ')')
    emit(")")

    return exprType
  } elif lexTokenType == TOKEN_KEYWORD and lexTokenKw == KW_INPUT {
    advanceParser() // eat the input
    generateInput()
    return TYPE_STRING
  } elif lexTokenType == TOKEN_KEYWORD and lexTokenKw == KW_NULL {
    advanceParser() // eat the input
    emit("null")
    return TYPE_VOID
  }

  print "ERROR: cannot parse token in atom(): " printToken()
  print " @ " print lexerLoc print "\n"
  exit
  return -1
}


///////////////////////////////////////////////////////////////////////////////
// STATEMENT RULES
///////////////////////////////////////////////////////////////////////////////

parseType: proc(): int {
  i = 1 while i <= 3 do i = i + 1 {
    if D_TYPE_NAMES[i] == lexTokenString {
      advanceParser()
      return i
    }
  }
  print "ERROR: Unknown type " printToken()
  print " @ " print lexerLoc print "\n"
  exit
  return -1
}


// Variable declaration; already saw the variable and the colon
// Might be a:int or a:int[] or a:int[3]
parseVarDecl: proc(variable: string) {
  baseType = parseType()
  if lexTokenType != TOKEN_LBRACKET {
    // just a:int
    registerOrLookUpVariable(variable, baseType)
    return
  }

  arrayType = baseType + TYPE_ARRAY
  expectToken(TOKEN_LBRACKET, '[')
  if lexTokenType == TOKEN_RBRACKET {
    expectToken(TOKEN_RBRACKET, ']')
    // empty size declaration
    // array:int[]
    registerOrLookUpVariable(variable, arrayType)
    return
  }

  exprBuffer:string[100]
  oldEmitBuffer=emitBuffer
  emitBuffer=exprBuffer
  oldBufferIndex = bufferIndex
  bufferIndex=0

  sizeType = expr()

  count=bufferIndex
  emitBuffer=oldEmitBuffer
  bufferIndex = oldBufferIndex

  if sizeType != TYPE_INT {
    print "ERROR: Array size must be an int, but was " print TYPE_NAMES[sizeType] print "\n"
    print " @ " print lexerLoc print "\n"
    exit
  }
  expectToken(TOKEN_RBRACKET, ']')

  registerOrLookUpVariable(variable, arrayType)
  emit(variable) emit(' = new ') emit(TYPE_NAMES[baseType]) 
  emit('[')
  spoolBuffer(exprBuffer, 0, count)
  emit('];\n')
}

// Procedure declaration
parseProc: proc(procName: string) {
  expectKeyword(KW_PROC, 'PROC')
  if currentProcNum != -1 {
    print "ERROR: cannot define nested procs\n"
    print " @ " print lexerLoc print "\n"
    exit
  }
  setCurrentProcNum(procName)

  // if next token is (, advance parser, read parameters until )
  expectToken(TOKEN_LPAREN, '(')

  // Parse params (but not really; they've already been added to the symbol table)
  while lexTokenType != TOKEN_RPAREN {
    expectToken(TOKEN_VARIABLE, 'variable')
    expectToken(TOKEN_COLON, ':')
    parseType()
    if lexTokenType == TOKEN_LBRACKET {
      expectToken(TOKEN_LBRACKET, '[')
      expectToken(TOKEN_RBRACKET, ']')
    }

    if lexTokenType == TOKEN_COMMA {
      advanceParser() // eat the comma
    } else {
      break
    }
  }

  expectToken(TOKEN_RPAREN, ')')

  oldBufferIndex = bufferIndex
  oldBuffer = emitBuffer
  emitBuffer = procBuffer
  bufferIndex = procBufferIndex

  // if next token is :, read return type
  returnType = TYPE_VOID
  if lexTokenType == TOKEN_COLON {
    advanceParser()  // eat the :
    returnType = parseType()
  }

  emit("\n  private static ") emit(TYPE_NAMES[returnType]) emit(" ") emit(procName) emit("(")
  i = 0 while i < numParams[currentProcNum] do i=i+1 {
    emit(TYPE_NAMES[paramTypes[currentProcNum*4+i]]) emit(" ") emit(paramNames[currentProcNum*4+i])
    if i < numParams[currentProcNum] - 1 {
      emit(", ")
    }
  }
  
  emit(") ")

  parseBlock(true)
  currentProcNum = -1

  emit("\n")
  procBufferIndex = bufferIndex
  emitBuffer = oldBuffer
  bufferIndex = oldBufferIndex
}

parseProcSignature: proc(procName: string) {
  // if next token is (, advance parser, read parameters until )
  expectToken(TOKEN_LPAREN, '(')

  // Parse params
  myProcNum = numProcs
  paramIndex = myProcNum * PARAMS_PER_PROC
  index = 0
  while lexTokenType != TOKEN_RPAREN {
    if lexTokenType != TOKEN_VARIABLE {
      print "ERROR: expected variable but found: " printToken()
      print " @ " print lexerLoc print "\n"
      exit
    }
    if numParams[myProcNum] == PARAMS_PER_PROC {
      print "ERROR: More than 4 parameters declared for proc " print procName print "\n"
      print " @ " print lexerLoc print "\n"
      exit
    }
    paramName = lexTokenString
    advanceParser() // eat the param name

    expectToken(TOKEN_COLON, ':')

    type = parseType()
    if lexTokenType == TOKEN_LBRACKET {
      expectToken(TOKEN_LBRACKET, '[')
      expectToken(TOKEN_RBRACKET, ']')
      type = type + TYPE_ARRAY
    }

    // store the name and type of the parameter
    paramNames[paramIndex] = paramName
    paramTypes[paramIndex] = type
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
  // print "registering proc " print procName print "\n"
  registerProc(procName, returnType)
  // print "; procs: " print procNames print "\n"
  // print "; numParams: " print numParams print "\n"
  // print "; return types: " print returnTypes print "\n"
  // print "; params: " print paramNames print "\n"
  // print "; param types: " print paramTypes print "\n"
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
    print " @ " print lexerLoc print "\n"
    exit
  }

  currentProcName = procNames[currentProcNum]
  // if we're at the start of an expression, parse it.
  emit("return")
  if isAtStartOfExpression() {
    emit(" ")
    actualType = expr()
    // check that return types match
    expectedType = returnTypes[currentProcNum]
    if actualType != expectedType {
      print "ERROR: Incorrect return type of '" print currentProcName print "'. Expected "
      print TYPE_NAMES[expectedType]
      print " but found " print TYPE_NAMES[actualType] print "\n"
      print " @ " print lexerLoc print "\n"
      exit
    }
  }
  emit(";\n")
}

registerLocal: proc(name: string, type: int) {
  if type == TYPE_UNKNOWN {
    print "ERROR: Cannot register local '" print name print "' with unknown type\n"
    print " @ " print lexerLoc print "\n"
    exit
  }
  myLocalCount = numLocals[currentProcNum]
  if myLocalCount == LOCALS_PER_PROC {
    print "ERROR: Too many locals. Max is " print LOCALS_PER_PROC print "\n"
    print " @ " print lexerLoc print "\n"
    exit
  }

  // These are sparse arrays; the start index for the 0th local of each proc is 10 * proc num
  base = currentProcNum * LOCALS_PER_PROC + myLocalCount

  localNames[base] = name
  localTypes[base] = type
  numLocals[currentProcNum] = myLocalCount + 1
}


registerOrLookUpVariable: proc(variable: string, exprType: int): int {
  varType = lookupGlobal(variable)
  isGlobal = varType != TYPE_UNKNOWN or currentProcNum == -1
  if isGlobal {
    if varType == TYPE_UNKNOWN {
      registerGlobal(variable, exprType)
      varType = exprType
      print("  private static ") print(TYPE_NAMES[varType]) print(" ") print(variable) print ";\n"
      indent()
    } 
    return varType
  }

  // not global; try param or local
  index = lookupParam(variable)
  if index != -1 {
    // found it
    varType = paramTypes[index]
    return varType
  }

  index = lookupLocal(variable)
  if index != -1 {
    varType = localTypes[index]
  } else {
    // declare local
    registerLocal(variable, exprType)
    varType = exprType
    emit(TYPE_NAMES[varType]) emit(" ") emit(variable) emit(";\n")
    indent()
  }
  return varType
}

generateArraySet: proc(variable: string) {
  // TODO: make sure 'variable' is an array
  emit(variable)
  emit('[')
  expectToken(TOKEN_LBRACKET, '[')
  indexType = expr()
  if indexType != TYPE_INT {
    print "ERROR: Array index must be int; was " print TYPE_NAMES[indexType] print "\n"
    print " @ " print lexerLoc print "\n"
    exit
  }
  expectToken(TOKEN_RBRACKET, ']')
  emit(']')
  expectToken(TOKEN_EQ, '=')
  emit('=')

  expr()
  emit(';\n')
  // TODO: make sure exprType matches baseType of the array
}

// variable=expression, procname: proc(), procname(), arrayname:type[intexpr]
parseStartsWithVariable: proc(semi: bool) {
  variable = lexTokenString
  advanceParser()  // eat the variable
  if lexTokenType == TOKEN_EQ {
    advanceParser()  // eat the eq

    exprBuffer:string[100]
    oldEmitBuffer=emitBuffer
    emitBuffer=exprBuffer
    oldBufferIndex = bufferIndex
    bufferIndex=0

    exprType = expr()

    count=bufferIndex
    emitBuffer=oldEmitBuffer
    bufferIndex = oldBufferIndex

    // this may declare the variable
    // we have to wait until now to output the LHS because we don't know the RHS type to 
    varType = registerOrLookUpVariable(variable, exprType)
    emit(variable)
    emit(' = ')
    spoolBuffer(exprBuffer, 0, count)
    if semi { emit(';\n') }

    if varType != exprType {
      print "ERROR: Type mismatch: '" print variable print "' is " print TYPE_NAMES[varType]
      print " but expression is " print TYPE_NAMES[exprType] print "\n"
      print " @ " print lexerLoc print "\n"
      exit
    }

    return
  } elif lexTokenType == TOKEN_COLON {
    advanceParser() // eat the colon
    if lexTokenType == TOKEN_KEYWORD and lexTokenKw == KW_PROC {
      parseProc(variable)
    } else {
      parseVarDecl(variable)
    }
    return
  } elif lexTokenType == TOKEN_LPAREN {
    generateProcCall(variable)
    if semi { emit(';\n') }
    return
  } elif lexTokenType == TOKEN_LBRACKET {
    // array set
    generateArraySet(variable)
    return
  }
  print "ERROR: expected one of '=' ':' '(' '[' but found: " printToken()
  print " @ " print lexerLoc print "\n"
  exit
}

indentSize = 0
indent:proc() {
  i = 0 while i < indentSize do i=i+1 { emit("  ") }
}

// expect {, parse statements until }
parseBlock: proc(emitBraces: bool) {
  expectToken(TOKEN_LBRACE, '{')
  if emitBraces {
    emit('{\n')
  }
  indentSize = indentSize + 1
  while lexTokenType != TOKEN_RBRACE and lexTokenType != TOKEN_EOF {
    indent()
    parseStmt(true)
  }
  expectToken(TOKEN_RBRACE, '}')
  indentSize = indentSize - 1
  if emitBraces {
    indent()
    emit('}\n')
  }
}

parseIf: proc() {
  // 1. calculate the condition
  emit('if (')
  condType = expr()

  if condType != TYPE_BOOL {
    print "ERROR: Expected boolean condition in if but found " print TYPE_NAMES[condType] print "\n"
    print " @ " print lexerLoc print "\n"
    exit
  }
  emit(') ')

  // we could futz with the indentation, but, eh.
  parseBlock(true)

  // this may not be necessary if there are no elses or elifs
  // after the successful "if" block, jump down to the end.
  while lexTokenType == TOKEN_KEYWORD and lexTokenKw == KW_ELIF {
    indentSize = indentSize - 1
    indent()
    indentSize = indentSize + 1
    emit('  else if (')
    advanceParser() // eat the elif
    condType = expr()
    emit(') ')
    if condType != TYPE_BOOL {
      print "ERROR: Expected boolean condition in elif but found " print TYPE_NAMES[condType] print "\n"
      print " @ " print lexerLoc print "\n"
      exit
    }
    parseBlock(true)
  }

  // 5. else:
  if lexTokenType == TOKEN_KEYWORD and lexTokenKw == KW_ELSE {
    indentSize = indentSize - 1
    indent()
    indentSize = indentSize + 1
    emit('  else ')
    advanceParser() // eat the "else"
    parseBlock(true)
  }
}

numWhiles=0

parseBreak: proc() {
  if numWhiles == 0 {
    print "ERROR: Cannot have break outside while loop\n"
    print " @ " print lexerLoc print "\n"
    exit
  }
  emit("break;\n")
}

parseContinue: proc() {
  if numWhiles == 0 {
    print "ERROR: Cannot have continue outside while loop\n"
    print " @ " print lexerLoc print "\n"
    exit
  }
  emit("continue;\n")
}


parseWhile: proc() {
  emit('for (; ')
  condType = expr()
  if condType != TYPE_BOOL {
    print "ERROR: Expected boolean as 'while' condition, but found " print TYPE_NAMES[condType] print "\n"
    print " @ " print lexerLoc print "\n"
    exit
  }
  emit(';')

  if lexTokenType == TOKEN_KEYWORD and lexTokenKw == KW_DO {
    advanceParser() // eat the "do"
    emit(" ")
    parseStmt(false)
  }
  emit(') ')
  parseBlock(true)

  // Pop the while label stack
  numWhiles = numWhiles - 1
}


// note, just print, not println
parsePrint: proc() {
  emit('System.out.print(')
  expr()
  emit(');\n')
}

parseStmt: proc(semi: bool) {
  if lexTokenType == TOKEN_EOF {
    return
  } elif lexTokenType == TOKEN_KEYWORD {
    kw = lexTokenKw
    advanceParser() // eat the token
    if kw == KW_PRINT {
      parsePrint()
      return
    } elif kw == KW_EXIT {
      emit('System.exit(-1);\n')
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
    parseStartsWithVariable(semi)
    return
  }

  print "ERROR: Cannot parse start of statement token: "  printToken()
  print " @ " print lexerLoc print "\n"
  exit
}


///////////////////////////////////////////////////////////////////////////////
// MAIN LOOP & OUTPUT ROUTINES
///////////////////////////////////////////////////////////////////////////////


parseProgram: proc() {
  print "package d2j;\n\n"
  print "import java.io.BufferedReader;\n"
  print "import java.io.IOException;\n"
  print "import java.io.InputStreamReader;\n\n"

  print "public class D2Program {\n"
  indentSize=1

  // cannot use parseblock because we need to insert the "main", below,
  // before the closing brace
  while lexTokenType != TOKEN_EOF {
    indent()
    parseStmt(true)
  }

  i = 0 while i < procBufferIndex do i = i + 1 {
    if procBuffer[i] != null {
      print procBuffer[i]
    }
  }
  if needsInput {
    outputInput()
  }
  print "\n  public static void main(String args[]) {\n"
  i = 0 while i < bufferIndex do i = i + 1 {
    if globBuffer[i] != null {
      print globBuffer[i]
    }
  }
  print "  }\n}\n"
}

procFinder: proc() {
  while lexTokenType != TOKEN_EOF {
    if lexTokenType == TOKEN_VARIABLE {
      variable = lexTokenString
      advanceParser() // eat the variable
      if lexTokenType == TOKEN_COLON {
        //print "variable found " print variable print "\n"
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
  newLexer(text)
  advanceParser()
}


initParser()
procFinder()
resetLexer()
advanceParser()
parseProgram()
