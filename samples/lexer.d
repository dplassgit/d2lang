Position: record {
  line: int
  col: int
}
  
Token: record {
  type: int
  start: Position
  end: Position
  value: String
}

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

Keywords: [
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
]

Lexer: record {
  text: string
  line: int
  col:int 
  // location inside text
  loc: int
  cc: string // current character
}

init_lexer: proc(text: string): Lexer {
  this=new Lexer(
    text, // text
    1,  // line
    0,  // col
    0,  //loc
    ''  // cc
  )
  advance(this)
  return this
}

advance: proc(this: Lexer) {
  if (this.loc < this.text.length()) {
    this.cc=this.text[this.loc]
    this.col=this.col + 1
  } else {
    // Indicates no more characters
    this.cc=0
  }
  this.loc=this.loc + 1
}

nextToken: proc(this: Lexer): Token {
  // skip unwanted whitespace
  while (this.cc == ' ' | this.cc == '\n' | this.cc == '\t' | this.cc == '\r') {
    if (cc == '\n') {
      this.line=this.line + 1
      this.col=0
    }
    advance(this)
  }

  start=new Position(this.line, this.col)
  if (isDigit(this.cc)) {
    return makeInt(this, start)
  } else if (isLetter(this.cc)) {
    return makeText(this, start)
  } else if (cc !=0) {
    return makeSymbol(this, start)
  }

  return new Token(Type_EOF, this.start)
}

makeText:proc(start:Position):Token {
  value=""
  if (isLetter(this.cc)) {
    value=value + this.cc
    advance(this)
  }
  while (isLetterOrDigit(this.cc)) {
    value=value + this.cc
    advance(this)
  }
  end=new Position(this.line, this.col)

  if value == 'true' {
    return new Token(TOKEN_true, start)
  } elif value == 'false' {
    return new Token(TOKEN_false, start)
  }

  i=0 while i < Keywords.length() do i = i + 1 {
    if value == Keywords[i] {
      return new Token(Type_KEYWORD, start, end, value)
  }

  return new Token(Type_VARIABLE, start, end, value)
}

makeInt:proc(this:Lexer, start:Position):IntToken  {
  value=0
  while this.cc >='0' & this.cc <='9' do advance(this) {
    value=value * 10 + (asc(this.cc[0]) - '0')
  }
  end=new Position(this.line, this.col)
  return new IntToken(start, end, value)
}

makeSymbol:proc(this:Lexer, start:Position):Token {
  oc=this.cc
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
    return makeStringToken(start, oc)
  } elif oc == ',' {
    advance(this)
    return new Token(Type_COMMA, start, oc)
  } else {
    error "Unknown character %c" % this.cc
  }
}

startsWithSlash:proc(this: Lexer, start: Position): Token {
  advance(this) // eat the first slash
  if (this.cc == '/') {
    advance(this) // eat the second slash
    while (this.cc !='\n' & this.cc !=0) {
      advance(this)
    }
    if (this.cc !=0) {
      advance(this)
    }
    this.line=this.line + 1
    this.col=0
    return nextToken(this)
  }
  return new Token(Type_DIV, start, '/')
}

startsWithNot:proc(this: Lexer, start: Position) :Token{
  oc=this.cc
  advance(this)
  if (this.cc == '=') {
    end=new Position(this.line, this.col)
    advance(this)
    return new Token(Type_NEQ, start, end, "!=")
  }
  return new Token(Type_NOT, start, oc)
}

startsWithGt:proc(this: Lexer, start: Position) :Token{
  oc=this.cc
  advance(this)
  if (this.cc == '=') {
    end=new Position(this.line, this.col)
    advance(this)
    return new Token(Type_GEQ, start, end, ">=")
  }
  return new Token(Type_GT, start, oc)
}

startsWithLt:proc(this: Lexer, start: Position) :Token{
  oc=this.cc
  advance(this)
  if (this.cc == '=') {
    end=new Position(this.line, this.col)
    advance(this)
    return new Token(Type_LEQ, start, end, "<=")
  }
  return new Token(Type_LT, start, oc)
}

startsWithEq:proc(this: Lexer, start: Position) :Token{
  oc=this.cc
  advance(this)
  if (this.cc == '=') {
    end=new Position(this.line, this.col)
    advance(this)
    return new Token(Type_EQEQ, start, end, " == ")
  }
  return new Token(Type_EQ, start, oc)
}

makeStringToken:proc(this: Lexer, start: Position, first: String) :Token {
  advance(this) // eat the tick/quote
  sb=""
  escape=false
  // TODO: fix backslash-escaping
  while this.cc !=first & this.cc !=0 {
    if (!escape) {
      sb=sb + this.cc
    }
    advance(this)
  }

  if (this.cc == 0) {
    error "Unclosed string literal at %d,%d" % start.line, start.col
  }

  advance(this) // eat the closing tick/quote
  end=new Position(this.line, this.col)
  return new Token(Type_STRING, this.start, this.end, sb)
}
