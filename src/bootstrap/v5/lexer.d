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
TOKEN_DOT=29
TOKEN_INC=30
TOKEN_DEC=31
//TOKEN_SHIFT_LEFT=32
//TOKEN_SHIFT_RIGHT=33
TOKEN_BIT_NOT=34 // bit not
TOKEN_BYTE=35 // byte constant
TOKEN_LONG=36 // long constant
TOKEN_DOUBLE=37 // double constant

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
  intValue:int
  keyword:int
  boolValue:bool
  line:int
}

makeBasicToken: proc(self: Lexer, type: int, value: string): Token {
  t = new Token
  t.type = type
  t.stringValue = value
  t.intValue = -1
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

// Build an int constant token
IntToken: proc(self: Lexer, value: int, valueAsString: string): Token {
  t = makeBasicToken(self, TOKEN_INT, valueAsString)
  t.intValue = value
  if debugLex {
    print "; Making int token: " println value
  }
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

makeNumberToken: proc(self: Lexer): Token {
  valueAsString = ''
  while isDigit(self.cc) do advanceLex(self) {
    valueAsString = valueAsString + chr(self.cc)
  }

  if self.cc == 76 { // long
    println "; long constant " + valueAsString
    advanceLex(self)
    return makeToken(self, TOKEN_LONG, valueAsString)
  } else {
    // make an int constant
    value=0
    i = 0 while i < length(valueAsString) do i++ {
      digit = asc(valueAsString[i]) - 48 // asc('0')
      value=value * 10 + digit
      if (value % 10) != digit or value < 0 {
        // overflow
        lineBasedError("Scanner", "Integer constant too big: " + valueAsString, self.line)
        exit
      }
    }
    return IntToken(self, value, valueAsString)
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
  if self.cc == 61 {
    advanceLex(self)
    return makeToken(self, TOKEN_NEQ, '!=')
  }
  lineBasedError("Scanner", "Unknown character: " + chr(self.cc) + " ASCII code: " + toString(self.cc), self.line)
  exit
  // return makeToken(TOKEN_BIT_NOT, '!'))
}

startsWithPlus: proc(self: Lexer): Token {
  oc=self.cc
  advanceLex(self)
  if self.cc == 43 {
    advanceLex(self)
    return makeToken(self, TOKEN_INC, '++')
  }
  return makeToken(self, TOKEN_PLUS, '+')
}

startsWithMinus: proc(self: Lexer): Token {
  oc=self.cc
  advanceLex(self)
  if self.cc == 45 {
    advanceLex(self)
    return makeToken(self, TOKEN_DEC, '--')
  }
  return makeToken(self, TOKEN_MINUS, '-')
}

startsWithGt: proc(self: Lexer): Token {
  oc=self.cc
  advanceLex(self)
  if self.cc == 61 {
    advanceLex(self)
    return makeToken(self, TOKEN_GEQ, '>=')
  //} elif self.cc == '>' {
    // shift right
    //advanceLex(self)
    //return makeToken(TOKEN_SHIFT_RIGHT, '>>')
  }
  return makeToken(self, TOKEN_GT, '>')
}

startsWithLt: proc(self: Lexer): Token {
  oc=self.cc
  advanceLex(self)
  if self.cc == 61 {
    advanceLex(self)
    return makeToken(self, TOKEN_LEQ, '<=')
  //} elif self.cc == '<' {
    //// shift right
    //advanceLex(self)
    //return makeToken(TOKEN_SHIFT_LEFT, '<<')
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
    lineBasedError("Scanner", "Unclosed string literal " + sb, self.line)
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
  } elif oc == 38 { // asc('&') {
    advanceLex(self)
    return makeToken(self, TOKEN_BIT_AND, '&')
  } elif oc == 124 { // asc('|') {
    advanceLex(self)
    return makeToken(self, TOKEN_BIT_OR, '|')
  } elif oc == 94 { // asc('^') {
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
printToken: proc(t:Token) {
  if t.type == TOKEN_EOF {
    println 'EOF'
  } elif t.type == TOKEN_INT {
    print 'Int constant: ' println t.intValue
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

