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
token_value: String
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
  lecer_cc = ''
  advance()
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

  return Token(Type_EOF)
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

  while lexer_cc >= '0' & lexer_cc <= '9' do advance() {
    value=value * 10 + (asc(lexer_cc) - asc('0'))
  }
  token_type = TYPE_int
  token_int = value
  return new Token(TYPE_int, start, end, new String(value), value)
}

makeSymbol: proc(this:record Lexer, start:record Position):record Token {
  oc=lexer_cc
  if oc == '=' {
    return startsWithEq(this, start)
  } elif oc == '<' {
    return startsWithLt(this, start)
  } elif oc == '>' {
    return startsWithGt(this, start)
  } elif oc == '+' {
    advance(this)
    return new Token(Type_PLUS, start, oc)
  } elif oc == '-' {
    advance(this)
    return new Token(Type_MINUS, start, oc)
  } elif oc == '(' {
    advance(this)
    return new Token(Type_LPAREN, start, oc)
  } elif oc == ')' {
    advance(this)
    return new Token(Type_RPAREN, start, oc)
  } elif oc == '*' {
    advance(this)
    return new Token(Type_MULT, start, oc)
  } elif oc == '/' {
    return startsWithSlash(this, start)
  } elif oc == '%' {
    advance(this)
    return new Token(Type_MOD, start, oc)
  } elif oc == '&' {
    advance(this)
    return new Token(Type_AND, start, oc)
  } elif oc == '|' {
    advance(this)
    return new Token(Type_OR, start, oc)
  } elif oc == '!' {
    return startsWithNot(this, start)
  } elif oc == '{' {
    advance(this)
    return new Token(Type_LBRACE, start, oc)
  } elif oc == '}' {
    advance(this)
    return new Token(Type_RBRACE, start, oc)
  } elif oc == ':' {
    advance(this)
    return new Token(Type_COLON, start, oc)
  } elif oc == '"'  | oc == '\'' {
    return makeStringrecord Token(start, oc)
  } elif oc == ',' {
    advance(this)
    return new Token(Type_COMMA, start, oc)
  } else {
    error "Unknown character %c" % lexer_cc
  }
}

startsWithSlash: proc(this: record Lexer, start: record Position): record Token {
  advance(this) // eat the first slash
  if (lexer_cc == '/') {
    advance(this) // eat the second slash
    while (lexer_cc !='\n' & lexer_cc !=0) {
      advance(this)
    }
    if (lexer_cc !=0) {
      advance(this)
    }
    lexer_line=lexer_line + 1
    lexer_col=0
    return nextrecord Token(this)
  }
  return new record Token(Type_DIV, start, '/')
}

startsWithNot: proc(this: record Lexer, start: record Position): record Token {
  oc=lexer_cc
  advance(this)
  if (lexer_cc == '=') {
    end=new record Position(lexer_line, lexer_col)
    advance(this)
    return new record Token(Type_NEQ, start, end, "!=")
  }
  return new record Token(Type_NOT, start, oc)
}

startsWithGt: proc(this: record Lexer, start: record Position): record Token {
  oc=lexer_cc
  advance(this)
  if (lexer_cc == '=') {
    end=new record Position(lexer_line, lexer_col)
    advance(this)
    return new record Token(Type_GEQ, start, end, ">=")
  }
  return new record Token(Type_GT, start, oc)
}

startsWithLt: proc(this: record Lexer, start: record Position): record Token {
  oc=lexer_cc
  advance(this)
  if (lexer_cc == '=') {
    end=new record Position(lexer_line, lexer_col)
    advance(this)
    return new record Token(Type_LEQ, start, end, "<=")
  }
  return new record Token(Type_LT, start, oc)
}

startsWithEq: proc(this: record Lexer, start: record Position): record Token {
  oc=lexer_cc
  advance(this)
  if (lexer_cc == '=') {
    end=new record Position(lexer_line, lexer_col)
    advance(this)
    return new record Token(Type_EQEQ, start, end, "==")
  }
  return new record Token(Type_EQ, start, oc)
}

makeStringToken: proc(this: record Lexer, start: record Position, first: String): record Token {
  advance(this) // eat the tick/quote
  sb=""
  escape=false
  // TODO: fix backslash-escaping
  while lexer_cc !=first & lexer_cc !=0 {
    if (!escape) {
      sb=sb + lexer_cc
    }
    advance(this)
  }

  if (lexer_cc == 0) {
    error "Unclosed string literal at %d,%d" % start.line, start.col
  }

  advance(this) // eat the closing tick/quote
  end=new record Position(lexer_line, lexer_col)
  return new record Token(Type_STRING, lexer_start, lexer_end, sb)
}
