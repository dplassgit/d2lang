Type_EOF=0
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
Type_TRUE=27
Type_FALSE=28
Type_LBRACKET=29
Type_RBRACKET=30
Type_DOT=31
Type_SHIFT_LEFT=32
Type_SHIFT_RIGHT=33

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
'string',
'record',
'new',
'null',
'delete',
'input',
'length',
'chr',
'asc',
'exit',
'and',
'or',
'not'
]

// Global for lexer: 
lexer_text: string // full text
lexer_line: int
lexer_col: int 
lexer_loc: int  // location inside text
lexer_cc: string // current character

new_lexer: proc(text: string) {
  lexer_text = text
  lexer_line = 1
  lexer_col = 0
  lexer_loc = 0
  lexer_cc = ''
  advance()
}

nextToken: proc(): String {
  // skip unwanted whitespace
  while (lexer_cc == ' ' or lexer_cc == chr(10) or lexer_cc == chr(9) or lexer_cc == chr(13)) {
    if (lexer_cc == chr(10)) {
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

advance: proc() {
  if lexer_loc < length(lexer_text) {
    lexer_cc=lexer_text[lexer_loc]
    lexer_col=lexer_col + 1
  } else {
    // Indicates no more characters
    lexer_cc=''
  }
  lexer_loc=lexer_loc + 1
}

// Bundle the data about the token in a single string of the format
// 'token <type> <value>'
Token: proc(type: int, value: string): string {
  return 't ' + toString(type) + ' ' + value
}

// Bundle the data about the token in a single string of the format
// 'inttoken <value>'
IntToken: proc(type: int, ti: int, value: string): string {
  return 'i ' + value
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

isLetter: proc(c: string): bool {
  return (c>='a' and c <= 'z') or (c>='A' and c <= 'Z') or c=='_'
}

isDigit: proc(c: string): bool {
  return c>='0' and c <= '9'
}

isLetterOrDigit: proc(c: string): bool {
  return isLetter(c) or isDigit(c)
}

makeText: proc(): String {
  value=''
  if (isLetter(lexer_cc)) {
    value=value + lexer_cc
    advance()
  }
  while (isLetterOrDigit(lexer_cc)) {
    value=value + lexer_cc
    advance()
  }

  if value == 'true' {
    return Token(Type_TRUE, value)
  } elif value == 'false' {
    return Token(Type_FALSE, value)
  }

  i=0 while i < length(KEYWORDS) do i = i + 1 {
    if value == KEYWORDS[i] {
      return Token(Type_KEYWORD, value)
    }
  }

  return Token(Type_VARIABLE, value)
}

makeInt: proc(): string {
  value=0
  value_as_string = ''

  while isDigit(lexer_cc) do advance() {
    value=value * 10 + asc(lexer_cc) - asc('0')
    value_as_string = value_as_string + lexer_cc
  }
  return IntToken(Type_INT, value, value_as_string)
}

startsWithSlash: proc(): String {
  advance() // eat the first slash
  if lexer_cc == '/' {
    // Comment.
    advance() // eat the second slash
    // Eat characters until newline
    while lexer_cc != '\n' and lexer_cc != '' do advance() {}
    if lexer_cc != '' {
      advance() // eat the newline
    }
    lexer_line=lexer_line + 1
    lexer_col=0
    return nextToken()
  }
  return Token(Type_DIV, '/')
}

startsWithNot: proc(): String{
  oc=lexer_cc
  advance()
  if (lexer_cc == '=') {
    advance()
    return Token(Type_NEQ, '!=')
  }
  return Token(Type_NOT, oc)
}

startsWithGt: proc(): String{
  oc=lexer_cc
  advance()
  if (lexer_cc == '=') {
    advance()
    return Token(Type_GEQ, '>=')
  }
  // TODO: shift right
  return Token(Type_GT, oc)
}

startsWithLt: proc(): String{
  oc=lexer_cc
  advance()
  if (lexer_cc == '=') {
    advance()
    return Token(Type_LEQ, '<=')
  }
  // TODO: shift left
  return Token(Type_LT, oc)
}

startsWithEq: proc(): String{
  oc=lexer_cc
  advance()
  if (lexer_cc == '=') {
    advance()
    return Token(Type_EQEQ, '==')
  }
  return Token(Type_EQ, oc)
}

makeString: proc(first: String): string {
  advance() // eat the tick/quote
  sb=''
  while lexer_cc != first and lexer_cc != '' {
    sb=sb + lexer_cc
    advance()
  }

  if (lexer_cc == '') {
    exit 'Unclosed string literal at ' + toString(lexer_line)
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
  } elif oc == '[' {
    advance()
    return Token(Type_LBRACKET, oc)
  } elif oc == ']' {
    advance()
    return Token(Type_RBRACKET, oc)
  } elif oc == ':' {
    advance()
    return Token(Type_COLON, oc)
  } elif oc == chr(34) or oc == chr(39) {
    return makeString(oc)
  } elif oc == ',' {
    advance()
    return Token(Type_COMMA, oc)
  } elif oc == '.' {
    advance()
    return Token(Type_DOT, oc)
  } else {
    error = 'Unknown character:' + lexer_cc + ' ASCII code: ' + toString(asc(lexer_cc))
    exit error
  }
}

tokenType:proc(token:string): int  {
  if token[0] == 'i' {
    // int token
    return Type_INT
  } else {
    // regular token
    val = 0
    i = 2 while token[i] != ' ' do i = i + 1 {
      val = val * 10 + asc(token[i])-asc('0')
    }
    return val
  }
}

intTokenVal:proc(token:string): int  {
  val = 0
  i = 2 while i < length(token) do i = i + 1 {
    val = val * 10 + asc(token[i])-asc('0')
  }
  return val
}

tokenVal:proc(token:string): string  {
  i = 2 while token[i] != ' ' do i = i + 1 {}

  val = ''
  i = i + 1 while i < length(token) do i = i + 1 {
    val = val + token[i]
  }
  return val
}

printToken: proc(token:string) {
  if tokenType(token) == Type_EOF {
    println 'Token: EOF'
  } elif tokenType(token) == Type_INT {
    println 'Int token: ' + toString(intTokenVal(token))
  } elif tokenType(token) == Type_STRING {
    println 'String token: ' + chr(39) + tokenVal(token) + chr(39)
  } elif tokenType(token) == Type_KEYWORD {
    println 'Keyword token: ' + tokenVal(token)
  } else {
    println 'Token: ' + tokenVal(token) + ' (type: ' + toString(tokenType(token)) + ')'
  }
}

main {
  text = input
  new_lexer(text)

  count = 1
  token = nextToken()
  printToken(token)

  while tokenType(token) != Type_EOF do count = count + 1 {
    token = nextToken()
    printToken(token)
  }
  println 'Total number of tokens: ' + toString(count)
}
