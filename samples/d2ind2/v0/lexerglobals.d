debug=false

///////////////////////////////////////////////////////////////////////////////
//                                     LEXER                                 //
///////////////////////////////////////////////////////////////////////////////
Type_EOF=0
Type_PLUS=1
Type_MINUS=2
Type_MULT=3
Type_AND=4 // bit and
Type_OR=5  // bit or
Type_DIV=6
Type_MOD=7
Type_INT=8   // int constant
Type_BOOL=9  // bool constant
Type_STRING=10 // string constant
Type_VARIABLE=11
Type_EQ=12
Type_NOT=13
Type_LPAREN=14
Type_RPAREN=15
Type_EQEQ=16
Type_LT=17
Type_GT=18
Type_LEQ=19
Type_GEQ=20
Type_NEQ=21
Type_LBRACE=22
Type_RBRACE=23
Type_COLON=24
Type_COMMA=25
Type_KEYWORD=26
//Type_TRUE=27
//Type_FALSE=28
Type_LBRACKET=29
Type_RBRACKET=30
//Type_DOT=31
//Type_SHIFT_LEFT=32
//Type_SHIFT_RIGHT=33

KW_print=0
KW_if=1
KW_else=2
KW_elif=3
KW_proc=4
KW_return=5
KW_while=6
KW_do=7
KW_break=8
KW_continue=9
KW_int=10
KW_bool=11
KW_string=12
KW_null=13
KW_input=14
KW_length=15
KW_chr=16
KW_asc=17
KW_exit=18
KW_and=19
KW_or=20
KW_not=21
//KW_main=22
//KW_record=23
//KW_new=24
//KW_delete=25
//KW_println=26

KEYWORDS:string[22]
KEYWORDS[KW_print]='print'
KEYWORDS[KW_if]='if'
KEYWORDS[KW_else]='else'
KEYWORDS[KW_elif]='elif'
KEYWORDS[KW_proc]='proc'
KEYWORDS[KW_return]='return'
KEYWORDS[KW_while]='while'
KEYWORDS[KW_do]='do'
KEYWORDS[KW_break]='break'
KEYWORDS[KW_continue]='continue'
KEYWORDS[KW_int]='int'
KEYWORDS[KW_bool]='bool'
KEYWORDS[KW_string]='string'
KEYWORDS[KW_null]='null'
KEYWORDS[KW_input]='input'
KEYWORDS[KW_length]='length'
KEYWORDS[KW_chr]='chr'
KEYWORDS[KW_asc]='asc'
KEYWORDS[KW_exit]='exit'
KEYWORDS[KW_and]='and'
KEYWORDS[KW_or]='or'
KEYWORDS[KW_not]='not'
//KEYWORDS[KW_println]='println'
//KEYWORDS[KW_main]='main'
//KEYWORDS[KW_record]='record'
//KEYWORDS[KW_new]='new'
//KEYWORDS[KW_delete]='delete'

// Global for lexer: 
lexerText: string // full text
lexerLoc: int  // location inside text
lexerCc: int // string // current character

newLexer: proc(text: string) {
  lexerText = text
  lexerCc = 0
  advanceLex()
}

nextToken: proc: string {
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

  return Token(Type_EOF, '')
}

advanceLex: proc {
  if lexerLoc < length(lexerText) {
    lexerCc=asc(lexerText[lexerLoc])
  } else {
    // Indicates no more characters
    lexerCc=0
  }
  lexerLoc = lexerLoc + 1
  if debug {
    print "Lexer cc " print lexerCc print ":" println chr(lexerCc)
  }
}

// Bundle the data about the token in a single string of the format
// 't <type> <value>'
Token: proc(type: int, value: string): string {
  return 't ' + toString(type) + ' ' + value
}

// Bundle the data about the token in a single string of the format
// 'i <value>'
IntToken: proc(value: string): string {
  return 'i ' + value
}

// Bundle the data about the token in a single string of the format
// 'b true/false'
BoolToken: proc(value: string): string {
  return 'b ' + value
}

toString: proc(i: int): string {
  if i == 0 {
    return '0'
  }
  val = ''
  while i > 0 do i = i / 10 {
    val = chr((i % 10) +asc('0')) + val
  }
  return val
}

isLetter: proc(c: int): bool {
  return (c>=asc('a') and c <= asc('z')) or (c>=asc('A') and c <= asc('Z')) or c==asc('_')
}

isDigit: proc(c: int): bool {
  return c>=asc('0') and c <= asc('9')
}

isLetterOrDigit: proc(c: int): bool {
  return isLetter(c) or isDigit(c)
}

makeTextToken: proc: string {
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

  if value == 'true' or value == 'false'{
    return BoolToken(value)
  }

  i=0 while i < length(KEYWORDS) do i = i + 1 {
    if value == KEYWORDS[i] {
      return Token(Type_KEYWORD, value)
    }
  }

  return Token(Type_VARIABLE, value)
}

makeIntToken: proc: string {
  value=0
  value_as_string = ''

  while isDigit(lexerCc) do advanceLex() {
    value=value * 10 + lexerCc - asc('0')
    value_as_string = value_as_string + chr(lexerCc)
  }
  return IntToken(value_as_string)
}

startsWithSlash: proc: string {
  advanceLex() // eat the first slash
  if lexerCc == asc('/') {
    // Comment.
    advanceLex() // eat the second slash
    // Eat characters until newline
    while lexerCc != 10 and lexerCc != 0 do advanceLex() {}
    if lexerCc != 0 {
      advanceLex() // eat the newline
    }
    return nextToken()
  }
  return Token(Type_DIV, '/')
}

startsWithNot: proc: string {
  oc=lexerCc
  advanceLex()
  if lexerCc == asc('=') {
    advanceLex()
    return Token(Type_NEQ, '!=')
  }
  exit 'Unknown character:' + chr(lexerCc) + ' ASCII code: ' + toString(lexerCc)
  // return Token(Type_NOT, chr(oc))
}

startsWithGt: proc: string {
  oc=lexerCc
  advanceLex()
  if lexerCc == asc('=') {
    advanceLex()
    return Token(Type_GEQ, '>=')
  //} elif lexerCc == '>' {
    // shift right
    //advanceLex()
    //return Token(Type_SHIFT_RIGHT, '>>')
  }
  return Token(Type_GT, chr(oc))
}

startsWithLt: proc: string {
  oc=lexerCc
  advanceLex()
  if lexerCc == asc('=') {
    advanceLex()
    return Token(Type_LEQ, '<=')
  //} elif lexerCc == '<' {
    //// shift right
    //advanceLex()
    //return Token(Type_SHIFT_LEFT, '<<')
  }
  return Token(Type_LT, chr(oc))
}

startsWithEq: proc: string {
  oc=lexerCc
  advanceLex()
  if lexerCc == asc('=') {
    advanceLex()
    return Token(Type_EQEQ, '==')
  }
  return Token(Type_EQ, chr(oc))
}

makeStringLiteralToken: proc(firstQuote: int): string {
  advanceLex() // eat the tick/quote
  sb=''
  while lexerCc != firstQuote and lexerCc != 0 {
    if lexerCc == 92 { // backslash
      advanceLex()
      if lexerCc == 110 { // backslash - n
        sb=sb + chr(10)  // lf
      } elif lexerCc == 92 {
        sb=sb + chr(92)  // literal backslash
      }
    } else {
      sb=sb + chr(lexerCc)
    }
    advanceLex()
  }

  if lexerCc == 0 {
    exit 'Unclosed string literal'
  }

  advanceLex() // eat the closing tick/quote
  return Token(Type_STRING, sb)
}

makeSymbolToken: proc: string {
  oc=lexerCc
  if oc == asc('=') {
    return startsWithEq()
  } elif oc == asc('<') {
    return startsWithLt()
  } elif oc == asc('>') {
    return startsWithGt()
  } elif oc == asc('+') {
    advanceLex()
    return Token(Type_PLUS, chr(oc))
  } elif oc == asc('-') {
    advanceLex()
    return Token(Type_MINUS, chr(oc))
  } elif oc == asc('(') {
    advanceLex()
    return Token(Type_LPAREN, chr(oc))
  } elif oc == asc(')') {
    advanceLex()
    return Token(Type_RPAREN, chr(oc))
  } elif oc == asc('*') {
    advanceLex()
    return Token(Type_MULT, chr(oc))
  } elif oc == asc('/') {
    return startsWithSlash()
  } elif oc == asc('%') {
    advanceLex()
    return Token(Type_MOD, chr(oc))
//  } elif oc == asc('&') {
 //   advanceLex()
  //  return Token(Type_AND, chr(oc))
  //} elif oc == asc('|') {
   // advanceLex()
   // return Token(Type_OR, chr(oc))
  } elif oc == asc('!') {
    return startsWithNot()
  } elif oc == asc('{') {
    advanceLex()
    return Token(Type_LBRACE, chr(oc))
  } elif oc == asc('}') {
    advanceLex()
    return Token(Type_RBRACE, chr(oc))
  } elif oc == asc('[') {
    advanceLex()
    return Token(Type_LBRACKET, chr(oc))
  } elif oc == asc(']') {
    advanceLex()
    return Token(Type_RBRACKET, chr(oc))
  } elif oc == asc(':') {
    advanceLex()
    return Token(Type_COLON, chr(oc))
  } elif oc == 34 or oc == 39 { // double or single quote
    return makeStringLiteralToken(oc)
  } elif oc == asc(',') {
    advanceLex()
    return Token(Type_COMMA, chr(oc))
  //} elif oc == '.' {
    //advanceLex()
    //return Token(Type_DOT, oc)
  } else {
    error = 'Unknown character:' + chr(lexerCc) + ' ASCII code: ' + toString(lexerCc)
    exit error
  }
}

getTokenType: proc(token: string): int  {
  if token[0] == 'i' {
    // int constant token
    return Type_INT
  } elif token[0] == 'b' {
    // bool constant token
    return Type_BOOL
  } else {
    // regular token
    val = 0
    i = 2 while token[i] != ' ' do i = i + 1 {
      val = val * 10 + asc(token[i])-asc('0')
    }
    return val
  }
}

intTokenVal: proc(token: string): int  {
  val = 0
  i = 2 while i < length(token) do i = i + 1 {
    val = val * 10 + asc(token[i])-asc('0')
  }
  return val
}

// Bool token format is 'b true' or 'b false'
boolTokenVal: proc(token: string): bool  {
  return token[2] == 't'
}

tokenVal: proc(token: string): string  {
  i = 2 while token[i] != ' ' do i = i + 1 {}

  val = ''
  i = i + 1 while i < length(token) do i = i + 1 {
    val = val + token[i]
  }
  return val
}

printToken: proc(token:string) {
  if getTokenType(token) == Type_EOF {
    print 'Token: EOF' print "\n"
  } elif getTokenType(token) == Type_INT {
    print 'Int token: ' + toString(intTokenVal(token)) print "\n"
  } elif getTokenType(token) == Type_STRING {
    print 'String token: ' + chr(39) + tokenVal(token) + chr(39) print "\n"
  } elif getTokenType(token) == Type_BOOL {
    if boolTokenVal(token) {
      print 'Bool token: true\n'
    } else {
      print 'Bool token: false\n'
    }
  } elif getTokenType(token) == Type_KEYWORD {
    print 'Keyword token: ' + tokenVal(token) print "\n"
  } else {
    print 'Token: ' + tokenVal(token) + ' (type: ' + toString(getTokenType(token)) + ')\n'
  }
}

text = input
newLexer(text)

count = 1
token = nextToken()
printToken(token)

while getTokenType(token) != Type_EOF do count = count + 1 {
  token = nextToken()
  printToken(token)
}

print 'Total number of tokens: ' 
print count print "\n"
