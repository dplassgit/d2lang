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
'string',
'record',
'error'
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

new_lexer: proc(text: string) {
  lexer_text = text
  lexer_line = 1
  lexer_col = 0
  lexer_loc = 0
  lexer_cc = ''
  advance()
}

Token: proc(type: int, value: string) {
  token_type = type
  token_string = value
}

IntToken: proc(type: int, ti: int, value: string) {
  Token(type, value)
  token_int = ti
}

advance: proc() {
  if (lexer_loc < lexer_text.length) {
    lexer_cc=lexer_text[lexer_loc]
    lexer_col=lexer_col + 1
  } else {
    // Indicates no more characters
    lexer_cc=0
  }
  lexer_loc=lexer_loc + 1
}

nextToken: proc(): String {
  // skip unwanted whitespace
  while (lexer_cc == ' ' | lexer_cc == '\n' | lexer_cc == '\t' | lexer_cc == '\r') {
    if (cc == '\n') {
      lexer_line=lexer_line + 1
      lexer_col=0
    }
    advance()
  }

  if (isDigit(lexer_cc)) {
    return makeInt()
  } else if (isLetter(lexer_cc)) {
    return makeText()
  } else if (cc !=0) {
    return makeSymbol()
  }

  return Token(Type_EOF, '')
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

Token: proc(type: int): String {
  token_type = type
  return ""
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
    return Token(Type_TRUE)
  } elif value == 'false' {
    return Token(Type_FALSE)
  }

  i=0 while i < KEYWORDS.length do i = i + 1 {
    if value == KEYWORDS[i] {
      return Token(Type_KEYWORD, value, i)
    }
  }

  return Token(Type_VARIABLE, value)
}

makeInt: proc() {
  value=0
  value_as_string = ''

  while isDigit(lexer_cc) do advance() {
    value=value * 10
    if lexer_cc == '1' { value = value + 1 }
    elif lexer_cc == '2' { value = value + 2 }
    elif lexer_cc == '3' { value = value + 3 }
    elif lexer_cc == '4' { value = value + 4 }
    elif lexer_cc == '5' { value = value + 5 }
    elif lexer_cc == '6' { value = value + 6 }
    elif lexer_cc == '7' { value = value + 7 }
    elif lexer_cc == '8' { value = value + 8 }
    elif lexer_cc == '9' { value = value + 9 }

    // value = value * 10 + (asc(lexer_cc) - 48)
    value_as_string = value_as_string + lexer_cc
  }
  return IntToken(TYPE_int, value, value_as_string)
}

makeSymbol: proc(){
  oc=lexer_cc
  if oc == '=' {
    return startsWithEq()
  } elif oc == '<' {
    return startsWithLt()
  } elif oc == '>' {
    return startsWithGt()
  } elif oc == '+' {
    advance()
    return new Token(Type_PLUS, oc)
  } elif oc == '-' {
    advance()
    return new Token(Type_MINUS, oc)
  } elif oc == '(' {
    advance()
    return new Token(Type_LPAREN, oc)
  } elif oc == ')' {
    advance()
    return new Token(Type_RPAREN, oc)
  } elif oc == '*' {
    advance()
    return new Token(Type_MULT, oc)
  } elif oc == '/' {
    return startsWithSlash()
  } elif oc == '%' {
    advance()
    return new Token(Type_MOD, oc)
  } elif oc == '&' {
    advance()
    return new Token(Type_AND, oc)
  } elif oc == '|' {
    advance()
    return new Token(Type_OR, oc)
  } elif oc == '!' {
    return startsWithNot()
  } elif oc == '{' {
    advance()
    return new Token(Type_LBRACE, oc)
  } elif oc == '}' {
    advance()
    return new Token(Type_RBRACE, oc)
  } elif oc == ':' {
    advance()
    return new Token(Type_COLON, oc)
  } elif oc == '"'  | oc == "'" { // d FTW
    return makeString(oc)
  } elif oc == ',' {
    advance()
    return new Token(Type_COMMA, oc)
  } else {
    error "Unknown character" + lexer_cc
  }
}

startsWithSlash: proc() {
  advance() // eat the first slash
  if (lexer_cc == '/') {
    advance() // eat the second slash
    while (lexer_cc !='\n' & lexer_cc !=0) {
      advance()
    }
    if (lexer_cc !=0) {
      advance()
    }
    lexer_line=lexer_line + 1
    lexer_col=0
    return nextToken()
  }
  return new record Token(Type_DIV, '/')
}

startsWithNot: proc(){
  oc=lexer_cc
  advance()
  if (lexer_cc == '=') {
    end=new record Position(lexer_line, lexer_col)
    advance()
    return new record Token(Type_NEQ, "!=")
  }
  return new record Token(Type_NOT, oc)
}

startsWithGt: proc(){
  oc=lexer_cc
  advance()
  if (lexer_cc == '=') {
    end=new record Position(lexer_line, lexer_col)
    advance()
    return new record Token(Type_GEQ, ">=")
  }
  return new record Token(Type_GT, oc)
}

startsWithLt: proc(){
  oc=lexer_cc
  advance()
  if (lexer_cc == '=') {
    end=new record Position(lexer_line, lexer_col)
    advance()
    return new record Token(Type_LEQ, "<=")
  }
  return new record Token(Type_LT, oc)
}

startsWithEq: proc(){
  oc=lexer_cc
  advance()
  if (lexer_cc == '=') {
    end=new record Position(lexer_line, lexer_col)
    advance()
    return new record Token(Type_EQEQ, "==")
  }
  return new record Token(Type_EQ, oc)
}

makeStringToken: proc(first: String){
  advance() // eat the tick/quote
  sb=""
  escape=false
  // TODO: fix backslash-escaping
  while lexer_cc != first & lexer_cc !=0 {
    if (!escape) {
      sb=sb + lexer_cc
    }
    advance()
  }

  if (lexer_cc == 0) {
    error "Unclosed string literal at " + lexer_line
  }

  advance() // eat the closing tick/quote
  return new record Token(Type_STRING, sb)
}
