///////////////////////////////////////////////////////////////////////////////
//                                    LEXER                                  //
//                           VERSION 1 (COMPILED BY V0)                      //
///////////////////////////////////////////////////////////////////////////////

// TODO (in no particular order)
//   compare string to null
//   records: declare with array field
//   array parameters
//   check negative index on array set
//   BUG: params (or locals) with the same name as a global gives precedence to the global, unlike D (java)
//   detect duplicate param names
//   detect duplicate record fields
//   ++, --
//   compare strings fully

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
TOKEN_DOT=29
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
lexCurrentLine=1

newLexer: proc(text: string) {
  lexerText = text
  resetLexer()
}

resetLexer: proc() {
  lexerLoc = 0
  lexerCc = 0
  lexCurrentLine=1
  advanceLex()
}

nextToken: proc(): string {
  // skip unwanted whitespace
  while (lexerCc == 32 or lexerCc == 10 or lexerCc == 9 or lexerCc == 13) {
    if lexerCc == 10 or lexerCc == 13 {
      lexCurrentLine = lexCurrentLine + 1
    }
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
//                Lexer token values, for external consumption               //
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
    print "; Making token type: " print type print " value: "
    print value print "\n"
  }
  return lexTokenString
}

// Bundle the data about the token in a single string of the format
// 'i <value>'
IntToken: proc(value: int, valueAsString: string): string {
  lexTokenType = TOKEN_INT
  lexTokenString = valueAsString
  lexTokenInt = value
  lexTokenKw = -1
  lexTokenBool = false
  if debug {
    print "; Making int token: " print value print "\n"
  }
  return valueAsString
}

// Bundle the data about the token in a single string of the format
// 'b true/false'
BoolToken: proc(value: bool, valueAsString: string): string {
  lexTokenType = TOKEN_BOOL
  lexTokenString = valueAsString
  lexTokenInt = -1
  lexTokenKw = -1
  lexTokenBool = value
  if debug {
    print "; Making bool token: " print value print "\n"
  }
  return lexTokenString
}

// Bundle the data about the token in a single string of the format
// 'k value'
KeywordToken: proc(value: int, valueAsString: string): string {
  lexTokenType = TOKEN_KEYWORD
  lexTokenString = valueAsString
  lexTokenKw = value
  lexTokenInt = -1
  lexTokenBool = false
  if debug {
    print "; Making kw token: " print valueAsString print "\n"
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
      lexCurrentLine = lexCurrentLine + 1
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
  } elif oc == 46 {
    advanceLex()
    return Token(TOKEN_DOT, '.')
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

generalError: proc(type: string, message: string) {
  print type print " error at line " print lexCurrentLine
  print ": " + message + "\n"
  exit
}

typeError: proc(message: string) {
  generalError("Type", message)
}

parserError: proc(message: string) {
  generalError("Parse", message)
}

