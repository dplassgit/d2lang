debug=false

///////////////////////////////////////////////////////////////////////////////
//                                     LEXER                                 //
///////////////////////////////////////////////////////////////////////////////
TOKEN_EOF=0
TOKEN_PLUS=1
TOKEN_MINUS=2
TOKEN_MULT=3
TOKEN_BIT_AND=4 // bit and
TOKEN_BIT_OR=5  // bit or
TOKEN_BIT_NOT=6 // bit not
TOKEN_BIT_XOR=7 // bit xor
TOKEN_DIV=8
TOKEN_MOD=9
TOKEN_EQEQ=10
TOKEN_NEQ=11
TOKEN_LT=12
TOKEN_GT=13
TOKEN_LEQ=14
TOKEN_GEQ=15
TOKEN_VARIABLE=16
TOKEN_EQ=17
TOKEN_LPAREN=18
TOKEN_RPAREN=19
TOKEN_LBRACE=20
TOKEN_RBRACE=21
TOKEN_COLON=22
TOKEN_COMMA=23
TOKEN_KEYWORD=24
TOKEN_LBRACKET=25
TOKEN_RBRACKET=26
TOKEN_DOT=27
TOKEN_SHIFT_LEFT=28
TOKEN_SHIFT_RIGHT=29
TOKEN_LITERAL_CONSTANT=30
TOKEN_PLUSPLUS=31
TOKEN_MINUSMINUS=32

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
KW_LONG=25  // long keyword
KW_DOUBLE=26  // double keyword
KW_XOR=27 // boolean 'xor' keyword

KEYWORDS:string[KW_XOR+1]
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
KEYWORDS[KW_LONG]="long"
KEYWORDS[KW_DOUBLE]="double"
KEYWORDS[KW_NULL]="null"
KEYWORDS[KW_INPUT]="input"
KEYWORDS[KW_LENGTH]="length"
KEYWORDS[KW_CHR]="chr"
KEYWORDS[KW_ASC]="asc"
KEYWORDS[KW_EXIT]="exit"
KEYWORDS[KW_AND]="and"
KEYWORDS[KW_OR]="or"
KEYWORDS[KW_NOT]="not"
KEYWORDS[KW_XOR]="xor"
KEYWORDS[KW_RECORD]="record"
KEYWORDS[KW_NEW]="new"
KEYWORDS[KW_PRINTLN]="println"

///////////////////////////////////////////////////////////////////////////////
//                                    TYPES
///////////////////////////////////////////////////////////////////////////////

MAX_RECORDS = 20

TYPE_UNKNOWN=0
TYPE_INT=1
TYPE_BOOL=2
TYPE_STRING=3
TYPE_DOUBLE=4
TYPE_LONG=5
TYPE_BYTE=6
TYPE_NULL=7
TYPE_RECORD_BASE=8 // types 8 to 128+MAX_REOCRD are records.
TYPE_ARRAY=TYPE_RECORD_BASE + MAX_RECORDS + 1 // NOTE THIS IS NOT AN OFFICIAL TYPE
TYPE_INT_ARRAY=TYPE_ARRAY+TYPE_INT
TYPE_BOOL_ARRAY=TYPE_ARRAY+TYPE_BOOL
TYPE_STRING_ARRAY=TYPE_ARRAY+TYPE_STRING
TYPE_DOUBLE_ARRAY=TYPE_ARRAY+TYPE_DOUBLE
TYPE_LONG_ARRAY=TYPE_ARRAY+TYPE_LONG
TYPE_BYTE_ARRAY=TYPE_ARRAY+TYPE_BYTE
LAST_TYPE=TYPE_BYTE_ARRAY+1
TYPE_VOID=LAST_TYPE+1

numRecords = 0
recordNames: string[MAX_RECORDS]
numFields: int[MAX_RECORDS]
recordSizes: int[MAX_RECORDS]
FIELDS_PER_RECORD = 20
// These are sparse arrays; the start index for the 0th field of each record is 20 * record num
fieldNames: string[MAX_RECORDS * FIELDS_PER_RECORD]
fieldTypes: int[MAX_RECORDS * FIELDS_PER_RECORD]


isNumeric: proc(type: int): bool {
  return isIntegral(type) or type == TYPE_DOUBLE
}

isIntegral: proc(type: int): bool {
  return type == TYPE_INT or type == TYPE_LONG // or type == TYPE_BYTE
}

isRecordType: proc(type: int): bool {
  return type >= TYPE_RECORD_BASE and type < TYPE_RECORD_BASE + numRecords
}

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
  //if debug {
    // print "; Lexer cc " print lexerCc print ":" print chr(lexerCc) print "\n"
  //}
}

///////////////////////////////////////////////////////////////////////////////
// Lexer token values, for external consumption
///////////////////////////////////////////////////////////////////////////////

lexTokenType=0
lexTokenString=''
lexTokenKw=0
lexTokenVarType=-1

// Bundle the data about the token in a single string of the format
// 't <type> <value>'
Token: proc(type: int, value: string): string {
  lexTokenType = type
  lexTokenString = value
  lexTokenKw = -1
  //if debug {
    //print "; Making token type: " print type print " value: (skipped)\n"
    // print value print "\n"
  //}
  return 't ' + toString(type) + ' ' + value
}

// Bundle the data about the token in a single string of the format
// 'i <value>'
LiteralToken: proc(prefix: string, varType: int, value: string): string {
  lexTokenType = TOKEN_LITERAL_CONSTANT
  lexTokenVarType = varType
  lexTokenString = value
  lexTokenKw = -1
  return prefix + value
}


// Bundle the data about the token in a single string of the format
// 'k value'
KeywordToken: proc(value: int, valueAsString: string): string {
  lexTokenType = TOKEN_KEYWORD
  lexTokenString = valueAsString
  lexTokenKw = value
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
    return LiteralToken("b ", TYPE_BOOL, value)
  }

  i=0 while i < length(KEYWORDS) do i = i + 1 {
    if value == KEYWORDS[i] {
      return KeywordToken(i, value)
    }
  }

  return Token(TOKEN_VARIABLE, value)
}

makeIntToken: proc(): string {
  value = ''

  while isDigit(lexerCc) do advanceLex() {
    value = value + chr(lexerCc)
  }
  if lexerCc == asc('L') {
    advanceLex()
    return LiteralToken("l ", TYPE_LONG, value)
  }
  if lexerCc == 46 {
    value = value + chr(lexerCc)
    advanceLex()
    while isDigit(lexerCc) do advanceLex() {
      value = value + chr(lexerCc)
    }
    return LiteralToken("d ", TYPE_DOUBLE, value)
  }

  return LiteralToken("i ", TYPE_INT, value)
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
  print 'ERROR: Unknown character:' print chr(lexerCc) print ' ASCII code: ' print lexerCc
  print " @ " print lexerLoc print "\n"
  exit
  return ""
}

startsWithGt: proc(): string {
  advanceLex()
  if lexerCc == 61 {
    advanceLex()
    return Token(TOKEN_GEQ, '>=')
  } elif lexerCc == 62 {
    // shift right
    advanceLex()
    return Token(TOKEN_SHIFT_RIGHT, '>>')
  }
  return Token(TOKEN_GT, '>')
}

startsWithLt: proc(): string {
  advanceLex()
  if lexerCc == 61 {
    advanceLex()
    return Token(TOKEN_LEQ, '<=')
  } elif lexerCc == 60 {
    // shift right
    advanceLex()
    return Token(TOKEN_SHIFT_LEFT, '<<')
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

startsWithPlus: proc(): string {
  advanceLex()
  if lexerCc == 43 {
    advanceLex()
    return Token(TOKEN_PLUSPLUS, '++')
  }
  return Token(TOKEN_PLUS, '+')
}

startsWithMinus: proc(): string {
  advanceLex()
  if lexerCc == 45 {
    advanceLex()
    return Token(TOKEN_MINUSMINUS, '--')
  }
  return Token(TOKEN_MINUS, '-')
}

makeStringLiteralToken: proc(firstQuote: int): string {
  advanceLex() // eat the tick/quote
  value=''
  while lexerCc != firstQuote and lexerCc != 0 {
    if lexerCc == 92 { // backslash
      advanceLex()
      if lexerCc == 110 { // backslash - n
        value=value + chr(10)  // linefeed
      } elif lexerCc == 92 {
        value=value + chr(92)  // literal backslash
      }
    } else {
      value=value + chr(lexerCc)
    }
    advanceLex()
  }

  if lexerCc == 0 {
    print 'ERROR: Unclosed string literal ' print value print "\n"
    print " @ " print lexerLoc print "\n"
    exit
  }

  advanceLex() // eat the closing tick/quote
  return LiteralToken("s ", TYPE_STRING, value)
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
    return startsWithPlus()
  } elif oc == 45 {
    return startsWithMinus()
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
  } elif oc == 38 {
    advanceLex()
    return Token(TOKEN_BIT_AND, '&')
  } elif oc == 124 {
    advanceLex()
    return Token(TOKEN_BIT_OR, '|')
  } elif oc == 94 {
    advanceLex()
    return Token(TOKEN_BIT_XOR, '^')
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
  } elif oc == 46 {
    advanceLex()
    return Token(TOKEN_DOT, '.')
  }

  print 'ERROR: Unknown character:' print chr(lexerCc) print ' ASCII code: ' print lexerCc
  print " @ " print lexerLoc print "\n"
  exit
  return ""
}

printToken: proc() {
  if lexTokenType == TOKEN_EOF {
    print 'Token: EOF' print "\n"
  } elif lexTokenType == TOKEN_LITERAL_CONSTANT {
    if lexTokenVarType == TYPE_INT { print 'Int token: ' print lexTokenString print "\n" }
    if lexTokenVarType == TYPE_BOOL { print 'Bool token: ' print lexTokenString print "\n" }
    if lexTokenVarType == TYPE_STRING { print 'String token: ' print lexTokenString print "\n" }
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

D_TYPE_NAMES:string[TYPE_VOID+1]
D_TYPE_NAMES[TYPE_UNKNOWN] = "unknown"
D_TYPE_NAMES[TYPE_INT] = "int"
D_TYPE_NAMES[TYPE_BOOL] = "bool"
D_TYPE_NAMES[TYPE_BYTE] = "byte"
D_TYPE_NAMES[TYPE_STRING] = "string"
D_TYPE_NAMES[TYPE_INT_ARRAY] = "int[]"
D_TYPE_NAMES[TYPE_BOOL_ARRAY] = "bool[]"
D_TYPE_NAMES[TYPE_STRING_ARRAY] = "string[]"
D_TYPE_NAMES[TYPE_VOID] = "void"
D_TYPE_NAMES[TYPE_NULL] = "null"
D_TYPE_NAMES[TYPE_DOUBLE] = "double"
D_TYPE_NAMES[TYPE_LONG] = "long"
D_TYPE_NAMES[TYPE_LONG_ARRAY] = "long[]"
D_TYPE_NAMES[TYPE_DOUBLE_ARRAY] = "double[]"
D_TYPE_NAMES[TYPE_BYTE_ARRAY] = "byte[]"

TYPE_NAMES:string[TYPE_VOID+1]
TYPE_NAMES[TYPE_UNKNOWN] = "unknown"
TYPE_NAMES[TYPE_INT] = "int"
TYPE_NAMES[TYPE_BOOL] = "boolean"
TYPE_NAMES[TYPE_STRING] = "String"
TYPE_NAMES[TYPE_INT_ARRAY] = "int[]"
TYPE_NAMES[TYPE_BOOL_ARRAY] = "boolean[]"
TYPE_NAMES[TYPE_STRING_ARRAY] = "String[]"
TYPE_NAMES[TYPE_VOID] = "void"
TYPE_NAMES[TYPE_NULL] = "null"
TYPE_NAMES[TYPE_DOUBLE] = "double"
TYPE_NAMES[TYPE_LONG] = "long"
TYPE_NAMES[TYPE_LONG_ARRAY] = "long[]"
TYPE_NAMES[TYPE_DOUBLE_ARRAY] = "double[]"

typeName: proc(type: int): string {
  if isRecordType(type) {
    return recordNames[type - TYPE_RECORD_BASE]
  }
  return TYPE_NAMES[type]
}


checkTypes: proc(leftType: int, rightType: int) {
  if leftType != rightType {
    print "ERROR: Type mismatch. Left operand is " print typeName(leftType)
    print ", but right operand is " print typeName(rightType)
    print " @ " print lexerLoc print "\n"
    exit
  }
}

///////////////////////////////////////////////////////////////////////////////
//                                    PARSER                                 //
///////////////////////////////////////////////////////////////////////////////

advanceParser: proc() {
  nextToken()
  //if debug {
    //print "; new token is " + lexTokenString print "\n"
  //}
}

expectToken: proc(expectedTokenType: int, tokenStr: string) {
  if lexTokenType != expectedTokenType  {
    print "ERROR: expected " + tokenStr + "; saw: " printToken()
    print "@ " print lexerLoc
    exit
  }
  advanceParser() // eat the expected token
}

expectKeyword: proc(expectedKwType: int, tokenStr: string) {
  if lexTokenType != TOKEN_KEYWORD or lexTokenKw != expectedKwType {
    print "ERROR: expected " print tokenStr print "; saw: " printToken()
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
    print "Internal ERROR: Cannot register global '"  print name print "' with UNKNOWN type\n"
    print " @ " print lexerLoc print "\n"
    exit
  }
  i = 0 while i < numGlobals do i = i + 1 {
    if globalNames[i] == name {
      return
    }
  }
  //if debug {
    //print "// Adding global name " + name print "\n"
  //}
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
// This file maxes out at about 1600
mainBuffer: string[2000]

// Stores pre-main definitions
preMainBufferIndex = 0
// This file maxes out at around 20305.
preMainBuffer: string[30000]

bufferIndex = 0
emitBuffer: string[]
emitBuffer=mainBuffer

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
  if debug { print "// " print line print "\n"}
}

spoolBuffer: proc(buffer:string[], len:int) {
  i=0 while i < len do i=i+1 {
    emit(buffer[i])
  }
}

registerProc: proc(name: string, returnType: int) {
  if returnType == TYPE_UNKNOWN {
    print "INTERNAL ERROR: Cannot have UNKNOWN PROC return type\n"
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
  print "INTERNAL ERROR: Cannot set current proc num for proc '" print name print "'\n"
  print " @ " print lexerLoc print "\n"
  exit
}

lookupProcReturnType: proc(name: string): int {
  i = 0 while i < numProcs do i = i + 1 {
    if name == procNames[i] {
      return returnTypes[i]
    }
  }
  print "INTERNAL ERROR: Cannot find PROC '" print name print "'\n"
  print " @ " print lexerLoc print "\n"
  exit
  return -1
}

// returns the index of the param in the arrays
lookupParam: proc(name: string): int {
  if currentProcNum == -1 {
    print "INTERNAL ERROR: Cannot look up parameter " print name print " because not in a PROC"
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
    print "INTERNAL ERROR: Cannot lookup local " print name print " because not in a PROC"
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
indentSize = 0
indent: proc() {
  i = 0 while i < indentSize do i=i+1 { emit("  ") }
}

///////////////////////////////////////////////////////////////////////////////
// EXPRESSION RULES
///////////////////////////////////////////////////////////////////////////////

// expr -> boolor
// boolOr ->
//   leftType = boolXor
//   while token == or or bit_or {
//      right = boolXor
//      generate code for left (op) right
//   }
//   return leftType
// boolXor -> boolAnd (op) boolAnd (xor or ^)
// boolAnd -> compare (op) compare (and or &&)
// compare -> shift (op) shift 
// *shift -> addSub (op) addSub (<< or >>)
// addSub -> muldiv (op) mulDiv (+ or -)
// mulDiv -> unary (op) unary (* or / or %)
// unary -> {
//    if minus { eat token; e = parseExpression; generate -e }
//    if plus { eat token; return parseexpression}
//    *if bitnot { eat token; e = parseexpression; generate !e}
//    *if not { eat token; e = parseexpression; generate not e}
//    length, asc, chr
//    else return composite
// composite -> atom | atom [ int ] | *atom . fieldname
// atom -> literal constant, variable, variable '(' params ') | '(' expr ')', null | 'input'


// Each of these returns the type of the expression: TYPE_INT, TYPE_BOOL, TYPE_STRING, etc.
expr: proc(): int {
  return boolOr()
}

// rats, v0 doesn't support array literals
//PREC = [
  //TOKEN_KEYWORD, KW_OR, TYPE_BOOL,
  //TOKEN_BIT_OR, 0, TYPE_INTEGRAL, // int, long, byte only...
  //TOKEN_KEYWORD, KW_XOR, TYPE_BOOL,
  //TOKEN_BIT_XOR, 0, TYPE_INTEGRAL, // int, long, byte only...
  //TOKEN_KEYWORD, KW_AND, TYPE_BOOL,
  //TOKEN_BIT_AND, 0, TYPE_INTEGRAL, // int, long, byte only...
  //TOKEN_EQEQ, 0, -1, // most kinds, but hm, strings will be weird.
  //TOKEN_NEQ, 0, -1,
  //TOKEN_LT, 0, -1,
  //TOKEN_GT, 0, -1,
  //TOKEN_LEQ, 0, -1,
  //TOKEN_GEQ, 0, -1,
  //TOKEN_SHIFT_LEFT, 0, TYPE_INTEGRAL,  // int, long, byte only...
  //TOKEN_SHIFT_RIGHT, 0, TYPE_INTEGRAL,  // int, long, byte only...
  //TOKEN_PLUS, 0, TYPE_NUMERIC+TYPE_STRING,
  //TOKEN_MINUS, 0, TYPE_NUMERIC,  // numeric only
  //TOKEN_MULT, 0, TYPE_NUMERIC,  // numeric only
  //TOKEN_DIV, 0, TYPE_NUMERIC,  // numeric only
  //TOKEN_MOD, 0, TYPE_INTEGRAL  // int, long, byte only...
//]

//JAVA_OPS = [
  //"||",
  //"|",
  //"^",
  //"^", // does Java have a separate xor? yes, but actually no.
  //"&&",
  //"&",
  //"==",
  //"!=",
  //"<",
  //">",
  //"<=",
  //">=",
  //"<<",
  //">>",
  //"+",
  //"-",
  //"*",
  //"/",
  //"%"
//]


boolOr: proc(): int {
  leftType = boolXor()
  rightType: int
  if leftType == TYPE_BOOL {
    while lexTokenKw == KW_OR {
      advanceParser() // eat the symbol
      emit(" || ")
      rightType = boolXor()
      checkTypes(leftType, rightType)
    }
  }
  if isIntegral(leftType) {
    while lexTokenType == TOKEN_BIT_OR {
      advanceParser() // eat the symbol
      emit(" | ")
      rightType = boolXor()
      checkTypes(leftType, rightType)
    }
  }
  return leftType
}

boolXor: proc(): int {
  leftType = boolAnd()
  rightType: int
  if leftType == TYPE_BOOL {
    while lexTokenKw == KW_XOR {
      advanceParser() // eat the symbol
      emit(" ^ ")
      rightType = boolAnd()
      checkTypes(leftType, rightType)
    }
    return leftType
  }
  if isIntegral(leftType) {
    while lexTokenType == TOKEN_BIT_XOR {
      advanceParser() // eat the symbol
      emit(" ^ ")
      rightType = boolAnd()
      checkTypes(leftType, rightType)
    }
  }
  return leftType
}

boolAnd: proc(): int {
  leftType = compare()
  rightType: int
  if leftType == TYPE_BOOL {
    while lexTokenKw == KW_AND {
      advanceParser() // eat the symbol
      emit(" && ")
      rightType = compare()
      checkTypes(leftType, rightType)
    }
  }
  if isIntegral(leftType) {
    while lexTokenType == TOKEN_BIT_AND {
      advanceParser() // eat the symbol
      emit(" & ")
      rightType = compare()
      checkTypes(leftType, rightType)
    }
  }
  return leftType
}

compare: proc(): int {
  leftType = shift()
  opstring = lexTokenString
  op = lexTokenType

  rightType: int
  compareBuffer:string[100] // yes just 100
  oldEmitBuffer:string[]
  oldBufferIndex:int
  count:int

  if isNumeric(leftType) and (lexTokenType >= TOKEN_EQEQ and lexTokenType <= TOKEN_GEQ) {
    advanceParser() // eat the symbol
    emit(" ") emit(opstring) emit(" ")
    rightType = shift()
    checkTypes(leftType, rightType)
    return TYPE_BOOL
  }
  if leftType == TYPE_BOOL and (lexTokenType == TOKEN_EQEQ or lexTokenType == TOKEN_NEQ) {
    advanceParser() // eat the symbol
    emit(" ") emit(opstring) emit(" ")
    rightType = shift()
    checkTypes(leftType, rightType)
    return TYPE_BOOL
  }
  if isRecordType(leftType) and (lexTokenType == TOKEN_EQEQ or lexTokenType == TOKEN_NEQ) {
    op = lexTokenType
    advanceParser() // eat the symbol

    oldEmitBuffer=emitBuffer
    emitBuffer=compareBuffer
    oldBufferIndex = bufferIndex
    bufferIndex=0

    rightType = shift()
    if rightType != TYPE_NULL and rightType != leftType {
      print "ERROR: Type mismatch. Left operand is " print typeName(leftType)
      print ", but right operand is " print typeName(rightType)
      print " @ " print lexerLoc print "\n"
      exit
    }

    count=bufferIndex
    emitBuffer=oldEmitBuffer
    bufferIndex=oldBufferIndex

    if rightType == TYPE_NULL {
      // juse do primitive comparison
      emit(" ") emit(opstring) emit(" null")
      return TYPE_BOOL
    }
    // use .equals
    emit(".equals(") spoolBuffer(compareBuffer, count)
    if op == TOKEN_NEQ {
      emit(" == false")
    }

    return TYPE_BOOL
  }
  if leftType == TYPE_STRING and (lexTokenType >= TOKEN_EQEQ and lexTokenType <= TOKEN_GEQ) {
    advanceParser() // eat the symbol

    oldEmitBuffer=emitBuffer
    emitBuffer=compareBuffer
    oldBufferIndex = bufferIndex
    bufferIndex=0

    rightType = shift()
    if (rightType != TYPE_STRING and rightType != TYPE_NULL) {
      print "ERROR: Type mismatch. Left operand is " print typeName(leftType)
      print ", but right operand is " print typeName(rightType)
      print " @ " print lexerLoc print "\n"
      exit
    }

    count=bufferIndex
    emitBuffer=oldEmitBuffer
    bufferIndex=oldBufferIndex

    if rightType == TYPE_NULL {
      // use primitive comparison
      emit(" ") emit(opstring) emit(" null")
    } else {
      emit(".compareTo(")
      spoolBuffer(compareBuffer, count)
      emit(") ")
      emit(opstring) emit(" 0")
    }
    return TYPE_BOOL
  }
  return leftType
}

shift: proc(): int {
  leftType = addSub()
  while isIntegral(leftType) and
      (lexTokenType == TOKEN_SHIFT_LEFT or
       lexTokenType == TOKEN_SHIFT_RIGHT) {

    op = lexTokenType
    advanceParser() // eat the symbol
    if op == TOKEN_SHIFT_LEFT { emit(" << ") }
    else { emit(" >> ") } 
    rightType = addSub()
    checkTypes(leftType, rightType)
  }
  return leftType
}


addSub: proc(): int {
  leftType = mulDiv()
  if leftType == TYPE_STRING or isNumeric(leftType) {
    while lexTokenType == TOKEN_PLUS or lexTokenType == TOKEN_MINUS {
      if leftType == TYPE_BOOL {
        print "ERROR: Cannot add or subtract booleans"
        print " @ " print lexerLoc print "\n"
        exit
      }
      if leftType == TYPE_STRING and lexTokenType == TOKEN_MINUS {
        print "ERROR: Cannot subtract strings"
        print " @ " print lexerLoc print "\n"
        exit
      }
      opstring = lexTokenString
      advanceParser() // eat the symbol
      emit(" ") emit(opstring) emit(" ") 
      rightType = mulDiv()
      checkTypes(leftType, rightType)
    }
  }
  return leftType
}

mulDiv: proc(): int {
  leftType = unary()
  while isNumeric(leftType) and
      (lexTokenType == TOKEN_MULT or lexTokenType == TOKEN_DIV or lexTokenType == TOKEN_MOD) {
    if leftType == TYPE_DOUBLE and lexTokenType == TOKEN_MOD {
      print "ERROR: Cannot take MOD of doubles"
      print " @ " print lexerLoc print "\n"
      exit
    }
    opstring = lexTokenString
    advanceParser() // eat the symbol
    emit(" ") emit(opstring) emit(" ") 
    rightType = unary()
    checkTypes(leftType, rightType)
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

    if isNumeric(type) {
      emit("-")
      spoolBuffer(unaryBuffer, count)
      return type
    }
    print "ERROR: cannot unary minus STRINGs"
    print " @ " print lexerLoc print "\n"
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
      print "ERROR: Cannot take LENGTH of " print typeName(type) print "\n"
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
      print "ERROR: Cannot take ASC of " print typeName(type) print "\n"
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
      print "ERROR: Cannot take CHR of " print typeName(type) print "\n"
      print " @ " print lexerLoc print "\n"
      exit
    }

    return TYPE_STRING
  }

  return composite()
}

isArrayType: proc(type: int): bool {
  return type > TYPE_ARRAY and type < TYPE_VOID
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
    print "ERROR: ARRAY index must be INT; was " print typeName(indexType) print "\n"
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
    print "ERROR: String index must be int; was " print typeName(indexType) print "\n"
    print " @ " print lexerLoc print "\n"
    exit
  }
  spoolBuffer(exprBuffer, count)
  emit(", ")
  spoolBuffer(exprBuffer, count)
  emit(" + 1)")
  // TODO: check for index > 0 and index < string length
}

// composite -> atom | atom [ int ] | atom . fieldname
composite: proc(): int {
  leftType = atom()
  while lexTokenType == TOKEN_LBRACKET or lexTokenType == TOKEN_DOT {
    if lexTokenType == TOKEN_LBRACKET {
      // array index
      expectToken(TOKEN_LBRACKET, '[')

      if isArrayType(leftType) {
        return generateArrayIndex(leftType)
      } elif leftType == TYPE_STRING {
        generateStringIndex()
        return leftType
      }

      print "ERROR: Cannot take index of " print typeName(leftType) print "\n"
      print " @ " print lexerLoc print "\n"
      exit
    } else {
      // field reference
      expectToken(TOKEN_DOT, '.')

      // 1. make sure leftType is a record.
      if isRecordType(leftType) {
        emit(".")
        // Huh, v0 wasn't letting me write
        // if isRecordType(leftType) == false {

        fieldName = lexTokenString
        // 2. make sure next token is a variable (field name)
        expectToken(TOKEN_VARIABLE, "field name")

        // 3. make sure field name is valid
        recordIndex = leftType - TYPE_RECORD_BASE
        fieldIndex = lookupField(recordIndex, fieldName)
        if fieldIndex == -1 {
          print "ERROR: Unknown field " print fieldName print " of record type " print recordNames[recordIndex]
          print "\n @ " print lexerLoc print "\n"
          exit
          return -1
        }
        emit(fieldName)

        fieldType = fieldTypes[fieldIndex]

        // Overwrite return type to be *this* field's type
        leftType = fieldType
      } else {
        print "ERROR: Cannot reference field of non-record type" print typeName(leftType)
        print "\n @ " print lexerLoc print "\n"
        exit
        return -1
      }
    }
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
      emit(", ")
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
  // NOTE MULTILINE STRING
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


// atom -> constant | variable | variable '(' args ')' | '(' expr ')' | input
atom: proc(): int {
  if lexTokenType == TOKEN_LITERAL_CONSTANT {
    if lexTokenVarType == TYPE_STRING {
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
      return lexTokenVarType

    } elif isNumeric(lexTokenVarType) {
      // numeric constant
      longConstant = lexTokenVarType == TYPE_LONG
      emit(lexTokenString)
      advanceParser()
      // TODO: byte
      if longConstant { emit ("L") }
      return lexTokenVarType
    } elif lexTokenVarType == TYPE_BOOL {
      // constant boolean
      emit(lexTokenString)
      advanceParser()
      return lexTokenVarType
    }

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
    advanceParser() // eat the null
    emit("null")
    return TYPE_NULL
  } elif lexTokenType == TOKEN_KEYWORD and lexTokenKw == KW_NEW {
    advanceParser() // eat the new
    if lexTokenType != TOKEN_VARIABLE {
      print "Expected variable after NEW; saw " print lexTokenString
      print " @ " print lexerLoc print "\n"
      exit
    }
    recordName = lexTokenString
    recordIndex = lookupRecord(recordName)
    if recordIndex == -1 {
      print "Unknown record " print lexTokenString
      print " @ " print lexerLoc print "\n"
      exit
    }
    emit("new ") emit(recordName) emit("()")
    advanceParser()
    return TYPE_RECORD_BASE + recordIndex
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
  i = TYPE_INT while i <= LAST_TYPE do i = i + 1 {
    if D_TYPE_NAMES[i] != null and D_TYPE_NAMES[i] == lexTokenString {
      advanceParser()
      return i
    }
  }

  // Lookup record here
  recordIndex = lookupRecord(lexTokenString)
  if recordIndex != -1 {
    //if debug {
     // print "  ; found record type " print lexTokenString print "\n"
    //}
    advanceParser()
    return TYPE_RECORD_BASE + recordIndex
  }

  print "ERROR: Unknown type " printToken()
  print " @ " print lexerLoc print "\n"
  exit
  return -1
}


skipType: proc() {
  i = TYPE_INT while i <= LAST_TYPE do i = i + 1 {
    if D_TYPE_NAMES[i] != null and D_TYPE_NAMES[i] == lexTokenString {
      advanceParser()
      return
    }
  }
  expectToken(TOKEN_VARIABLE, 'record type')
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
    print "ARRAY size must be INT; was " print typeName(sizeType) print "\n"
    print " @ " print lexerLoc print "\n"
    exit
  }
  expectToken(TOKEN_RBRACKET, ']')

  registerOrLookUpVariable(variable, arrayType)
  emit(variable) emit(' = new ') emit(typeName(baseType)) 
  emit('[')
  spoolBuffer(exprBuffer, count)
  emit('];\n')
}

lookupRecord: proc(name: string): int {
  i = 0 while i < numRecords do i = i + 1 {
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


registerRecord: proc(name: string): int {
  if numRecords == MAX_RECORDS {
    print "Max records already defined. Cannot add " print name
    print " @ " print lexerLoc print "\n"
    exit
  }
  i = 0 while i < numRecords do i = i + 1 {
    // Make sure it doesn't exist yet
    if recordNames[i] == name {
      print "Record " print name print " already declared\n"
      print " @ " print lexerLoc print "\n"
      exit
    }
  }
  recordNames[numRecords] = name
  //if debug {
    //print "; registered " print name print " as index " print numRecords print "\n"
  //}
  numRecords = numRecords + 1
  return numRecords - 1
}

registerRecordName: proc(recordName: string) {
  registerRecord(recordName)

  // Skips the rest of the record now that the name is registered.
  expectKeyword(KW_RECORD, 'RECORD')
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

parseRecordDecl: proc(recordName: string) {
  oldBufferIndex = bufferIndex
  oldBuffer = emitBuffer
  emitBuffer = preMainBuffer
  bufferIndex = preMainBufferIndex

  recIndex = lookupRecord(recordName)
  expectKeyword(KW_RECORD, 'RECORD')
  expectToken(TOKEN_LBRACE, "{")

  emit("  private static class ") emit(recordName) emit (" {\n")
  indentSize = indentSize + 1

  fieldIndex = recIndex * FIELDS_PER_RECORD
  // zero or more variable declarations, NOT followed by commas
  while lexTokenType != TOKEN_RBRACE and lexTokenType != TOKEN_EOF {
    if numFields[recIndex] == FIELDS_PER_RECORD {
      print "More than 20 parameters declared for record " print recordName
      print "\n @ " print lexerLoc print "\n"
      exit
    }

    // record the field names and types
    fieldName = lexTokenString
    expectToken(TOKEN_VARIABLE, 'variable')
    expectToken(TOKEN_COLON, ':')

    type = parseType()
    // TODO: this will fail for records.
    indent() emit(typeName(type)) emit(" ") emit(fieldName) emit(";\n")

    // store the name and type of the field
    // TODO: detect duplicate fields
    // registerField(fieldName, type)
    fieldNames[fieldIndex] = fieldName
    fieldTypes[fieldIndex] = type
    fieldIndex = fieldIndex + 1
    numFields[recIndex] = numFields[recIndex] + 1
  }
  indentSize = indentSize - 1
  indent()
  emit("}\n")
  expectToken(TOKEN_RBRACE, "}")

  if debug {
    print "; # records: " print numRecords print "\n"
    print "; record name: " print recordNames[recIndex] print "\n"
    print "; numFields: " print numFields[recIndex] print "\n"
    print "; fields: " i=0 while i < numFields[recIndex] do i = i + 1 {print fieldNames[recIndex*FIELDS_PER_RECORD+i] print " " }
    print "\n"
    //print "; field types: " i=0 while i < numFields[recIndex] do i = i + 1 {print typeName(fieldTypes[recIndex*FIELDS_PER_RECORD+i]) print " " }
    print "\n"
    print "\n"
  }
  preMainBufferIndex = bufferIndex
  emitBuffer = oldBuffer
  bufferIndex = oldBufferIndex
}


// Procedure declaration
parseProc: proc(procName: string) {
  expectKeyword(KW_PROC, 'PROC')
  if currentProcNum != -1 {
    print "Cannot define nested PROCs\n"
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
  emitBuffer = preMainBuffer
  bufferIndex = preMainBufferIndex

  // if next token is :, read return type
  returnType = TYPE_VOID
  if lexTokenType == TOKEN_COLON {
    advanceParser()  // eat the :
    returnType = parseType()
  }

  emit("\n  private static ") emit(typeName(returnType)) emit(" ") emit(procName) emit("(")
  i = 0 while i < numParams[currentProcNum] do i=i+1 {
    emit(typeName(paramTypes[currentProcNum*4+i])) emit(" ") 
    emit(paramNames[currentProcNum*4+i])
    if i < numParams[currentProcNum] - 1 {
      emit(", ")
    }
  }
  
  emit(") ")

  parseBlock(true)
  currentProcNum = -1

  preMainBufferIndex = bufferIndex
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
      print "Expected variable but found: " printToken()
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
    lexTokenType == TOKEN_LITERAL_CONSTANT or
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
    checkTypes(expectedType, actualType)
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

      oldBufferIndex = bufferIndex
      oldBuffer = emitBuffer
      emitBuffer = preMainBuffer
      bufferIndex = preMainBufferIndex

      emit("  private static ") emit(typeName(varType)) emit(" ") emit(variable) 
      emit(";\n")
      preMainBufferIndex = bufferIndex
      emitBuffer = oldBuffer
      bufferIndex = oldBufferIndex
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
    emit(typeName(varType)) emit(" ") emit(variable) emit(";\n")
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
    print "ERROR: Array index must be int; was " print typeName(indexType) print "\n"
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
    checkTypes(varType, exprType)
    emit(variable)
    emit(' = ')
    spoolBuffer(exprBuffer, count)
    if semi { emit(';\n') }

    return
  } elif lexTokenType == TOKEN_COLON {
    advanceParser() // eat the colon
    if lexTokenType == TOKEN_KEYWORD and lexTokenKw == KW_PROC {
      parseProc(variable)
    } elif lexTokenType == TOKEN_KEYWORD and lexTokenKw == KW_RECORD {
      parseRecordDecl(variable)
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
  } elif lexTokenType == TOKEN_PLUSPLUS {
    advanceParser()
    emit(variable)
    emit("++")
    if semi { emit(';\n') }
    return
  } elif lexTokenType == TOKEN_MINUSMINUS {
    advanceParser()
    emit(variable)
    emit("--")
    return
  } elif lexTokenType == TOKEN_DOT {
    generateFieldSet(variable)
    if semi { emit(';\n') }
    return
  }

  print "ERROR: expected one of '=' ':' '(' '[' '--' '++' but found: " printToken()
  print " @ " print lexerLoc print "\n"
  exit
}

generateFieldSet: proc(variable: string) {
  // 1. make sure variable is a record
  varType = generateGetVariable(variable)

  if isRecordType(varType) {
    expectToken(TOKEN_DOT, '.')
    emit(".")

    // 2. make sure field is valid
    fieldName = lexTokenString
    recordIndex = varType - TYPE_RECORD_BASE
    expectToken(TOKEN_VARIABLE, 'field name')
    fieldIndex = lookupField(recordIndex, fieldName)
    if fieldIndex == -1 {
      print "ERROR: Unknown field " print fieldName print " of record " print variable
      print "\n @ " print lexerLoc print "\n"
      exit
    }

    fieldType = fieldTypes[fieldIndex]
    emit(fieldName)
    expectToken(TOKEN_EQ, '=')
    emit("=")
    exprType = expr()
    // 3. make sure field type matches expr type
    checkTypes(fieldType, exprType)
  } else {
    print "ERROR: variable " print variable print " is not record type."
    print " @ " print lexerLoc print "\n"
    exit
  }
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
    print "ERROR: Expected boolean condition in if but found " print typeName(condType) print "\n"
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
      print "ERROR: Expected boolean condition in elif but found " print typeName(condType) print "\n"
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
  numWhiles = numWhiles + 1
  emit('for (; ')
  condType = expr()
  if condType != TYPE_BOOL {
    print "ERROR: Expected boolean as 'while' condition, but found " print typeName(condType) print "\n"
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


parsePrint: proc(kw: int) {
  emit('System.out.print') 
  // is this too clever?
  if kw == KW_PRINTLN { emit('ln') }
  emit('(')
  expr()
  emit(');\n')
}

parseStmt: proc(semi: bool) {
  if lexTokenType == TOKEN_EOF {
    return
  } elif lexTokenType == TOKEN_KEYWORD {
    kw = lexTokenKw
    advanceParser() // eat the token
    if kw == KW_PRINT or kw == KW_PRINTLN {
      parsePrint(kw)
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

  i = 0 while i < preMainBufferIndex do i = i + 1 {
    if preMainBuffer[i] != null {
      print preMainBuffer[i]
    }
  }
  if needsInput {
    outputInput()
  }
  print "\n  public static void main(String args[]) {\n"
  i = 0 while i < bufferIndex do i = i + 1 {
    if mainBuffer[i] != null {
      print mainBuffer[i]
    }
  }
  if debug {
    print "  // preMainBufferIndex" print preMainBufferIndex print "\n"
    print "  // bufferIndex" print bufferIndex print "\n"
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
  resetLexer()
  advanceParser()
}

// Just registers the record names, so we can have forward (or
// recursive) record declarations later.
recordFinder: proc() {
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
  resetLexer()
  advanceParser()
}

initParser: proc() {
  text = input
  newLexer(text)
  advanceParser()
}


initParser()
recordFinder()
procFinder()
parseProgram()
