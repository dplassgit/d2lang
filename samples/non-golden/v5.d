///////////////////////////////////////////////////////////////////////////////
//                                    LEXER                                  //
//                           VERSION 5 (COMPILED BY V4)                      //
///////////////////////////////////////////////////////////////////////////////

//   BUG: params with the same name as a global gives precedence to the global, unlike D (java)

debug=false
debugLex=false
VERSION='v5'

lineBasedError: proc(type: string, message: string, line: int) {
  print type + " error at line " print line println ": " + message
  exit
}

///////////////////////////////////////////////////////////////////////////////
//                                     LEXER                                 //
///////////////////////////////////////////////////////////////////////////////
TOKEN_EOF=0
TOKEN_PLUS=1
TOKEN_MINUS=2
TOKEN_MULT=3
TOKEN_BIT_AND=4 // bit and
TOKEN_BIT_OR=5  // bit or
TOKEN_BIT_XOR=6 // bit xor
TOKEN_DIV=7
TOKEN_MOD=8
TOKEN_EQEQ=9
TOKEN_NEQ=10
TOKEN_LT=11
TOKEN_GT=12
TOKEN_LEQ=13
TOKEN_GEQ=14
TOKEN_SHIFT_LEFT=15
TOKEN_SHIFT_RIGHT=16
TOKEN_BIT_NOT=17 // bit not
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
TOKEN_DOT=29
TOKEN_INC=30
TOKEN_DEC=31
TOKEN_INT=32  // int constant
TOKEN_BOOL=33  // bool constant
TOKEN_BYTE=34 // byte constant
TOKEN_LONG=35 // long constant
TOKEN_DOUBLE=36 // double constant
TOKEN_STRING=37 // string constant

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
KW_RECORD=22
KW_NEW=23
KW_DELETE=24
KW_PRINTLN=25
KW_LONG=26
KW_BYTE=27
KW_DOUBLE=28
KW_ARGS=29
KW_EXTERN=30
KW_XOR=31
// TODO: for, in, get, this, private, load, save, export

KEYWORDS=[
    "print",
    "if",
    "else",
    "elif",
    "proc",
    "return",
    "while",
    "do",
    "break",
    "continue",
    "int",
    "bool",
    "string",
    "null",
    "input",
    "length",
    "chr",
    "asc",
    "exit",
    "and",
    "or",
    "not",
    "record",
    "new",
    "delete",
    "println",
    "long",
    "byte",
    "double",
    "args",
    "extern",
    "xor"
    ]

Lexer: record {
  text: string  // full text
  loc: int      // index/location inside text
  cc: int       // current character
  line: int     // current line
}

newLexer: proc(text: string): Lexer {
  lexer = new Lexer
  lexer.text = text
  resetLexer(lexer)
  return lexer
}

resetLexer: proc(self: Lexer) {
  self.loc = 0
  self.cc = 0
  self.line = 1
  advanceLex(self)
}

nextToken: proc(self: Lexer): Token {
  // skip unwanted whitespace
  while (self.cc == 32 or self.cc == 10 or self.cc == 9 or self.cc == 13) {
    if self.cc == 10 or self.cc == 13 {
      self.line = self.line + 1
    }
    advanceLex(self)
  }
  if self.cc != 0 {
    if isDigit(self.cc) {
      return makeNumberToken(self)
    } elif isLetter(self.cc) {
      // might be string, keyword, boolean constant
      return makeTextToken(self)
    } else {
      return makeSymbolToken(self)
    }
  }

  return makeToken(self, TOKEN_EOF, "")
}

advanceLex: proc(self: Lexer) {
  if self.loc < length(self.text) {
    self.cc=asc(self.text[self.loc])
  } else {
    // Indicates no more characters
    self.cc=0
  }
  self.loc = self.loc + 1
  if debugLex {
    // print "; Lexer cc " print self.cc print ":" println chr(self.cc)
  }
}

///////////////////////////////////////////////////////////////////////////////
//                   Token record, for external consumption                  //
///////////////////////////////////////////////////////////////////////////////

Token: record {
  type:int
  stringValue:string
  keyword:int
  boolValue:bool
  line:int
}

makeBasicToken: proc(self: Lexer, type: int, value: string): Token {
  t = new Token
  t.type = type
  t.stringValue = value
  t.keyword = -1
  t.line = self.line
  return t
}

// Build a string token
makeToken: proc(self: Lexer, type: int, value: string): Token {
  if debugLex {
    print "; Making token type: " print type print " value: "
    println value
  }
  t = makeBasicToken(self, type, value)
  return t
}

// Build a bool constant token
BoolToken: proc(self: Lexer, value: bool, valueAsString: string): Token {
  t = makeBasicToken(self, TOKEN_BOOL, valueAsString)
  t.boolValue = value
  if debugLex {
    print "; Making bool token: " println value
  }
  return t
}

// Build a keyword token
KeywordToken: proc(self: Lexer, value: int, valueAsString: string): Token {
  t = makeBasicToken(self, TOKEN_KEYWORD, valueAsString)
  t.keyword = value
  if debugLex {
    print "; Making kw token: " println valueAsString
  }
  return t
}

// Convert an int into a string.
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


isalpha: extern proc(c: int): int
isLetter: proc(c: int): bool {
  // return (c>=asc('a') and c <= asc('z')) or (c>=asc('A') and c<=asc('Z')) or c==asc('_')
  return isalpha(c) != 0 or c == 95
}

isdigit: extern proc(c: int): int
isDigit: proc(c: int): bool {
  // return c>=asc('0') and c <= asc('9')
  return isdigit(c) != 0
}

isLetterOrDigit: proc(c: int): bool {
  return isLetter(c) or isDigit(c)
}

stricmp: extern proc(s1:string, s2:string): int

makeTextToken: proc(self: Lexer): Token {
  value=''
  first = self.cc
  if isLetter(self.cc) {
    value=value + chr(self.cc)
    advanceLex(self)
  }
  while isLetterOrDigit(self.cc) {
    value=value + chr(self.cc)
    advanceLex(self)
  }

  if first == 95 {
    // do not allow leading _
    lineBasedError("Scanner", "Illegal variable name " + value, self.line)
    exit
  }
  if value == 'true' or value == 'false' {
    return BoolToken(self, value == 'true', value)
  }

  i=0 while i < length(KEYWORDS) do i++ {
    if stricmp(value, KEYWORDS[i]) == 0 {
      return KeywordToken(self, i, value)
    }
  }

  return makeToken(self, TOKEN_VARIABLE, value)
}


isHexDigit: proc(ch: int): bool {
  return isDigit(ch) or
      (ch >= 65 and ch <= 70) or
      (ch >= 97 and ch <= 102)
}

makeNumberToken: proc(self: Lexer): Token {
  valueAsString = ''
  while isDigit(self.cc) do advanceLex(self) {
    valueAsString = valueAsString + chr(self.cc)
  }

  if self.cc == 76 { // L for long
    advanceLex(self)
    // Don't make the constant
    // Note: it still may overflow a long... shrug.
    return makeToken(self, TOKEN_LONG, valueAsString)
  } elif self.cc == 121 { // asc('y')

    // Byte constant
    if valueAsString != '0' {
      lineBasedError("Scanner", "Invalid BYTE constant: " + valueAsString, self.line)
      exit
    }
    advanceLex(self)   // eat the 'y'

    first = self.cc
    advanceLex(self)
    second = self.cc
    advanceLex(self)
    valueAsString = chr(first) + chr(second)
    // Make sure it's a valid hex string
    if not (isHexDigit(first) and isHexDigit(second)) {
      lineBasedError("Scanner", "Invalid BYTE constant: " + valueAsString, self.line)
      exit
    }

    return makeToken(self, TOKEN_BYTE, valueAsString)
  } elif self.cc == 46 { // dot
    advanceLex(self)
    valueAsString = valueAsString + "."
    while isDigit(self.cc) do advanceLex(self) {
      valueAsString = valueAsString + chr(self.cc)
    }
    return makeToken(self, TOKEN_DOUBLE, valueAsString)
  } else {
    // make an int constant
    value=0
    // TODO: when longs are supported, make a long and
    // see it it exceeds 2^31.
    i = 0 while i < length(valueAsString) do i++ {
      digit = asc(valueAsString[i]) - 48
      value=value * 10 + digit
      if (value % 10) != digit or value < 0 {
        // hacky way to detect overflow
        lineBasedError("Scanner", "INT constant too big: " + valueAsString, self.line)
        exit
      }
    }
    return makeToken(self, TOKEN_INT, valueAsString)
  }
}

startsWithSlash: proc(self: Lexer): Token {
  advanceLex(self) // eat the first slash
  if self.cc == 47 {
    // second slash == comment.
    advanceLex(self) // eat the second slash
    // Eat characters until newline
    while self.cc != 10 and self.cc != 0 do advanceLex(self) {}
    if self.cc != 0 {
      self.line = self.line + 1
      advanceLex(self) // eat the newline
    }
    // TODO figure out if this can be done a different way, maybe with a "comment" token?
    return nextToken(self)
  }
  return makeToken(self, TOKEN_DIV, '/')
}

startsWithBang: proc(self: Lexer): Token {
  oc=self.cc
  advanceLex(self) // eat the !
  if self.cc == 61 { // =
    advanceLex(self)
    return makeToken(self, TOKEN_NEQ, '!=')
  }
  return makeToken(self, TOKEN_BIT_NOT, '!')
}

startsWithPlus: proc(self: Lexer): Token {
  oc=self.cc
  advanceLex(self)
  if self.cc == 43 { // +
    advanceLex(self)
    return makeToken(self, TOKEN_INC, '++')
  }
  return makeToken(self, TOKEN_PLUS, '+')
}

startsWithMinus: proc(self: Lexer): Token {
  oc=self.cc
  advanceLex(self)
  if self.cc == 45 { // -
    advanceLex(self)
    return makeToken(self, TOKEN_DEC, '--')
  }
  return makeToken(self, TOKEN_MINUS, '-')
}

startsWithGt: proc(self: Lexer): Token {
  oc=self.cc
  advanceLex(self)
  if self.cc == 61 { // =
    advanceLex(self)
    return makeToken(self, TOKEN_GEQ, '>=')
  } elif self.cc == 62 {
    // shift right
    advanceLex(self)
    return makeToken(self, TOKEN_SHIFT_RIGHT, '>>')
  }
  return makeToken(self, TOKEN_GT, '>')
}

startsWithLt: proc(self: Lexer): Token {
  oc=self.cc
  advanceLex(self)
  if self.cc == 61 {
    advanceLex(self)
    return makeToken(self, TOKEN_LEQ, '<=')
  } elif self.cc == 60 {
    // shift left
    advanceLex(self)
    return makeToken(self, TOKEN_SHIFT_LEFT, '<<')
  }
  return makeToken(self, TOKEN_LT, '<')
}

startsWithEq: proc(self: Lexer): Token {
  oc=self.cc
  advanceLex(self)
  if self.cc == 61 {
      advanceLex(self)
    return makeToken(self, TOKEN_EQEQ, '==')
  }
  return makeToken(self, TOKEN_EQ, '=')
}

makeStringLiteralToken: proc(self: Lexer, firstQuote: int): Token {
  advanceLex(self) // eat the tick/quote
  sb=''
  while self.cc != firstQuote and self.cc != 0 {
    if self.cc == 92 { // backslash
      advanceLex(self)
      if self.cc == 110 { // backslash - n
        sb=sb + chr(10)  // linefeed
      } elif self.cc == 114 { // backslash-r
        sb=sb + chr(13)  // carriage-return
      } elif self.cc == 116 { // backslash-t
        sb=sb + chr(9)  // tab
      } elif self.cc == 34 or self.cc == 39 or self.cc == 92 {
        sb=sb + chr(self.cc)  // tick/quote/backslash
      }
    } else {
      sb=sb + chr(self.cc)
    }
    advanceLex(self)
  }

  if self.cc == 0 {
    lineBasedError("Scanner", "Unclosed STRING literal " + sb, self.line)
    exit
  }

  advanceLex(self) // eat the closing tick/quote
  return makeToken(self, TOKEN_STRING, sb)
}

makeSymbolToken: proc(self: Lexer): Token {
  oc = self.cc
  if oc == 61 {
    return startsWithEq(self)
  } elif oc == 60 {
    return startsWithLt(self)
  } elif oc == 62 {
    return startsWithGt(self)
  } elif oc == 43 {
    return startsWithPlus(self)
  } elif oc == 45 {
    return startsWithMinus(self)
  } elif oc == 40 {
    advanceLex(self)
    return makeToken(self, TOKEN_LPAREN, '(')
  } elif oc == 41 {
    advanceLex(self)
    return makeToken(self, TOKEN_RPAREN, ')')
  } elif oc == 42 {
    advanceLex(self)
    return makeToken(self, TOKEN_MULT, '*')
  } elif oc == 47 {
    return startsWithSlash(self)
  } elif oc == 37 {
    advanceLex(self)
    return makeToken(self, TOKEN_MOD, '%')
  } elif oc == 38 {
    advanceLex(self)
    return makeToken(self, TOKEN_BIT_AND, '&')
  } elif oc == 124 {
    advanceLex(self)
    return makeToken(self, TOKEN_BIT_OR, '|')
  } elif oc == 94 {
    advanceLex(self)
    return makeToken(self, TOKEN_BIT_XOR, '^')
  } elif oc == 33 {
    return startsWithBang(self)
  } elif oc == 123 {
    advanceLex(self)
    return makeToken(self, TOKEN_LBRACE, '{')
  } elif oc == 125 {
    advanceLex(self)
    return makeToken(self, TOKEN_RBRACE, '}')
  } elif oc == 91 {
    advanceLex(self)
    return makeToken(self, TOKEN_LBRACKET, '[')
  } elif oc == 93 {
    advanceLex(self)
    return makeToken(self, TOKEN_RBRACKET, ']')
  } elif oc == 58 {
    advanceLex(self)
    return makeToken(self, TOKEN_COLON, ':')
  } elif oc == 34 or oc == 39 { // double or single quote
    return makeStringLiteralToken(self, oc)
  } elif oc == 44 {
    advanceLex(self)
    return makeToken(self, TOKEN_COMMA, ',')
  } elif oc == 46 {
    advanceLex(self)
    return makeToken(self, TOKEN_DOT, '.')
  }
  lineBasedError("Scanner", "Unknown character: " + chr(self.cc) + " ASCII code: " + toString(self.cc), self.line)
  exit
}

// for debugging
printToken: proc(t: Token) {
  if t.type == TOKEN_EOF {
    println 'EOF'
  } elif t.type == TOKEN_INT {
    print 'Int constant: ' println t.stringValue
  } elif t.type == TOKEN_LONG {
    print 'Long constant: ' println t.stringValue
  } elif t.type == TOKEN_STRING {
    print 'String constant: ' println t.stringValue
  } elif t.type == TOKEN_BOOL {
    print 'Bool constant: ' println t.boolValue
  } elif t.type == TOKEN_KEYWORD {
    print 'Keyword: ' println t.stringValue
  } elif t.type == TOKEN_VARIABLE {
    print 'Variable: ' println t.stringValue
  } else {
    print 'Token: ' print t.stringValue print ' type: ' println t.type
  }
}

//text = input
//lex=newLexer(text)

//count = 1
//t=nextToken(lex)
//print "; line #:" print lex.line print ":" printToken(t)

//while t.type != TOKEN_EOF do count++ {
//  t=nextToken(lex)
//  print "; line #:" print lex.line print ":" printToken(t)
//}

//print 'Total number of tokens: '
//print count print "\n"
//exit

StringListEntry: record {
  value: string
  index: int
  next: StringListEntry
}
StringList: record {
  head: StringListEntry
}

// Append the given string to the list. Returns the entry. If the string
// was already on the list, does not append.
append: proc(self: StringList, newvalue: string): StringListEntry {
  node = new StringListEntry
  node.value = newvalue
  if self.head == null {
    // no entries, make one
    self.head = node
    return node
  }
  head = self.head last = head while head != null do head = head.next {
    if head.value == newvalue {
      // duplicate
      return head
    }
    last = head
  }
  
  // new entry
  node.index = last.index + 1
  last.next = node
  return node
}
///////////////////////////////////////////////////////////////////////////////
//                                    EMITTER                                //
///////////////////////////////////////////////////////////////////////////////

Emitter: record {
  head: StringListEntry  // set once
  tail: StringListEntry  // set once, but updated
}


emitter_emit0: proc(self: Emitter, s: string): StringListEntry {
  entry = new StringListEntry
  entry.value = s
  if self.head == null {
    self.head = entry
    self.tail = entry
  } else {
    // self.tail.next = entry
    tail = self.tail
    tail.next = entry
    self.tail = entry
  }
  return entry
}

emitter_emit: proc(self: Emitter, s: string): StringListEntry {
  return emitter_emit0(self, "  " + s)
}

emitter_emitLabel: proc(self: Emitter, label: string): StringListEntry {
  return emitter_emit0(self, "\n" + label + ":")
}

emitter_emitNum: proc(self: Emitter, s: string, num: int): StringListEntry {
  return emitter_emit(self, s + toString(num))
}

emitter_printEntries: proc(self: Emitter) {
  head = self.head while head != null do head = head.next {
    println head.value
  }
  println ""
}

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
TYPE_BOOL = makePrimitiveType('bool', 1, ' BYTE ', 'db')
TYPE_STRING = makePrimitiveType('string', 8, ' QWORD ', 'dq')
TYPE_VOID = makePrimitiveType('void', 0, '', '')
TYPE_NULL = makePrimitiveType('null', 8, '', '')

TYPE_INT = makePrimitiveType('int', 4, ' DWORD ', 'dd')
TYPE_INT.isIntegral = true
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
doubleTable=new StringList

// Create a string constant and return its index. If the string already is in the table,
// will not re-create it.
addStringConstant: proc(s: string): int  {
  node = append(stringTable, s)
  return node.index
}

addDoubleConstant: proc(d: string): int  {
  node = append(doubleTable, d)
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
      typeError("PROC '" + name + "' already declared")
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
  typeError("Cannot find PROC '" + name + "'")
  exit
}

// returns the param VarSymbol, or null if not found.
lookupParam: proc(name: string): VarSymbol {
  if currentProc == null {
    typeError("Cannot lookup parameter '" + name + "' because not in a PROC")
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
    typeError("Cannot lookup local '" + name + "' because not in a PROC")
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
    typeError("Cannot add local '" + name + "' because not in a PROC")
    exit
  }

  offset = 0
  head = currentProc.locals last = head while head != null do head = head.next {
    if head.name == name {
      // TODO: improve this message:
      // Type error at line 2: Variable 'a' already declared as INT, cannot be redeclared as BOOL
      typeError("Duplicate local '" + name + "' in PROC '" + currentProc.name + "'")
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
      typeError("Duplicate parameter '" + name + "' to PROC '" + currentProc.name + "'")
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
      typeError("RECORD type '" + name + "' already declared")
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
    generalError("Internal error", "Variable type is null for RECORD type '" +rt.name + 
                 "', field " + name)
    exit
  }

  offset = 0 head = rt.fields last = head while head != null do head=head.next {
    if head.name == name {
      typeError("Duplicate field " + name + " in RECORD " + rt.name)
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
    generalError("Internal", "VarType is null, cannot update size of " + rt.name)
    exit
  } else {
    rt.size = rt.size + varType.size
  }
  return newField
}

A_REG=0
B_REG=1
C_REG=2
D_REG=3
XMM0=0
XMM1=1
XMM2=2
XMM3=3

DOUBLE_REGISTERS=[
  'XMM0',
  'XMM1',
  'XMM2',
  'XMM3'
]

REGISTERS=[
  'AL',
  'BL',
  'CL',
  'DL',
  'AX',
  'BX',
  'CX',
  'DX',
  'EAX',
  'EBX',
  'ECX',
  'EDX',
  'RAX',
  'RBX',
  'RCX',
  'RDX'
]

makeRegister: proc(base: int, varType: VarType): string {
  if varType == TYPE_DOUBLE {
    return DOUBLE_REGISTERS[base]
  }
  index = base
  bytes = varType.size
  if bytes == 2 { index = index + 4}
  elif bytes == 4 { index = index + 8}
  elif bytes == 8 { index = index + 12}
  return REGISTERS[index]
}

push: proc(register: int, type: VarType, emitter: Emitter) {
  if type != TYPE_DOUBLE {
    emitter_emit(emitter, "push " + REGISTERS[register + 12]) // makeRegister(register, TYPE_LONG))
  } else {
    emitter_emit(emitter, "sub RSP, 8")
    emitter_emit(emitter, "movq [RSP], " + makeRegister(register, TYPE_DOUBLE))
  }
  
}

pop: proc(register: int, type: VarType, emitter: Emitter) {
  if type != TYPE_DOUBLE {
    emitter_emit(emitter, "pop " + REGISTERS[register + 12]) // makeRegister(register, TYPE_LONG))
  } else {
    emitter_emit(emitter, "movq " + makeRegister(register, TYPE_DOUBLE) + ", [RSP]")
    emitter_emit(emitter, "add RSP, 8")
  }
}
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

DOUBLE_OPCODES=[
    ";nop ",  // eof
    "addsd ",
    "subsd ",
    "mulsd ",
    "; and ",
    "; or ",
    "; xor ",
    "divsd ",
    "; mod ",
    "setz ",  // ==
    "setnz ", // !=
    "setb ",  // <
    "seta ",  // >
    "setbe ", // <=
    "setae ",  // >=
    "; shl ",
    "; shr "
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
  } elif leftType == TYPE_DOUBLE and (parser.token.type >= TOKEN_EQEQ and parser.token.type <= TOKEN_GEQ) {
    op = parser.token.type
    opstring = parser.token.stringValue
    advanceParser() // eat the symbol
    // push xmm0
    push(XMM0, TYPE_DOUBLE, parser.emitter)

    rightType = shift()
    checkTypes(leftType, rightType)

    // pop into xmm1
    pop(XMM1, TYPE_DOUBLE, parser.emitter)

    // left = left (op) right
    emit("comisd XMM1, XMM0")
    emit(DOUBLE_OPCODES[op] + "AL")
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
    push(0, leftType, parser.emitter) // push left from rax or xmm0

    rightType = mulDiv()
    checkTypes(leftType, rightType)

    pop(1, leftType, parser.emitter) // pop left into rbx or xmm1
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
    } elif leftType == TYPE_DOUBLE {
      // left = left (op) right
      if op == TOKEN_PLUS {
        // If plus, can just do add xmm0,xmm1 instead of two lines
        emit(DOUBLE_OPCODES[op] + "XMM0, XMM1")
      } else {
        // minus; have to do sub ebx, eax
        // xmm1=xmm1 (op) xmm0
        emit(DOUBLE_OPCODES[op] + "XMM1, XMM0")
        emit("movsd XMM0, XMM1")
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
  while (leftType.isIntegral or leftType == TYPE_DOUBLE) and
      (parser.token.type == TOKEN_MULT or parser.token.type == TOKEN_DIV or parser.token.type == TOKEN_MOD) {
    op = parser.token.type

    advanceParser() // eat the symbol
    push(0, leftType, parser.emitter) // push left from rax or xmm0

    rightType = unary()
    checkTypes(leftType, rightType)

    pop(1, leftType, parser.emitter) // pop left into rbx or xmm1

    if leftType.isIntegral {
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
    } else {
      if op == TOKEN_MOD {
        exit "Cannot apply % to DOUBLEs"
      }
      if op == TOKEN_MULT {
        // If mult, can just do mulsd xmm0,xmm1 instead of two lines
        emit(DOUBLE_OPCODES[op] + "XMM0, XMM1")
      } else {
        // division; have to do divsd ebx, eax
        // xmm1=xmm1 (op) xmm0
        emit(DOUBLE_OPCODES[op] + "XMM1, XMM0")
        emit("movsd XMM0, XMM1")
      }
    }
  }
  return leftType
}

// -unary | +unary | length(expr) | asc(expr) | chr(expr) | not expr
unary: proc: VarType {
  if parser.token.type == TOKEN_PLUS {
    advanceParser() // eat the plus
    type = unary()
    if not (type.isIntegral or type == TYPE_DOUBLE) {
      typeError("Cannot apply unary plus to " + type.name)
      exit
    }
    return type
  } elif parser.token.type == TOKEN_MINUS {
    advanceParser() // eat the minus
    type = unary()
    if type == TYPE_DOUBLE {
      emit("xorpd XMM1, XMM1  ; instead of mov xmm1, 0")
      emit("subsd XMM1, XMM0  ; xmm1=-xmm0")
      emit("movsd XMM0, XMM1")
    } elif type.isIntegral {
      // two's complement
      emit("neg " + makeRegister(A_REG, type))
    }  else {
      typeError("Cannot apply unary minus to " + type.name)
      exit
    }
    return type
  } elif parser.token.keyword == KW_LENGTH {
    advanceParser() // eat the length
    expectToken(TOKEN_LPAREN, '(')
    type = expr()
    expectToken(TOKEN_RPAREN, ')')
    if type == TYPE_STRING {
      generateNpeCheck()

      emit("mov RCX, RAX")
      emitExtern("strlen")
    } elif type.isArray {
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
      typeError("RECORD '" + recordName + "' not defined")
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
    typeError("Array index must be INT; was " + indexType.name)
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
  mov = makeMov(baseType)
  emit(mov + baseType.opcodeSize + makeRegister(A_REG, baseType) + ", [RAX]")

  return baseType
}

// Generate a "get" of foo[int] for strings
generateStringIndex: proc {
  emit("push RAX  ; string location")
  indexType = expr()
  if indexType != TYPE_INT {
    typeError("STRING index must be INT; was " + indexType.name)
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
          typeError("Unknown field '" + fieldName + "' of RECORD type " + recSym.name)
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
  mov = makeMov(symbol.varType)
  emit(mov + symbol.varType.opcodeSize + reg + ", " + ref + "  ; get variable " + variable)
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
      typeError("Too many arguments to PROC '" + procName + "'")
    }
    expectedType = param.varType

    if not compatibleTypes(expectedType, actualArgType) {
      typeError("Incorrect type for actual '" + param.name + "' to PROC '" + procName +
        "'. Expected: " + expectedType.name + ", actual: " + actualArgType.name)
    }
    param = param.next
    if procSym.isExtern and numArgs > 4 {
      generalError("Internal", "too many arguments to EXTERN " + procSym.name)
      exit
    }

    // NOTE: Always using 8 bytes per arg.
    push(0, actualArgType, parser.emitter)
    if parser.token.type == TOKEN_COMMA {
      advanceParser() // eat the comma
    }
  }
  // TODO: print the actual and expected #s of arguments
  if numArgs < procSym.numParams {
    typeError("Too few arguments to PROC '" + procName + "'")
  }
  if numArgs > procSym.numParams {
    typeError("Too many arguments to PROC '" + procName + "'")
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
    if type == TOKEN_INT {
      constType = TYPE_INT
    } elif type == TOKEN_LONG {
      constType = TYPE_LONG
    } elif type == TOKEN_BYTE {
      constType = TYPE_BYTE
    }
    // int, long or byte constant
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

  } elif type == TOKEN_DOUBLE {

    // treating doubles as strings because v5 is compiled by v4 which doesn't have doubles
    theValue = parser.token.stringValue
    advanceParser()
    index = addDoubleConstant(theValue)
    line = "movq XMM0, [DOUBLE_" + toString(index) + "]"
    emit(line)
    return TYPE_DOUBLE

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
      typeError("Return type of " + variable + " is VOID. Cannot assign it to a variable.")
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
    typeError("Array size must be an INT, but was " + sizeType.name)
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
    parserError("Cannot define nested PROCs")
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
      parserError("No RETURN statement from non-VOID PROC: " + procName)
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
    parserError("Cannot define nested PROCs")
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
    parserError("Cannot RETURN from outside PROC")
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
    typeError("Incorrect RETURN type of PROC '" + currentProcName + "'. Expected "
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
  mov = makeMov(symbol.varType)
  emit(mov + symbol.varType.opcodeSize + ref + ", " + reg + "  ; set variable " + variable)
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
    typeError("Incorrect type for array index. Expected: INT, actual: " + indexType.name)
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
    typeError("RECORD type '" + recordName + "' not found")
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
    typeError("RECORD type '" + recSym.name + "' not found")
    exit
  }
  fldSym = lookupField(recSym, fieldName)
  if fldSym == null {
    typeError("Field " + fieldName + " of RECORD type '" + recSym.name + "' not found")
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


makeMov: proc(vt: VarType): string {
  if vt == TYPE_DOUBLE {
    return "movq "
  } else {
    return "mov "
  }
}

// Allocates and populates an array literal. Returns the array type.
parseArrayLiteral: proc: VarType {
  expectToken(TOKEN_LBRACKET, '[')

  slotType = TYPE_UNKNOWN
  slotCount = 0
  emit("; collect array literal entries")
  while parser.token.type != TOKEN_RBRACKET and parser.token.type != TOKEN_EOF {
    thisSlotType = expr()
    // pushes xmm0 or rax
    push(0, thisSlotType, parser.emitter) 
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
    // pop into rbx or xmm1
    pop(1, slotType, parser.emitter) 
    mov = makeMov(slotType)
    emit(mov + slotType.opcodeSize + "[RAX + " + toString(offset) + "], " +
        makeRegister(1, slotType))
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
    typeError("Incorrect type of condition in IF. Expected: BOOL, actual: " + condType.name)
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
      typeError("Incorrect type of condition in ELIF. Expected: BOOL, actual: " + condType.name)
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
    parserError("Cannot have BREAK outside WHILE loop")
    exit
  }
  emit("jmp " + whileBreakLabels[parser.numWhiles - 1])
}

parseContinue: proc {
  if parser.numWhiles == 0 {
    parserError("Cannot have CONTINUE outside WHILE loop")
    exit
  }
  emit("jmp " + whileContinueLabels[parser.numWhiles - 1])
}

parseWhile: proc {
  continueLabel = nextLabel("while_continue")
  emitLabel(continueLabel)

  condType = expr()
  if condType != TYPE_BOOL {
    typeError("Incorrect type of condition in WHILE. Expected: BOOL, actual: " + condType.name)
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
  doStmtLabel = ""
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
  exprType = expr()  // puts result in RAX or XMM
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

  } elif exprType == TYPE_INT  {

    index = addStringConstant("%d")
    emitNum("mov RCX, CONST_", index)
    ax = makeRegister(A_REG, exprType)
    emit("movsx RDX, " + ax)

  } elif exprType == TYPE_BYTE {

    index = addStringConstant("0y%02x")
    emitNum("mov RCX, CONST_", index)
    ax = makeRegister(A_REG, exprType)
    emit("mov DL, " + ax)

  } elif exprType == TYPE_LONG {

    index = addStringConstant("%lldL")
    emitNum("mov RCX, CONST_", index)
    emit("mov RDX, RAX")

  } elif exprType == TYPE_DOUBLE {

    index = addStringConstant("%.16g")
    emitNum("mov RCX, CONST_", index)
    emit("movq RDX, XMM0")

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

emitDoubleTable: proc {
  head = doubleTable.head
  while head != null do head = head.next {
    print "  DOUBLE_" print head.index print ": dq " println head.value
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

  if stringTable.head != null or globals != null or doubleTable.head != null {
    println "section .data"
  }
  if stringTable.head != null {
    emitStringTable()
  }
  if doubleTable.head != null {
    emitDoubleTable()
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

