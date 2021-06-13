Type_INT=1
Type_BOOL=2
Type_STRING=3
Type_VARIABLE=4
Type_EQ=5
Type_NOT=6
Type_PLUS=7
Type_MINUS=8
Type_LPAREN=9
Type_RPAREN=10
Type_MULT=11
Type_DIV=12
Type_MOD=13
Type_AND=14
Type_OR=15
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
Type_EOF=0
Type_TRUE=27
Type_FALSE=28

KEYWORDS=[
'print',
'println',
'true',
'false',
'if',
'else',
'elif',
'main',
'proc',
'return',
'while',
'do',
'break',
'continue',
'int',
'bool',
'string'
// 'record'
// 'error'
]

// Global for token:
token_type: int
token_int: int
token_string: string

// Global for lexer:
lexer_text: string // full text
lexer_line: int
lexer_col:int 
lexer_loc: int  // location inside text
lexer_cc: string // current character

Token: proc(type: int, value: string): string {
  token_type = type
  token_string = value
  return "Token: " + value
}

IntToken: proc(type: int, ti: int, value: string): string {
  Token(type, value)
  token_int = ti
  return "IntToken: " + value
}

isLetter: proc(c:string):bool {
  return (c>='a' & c <= 'z') | (c>='A' & c <= 'Z') | c=='_'
}

isDigit: proc(c:string):bool {
  return c>='0' & c <= '9'
}

isLetterOrDigit: proc(c:string):bool {
  return isLetter(c) | isDigit(c)
}

advance: proc() {
  // if (lexer_text[lexer_loc] != '$') { //lexer_loc < lexer_text.length) {
  if lexer_loc < length(lexer_text) {
    lexer_cc=lexer_text[lexer_loc]
    lexer_col=lexer_col + 1
  } else {
    // Indicates no more characters
    lexer_cc=''
  }
  lexer_loc=lexer_loc + 1
}

new_lexer: proc(text: string) {
  lexer_text = text
  lexer_line = 1
  lexer_col = 0
  lexer_loc = 0
  lexer_cc = ''
  advance()
}

makeText: proc():String {
  value=""
  if (isLetter(lexer_cc)) {
    value=value + lexer_cc
    advance()
  }
  while (isLetterOrDigit(lexer_cc)) {
    value=value + lexer_cc
    advance()
  }

  if value == 'true' {
    return Token(Type_TRUE, 'true')
  } elif value == 'false' {
    return Token(Type_FALSE, 'false')
  }

  i=0 while i < length(KEYWORDS) do i = i + 1 {
    if value == KEYWORDS[i] {
      return Token(Type_KEYWORD, value)
    }
  }

  return Token(Type_VARIABLE, value)
}

makeInt: proc():string {
  value=0
  value_as_string = ''

  while isDigit(lexer_cc) do advance() {
    value=value * 10 + asc(lexer_cc) - asc('0')
    value_as_string = value_as_string + lexer_cc
  }
  return IntToken(Type_INT, value, value_as_string)
}

startsWithSlash: proc():String {
  advance() // eat the first slash
  if (lexer_cc == '/') {
    advance() // eat the second slash
    while (lexer_cc !='\n' & lexer_cc != '') {
      advance()
    }
    if (lexer_cc != '') {
      advance()
    }
    lexer_line=lexer_line + 1
    lexer_col=0
    return nextToken()
  }
  return Token(Type_DIV, '/')
}

startsWithNot: proc():String{
  oc=lexer_cc
  advance()
  if (lexer_cc == '=') {
    advance()
    return Token(Type_NEQ, "!=")
  }
  return Token(Type_NOT, oc)
}

startsWithGt: proc():String{
  oc=lexer_cc
  advance()
  if (lexer_cc == '=') {
    advance()
    return Token(Type_GEQ, ">=")
  }
  return Token(Type_GT, oc)
}

startsWithLt: proc():String{
  oc=lexer_cc
  advance()
  if (lexer_cc == '=') {
    advance()
    return Token(Type_LEQ, "<=")
  }
  return Token(Type_LT, oc)
}

startsWithEq: proc():String{
  oc=lexer_cc
  advance()
  if (lexer_cc == '=') {
    advance()
    return Token(Type_EQEQ, "==")
  }
  return Token(Type_EQ, oc)
}

makeString: proc(first: String): string {
  advance() // eat the tick/quote
  sb=""
  while lexer_cc != first & lexer_cc != '' {
    sb=sb + lexer_cc
    advance()
  }

  if (lexer_cc == '') {
    print "Unclosed string literal at "  println lexer_line
    print 1/0
  }

  advance() // eat the closing tick/quote
  return Token(Type_STRING, sb)
}

makeSymbol: proc(): string {
  oc=lexer_cc
  if oc == '=' {
    return startsWithEq()
  } elif oc == '<' {
    return startsWithLt()
  } elif oc == '>' {
    return startsWithGt()
  } elif oc == '+' {
    advance()
    return Token(Type_PLUS, oc)
  } elif oc == '-' {
    advance()
    return Token(Type_MINUS, oc)
  } elif oc == '(' {
    advance()
    return Token(Type_LPAREN, oc)
  } elif oc == ')' {
    advance()
    return Token(Type_RPAREN, oc)
  } elif oc == '*' {
    advance()
    return Token(Type_MULT, oc)
  } elif oc == '/' {
    return startsWithSlash()
  } elif oc == '%' {
    advance()
    return Token(Type_MOD, oc)
  } elif oc == '&' {
    advance()
    return Token(Type_AND, oc)
  } elif oc == '|' {
    advance()
    return Token(Type_OR, oc)
  } elif oc == '!' {
    return startsWithNot()
  } elif oc == '{' {
    advance()
    return Token(Type_LBRACE, oc)
  } elif oc == '}' {
    advance()
    return Token(Type_RBRACE, oc)
  } elif oc == ':' {
    advance()
    return Token(Type_COLON, oc)
  } elif oc == '"'  | oc == "'" { // d FTW
    return makeString(oc)
  } elif oc == ',' {
    advance()
    return Token(Type_COMMA, oc)
  } else {
    print "Unknown character" + lexer_cc
    print 1/0
    return Token(Type_EOF, 'Unknown')
  }
}

nextToken: proc(): String {
  // skip unwanted whitespace
  while (lexer_cc == ' ' | lexer_cc == '\n' | lexer_cc == '\t' | lexer_cc == '\r') {
    if (lexer_cc == '\n') {
      lexer_line=lexer_line + 1
      lexer_col=0
    }
    advance()
  }

  if (isDigit(lexer_cc)) {
    return makeInt()
  } elif (isLetter(lexer_cc)) {
    return makeText()
  } elif (lexer_cc != '') {
    return makeSymbol()
  }

  return Token(Type_EOF, '')
}

main {
  text = "print 'hi' a = a + 314 0"
  new_lexer(text)

  print nextToken() print " " println token_type

  if token_type != Type_KEYWORD {
    print "Expected " print Type_KEYWORD print " but was " println token_type
    print 1/0
  }

  while token_type != Type_EOF {
    print nextToken() print " " println token_type
  }

}
