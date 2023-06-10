///////////////////////////////////////////////////////////////////////////////
//                                    LEXER                                  //
//                           VERSION 3 (COMPILED BY V2)                      //
///////////////////////////////////////////////////////////////////////////////

//   BUG: params with the same name as a global gives precedence to the global, unlike D (java)

debug=false
debugLex=false
VERSION='v3'

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
TOKEN_DOT=29
TOKEN_INC=30
TOKEN_DEC=31
//TOKEN_SHIFT_LEFT=32
//TOKEN_SHIFT_RIGHT=33
//TOKEN_XOR=34 // bit xor

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
KW_LONG=26
KW_BYTE=27
KW_DOUBLE=28
KW_ARGS=29
KW_EXTERN=30
KW_XOR=31
// TODO: for, in, get, this, private, load, save, export

KEYWORDS:string[32]
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
KEYWORDS[KW_LONG]="long"
KEYWORDS[KW_BYTE]="byte"
KEYWORDS[KW_DOUBLE]="double"
KEYWORDS[KW_ARGS]="args"
KEYWORDS[KW_EXTERN]="extern"
KEYWORDS[KW_XOR]="xor"

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

nextToken: proc(self: Lexer): string {
  // skip unwanted whitespace
  while (self.cc == 32 or self.cc == 10 or self.cc == 9 or self.cc == 13) {
    if self.cc == 10 or self.cc == 13 {
      self.line = self.line + 1
    }
    advanceLex(self)
  }
  if self.cc != 0 {
    if isDigit(self.cc) {
      return makeIntToken(self)
    } elif isLetter(self.cc) {
      // might be string, keyword, boolean constant
      return makeTextToken(self)
    } else {
      return makeSymbolToken(self)
    }
  }

  return Token(TOKEN_EOF, "")
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
//                Lexer token values, for external consumption               //
///////////////////////////////////////////////////////////////////////////////

lexTokenType=0
lexTokenString=''
lexTokenInt=0
lexTokenKw=0
lexTokenBool=false

// Build a string token
Token: proc(type: int, value: string): string {
  lexTokenType = type
  lexTokenString = value
  lexTokenInt = -1
  lexTokenKw = -1
  lexTokenBool = false
  if debugLex {
    print "; Making token type: " print type print " value: "
    println value
  }
  return lexTokenString
}

// Build an int constant token
IntToken: proc(value: int, valueAsString: string): string {
  lexTokenType = TOKEN_INT
  lexTokenString = valueAsString
  lexTokenInt = value
  lexTokenKw = -1
  lexTokenBool = false
  if debugLex {
    print "; Making int token: " println value
  }
  return valueAsString
}

// Build a bool constant token
BoolToken: proc(value: bool, valueAsString: string): string {
  lexTokenType = TOKEN_BOOL
  lexTokenString = valueAsString
  lexTokenInt = -1
  lexTokenKw = -1
  lexTokenBool = value
  if debugLex {
    print "; Making bool token: " println value
  }
  return lexTokenString
}

// Build a keyword token
KeywordToken: proc(value: int, valueAsString: string): string {
  lexTokenType = TOKEN_KEYWORD
  lexTokenString = valueAsString
  lexTokenKw = value
  lexTokenInt = -1
  lexTokenBool = false
  if debugLex {
    print "; Making kw token: " println valueAsString
  }
  return lexTokenString
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

makeTextToken: proc(self: Lexer): string {
  value=''
  if self.cc == 95 {
    // do not allow leading _
    // TODO: Use error framework
    println "ERROR: Cannot start variable with an underscore"
    exit
  }
  if isLetter(self.cc) {
    value=value + chr(self.cc)
    advanceLex(self)
  }
  while isLetterOrDigit(self.cc) {
    value=value + chr(self.cc)
    advanceLex(self)
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

makeIntToken: proc(self: Lexer): string {
  value=0
  value_as_string = ''

  while isDigit(self.cc) do advanceLex(self) {
    // value=value * 10 + self.cc - asc('0')
    value=value * 10 + self.cc - 48
    value_as_string = value_as_string + chr(self.cc)
  }
  return IntToken(value, value_as_string)
}

startsWithSlash: proc(self: Lexer): string {
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
  return Token(TOKEN_DIV, '/')
}

startsWithBang: proc(self: Lexer): string {
  oc=self.cc
  advanceLex(self) // eat the !
  if self.cc == 61 {
    advanceLex(self)
    return Token(TOKEN_NEQ, '!=')
  }
  // TODO: use error framework
  print 'ERROR: Unknown character:' + chr(self.cc) + ' ASCII code: ' + toString(self.cc)
  exit
  // return Token(TOKEN_BIT_NOT, '!'))
}

startsWithPlus: proc(self: Lexer): string {
  oc=self.cc
  advanceLex(self)
  if self.cc == 43 {
    advanceLex(self)
    return Token(TOKEN_INC, '++')
  }
  return Token(TOKEN_PLUS, '+')
}

startsWithMinus: proc(self: Lexer): string {
  oc=self.cc
  advanceLex(self)
  if self.cc == 45 {
    advanceLex(self)
    return Token(TOKEN_DEC, '--')
  }
  return Token(TOKEN_MINUS, '-')
}

startsWithGt: proc(self: Lexer): string {
  oc=self.cc
  advanceLex(self)
  if self.cc == 61 {
    advanceLex(self)
    return Token(TOKEN_GEQ, '>=')
  //} elif self.cc == '>' {
    // shift right
    //advanceLex(self)
    //return Token(TOKEN_SHIFT_RIGHT, '>>')
  }
  return Token(TOKEN_GT, '>')
}

startsWithLt: proc(self: Lexer): string {
  oc=self.cc
  advanceLex(self)
  if self.cc == 61 {
    advanceLex(self)
    return Token(TOKEN_LEQ, '<=')
  //} elif self.cc == '<' {
    //// shift right
    //advanceLex(self)
    //return Token(TOKEN_SHIFT_LEFT, '<<')
  }
  return Token(TOKEN_LT, '<')
}

startsWithEq: proc(self: Lexer): string {
  oc=self.cc
  advanceLex(self)
  if self.cc == 61 {
      advanceLex(self)
    return Token(TOKEN_EQEQ, '==')
  }
  return Token(TOKEN_EQ, '=')
}

makeStringLiteralToken: proc(self: Lexer, firstQuote: int): string {
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
    // TODO: Use error framework
    print 'ERROR: Unclosed string literal ' println sb
    exit
  }

  advanceLex(self) // eat the closing tick/quote
  return Token(TOKEN_STRING, sb)
}

makeSymbolToken: proc(self: Lexer): string {
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
    return Token(TOKEN_LPAREN, '(')
  } elif oc == 41 {
    advanceLex(self)
    return Token(TOKEN_RPAREN, ')')
  } elif oc == 42 {
    advanceLex(self)
    return Token(TOKEN_MULT, '*')
  } elif oc == 47 {
    return startsWithSlash(self)
  } elif oc == 37 {
    advanceLex(self)
    return Token(TOKEN_MOD, '%')
  // } elif oc == asc('&') {
  //   advanceLex(self)
  //   return Token(TOKEN_AND, '&')
  // } elif oc == asc('|') {
  //   advanceLex(self)
  //   return Token(TOKEN_OR, '|')
  } elif oc == 33 {
    return startsWithBang(self)
  } elif oc == 123 {
    advanceLex(self)
    return Token(TOKEN_LBRACE, '{')
  } elif oc == 125 {
    advanceLex(self)
    return Token(TOKEN_RBRACE, '}')
  } elif oc == 91 {
    advanceLex(self)
    return Token(TOKEN_LBRACKET, '[')
  } elif oc == 93 {
    advanceLex(self)
    return Token(TOKEN_RBRACKET, ']')
  } elif oc == 58 {
    advanceLex(self)
    return Token(TOKEN_COLON, ':')
  } elif oc == 34 or oc == 39 { // double or single quote
    return makeStringLiteralToken(self, oc)
  } elif oc == 44 {
    advanceLex(self)
    return Token(TOKEN_COMMA, ',')
  } elif oc == 46 {
    advanceLex(self)
    return Token(TOKEN_DOT, '.')
  } else {
    // TODO: use error framework
    print 'ERROR: Unknown character:' + chr(self.cc) + ' ASCII code: ' println self.cc
    exit
  }
}

printToken: proc(self: Lexer) {
  if lexTokenType == TOKEN_EOF {
    println 'EOF'
  } elif lexTokenType == TOKEN_INT {
    print 'Int constant: ' println lexTokenInt
  } elif lexTokenType == TOKEN_STRING {
    print 'String constant: ' println lexTokenString
  } elif lexTokenType == TOKEN_BOOL {
    print 'Bool constant: ' println lexTokenBool
  } elif lexTokenType == TOKEN_KEYWORD {
    print 'Keyword: ' println lexTokenString
  } elif lexTokenType == TOKEN_VARIABLE {
    print 'Variable: ' println lexTokenString
  } else {
    print 'Token: ' print lexTokenString print ' type: ' println lexTokenType
  }
}

//text = input
//lex=newLexer(text)

//count = 1
//nextToken(lex)
//print "; line #:" print lex.line print ":" printToken(lex)

//while lexTokenType != TOKEN_EOF do count = count + 1 {
//  nextToken(lex)
//  print "; line #:" print lex.line print ":" printToken(lex)
//}

//print 'Total number of tokens: '
//print count print "\n"
//exit

