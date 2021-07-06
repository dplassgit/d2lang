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
Type_LBRACKET=29
Type_RBRACKET=30
Type_DOT=31
Type_SHIFT_LEFT=32
Type_SHIFT_RIGHT=33
Type_XOR=34

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
'not',
'xor'
]

Position: record {
  line: int
  col: int
}
  
Token: record {
  type: int
  start: Position
  end: Position
  value: String
  int_value: int
}

Lexer: record {
  text: string // full text
  line: int
  col: int 
  loc: int  // location inside text
  cc: string // current character
}

new_lexer: proc(text: string): Lexer {
  this = new Lexer
  this.text = text
  this.line = 1
  advance(this)
  return this
}

advance: proc(this: Lexer) {
  if this.loc < length(this.text) {
    this.cc=this.text[(this.loc)]
    this.col=this.col + 1
  } else {
    // Indicates no more characters
    this.cc=''
  }
  this.loc=this.loc + 1
}

nextToken: proc(this: Lexer): Token {
  // skip unwanted whitespace
  while (this.cc == ' ' or this.cc == '\n' or this.cc == '\t' or this.cc == '\r') {
    if (this.cc == '\n') {
      this.line=this.line + 1
      this.col=0
    }
    advance(this)
  }

  start=makePosition(this)
  if (isDigit(this.cc)) {
    return makeInt(this, start)
  } elif (isLetter(this.cc)) {
    return makeText(this, start)
  } elif (this.cc != '') {
    return makeSymbol(this, start)
  }

  return makeToken(Type_EOF, start, start, '')
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

makeText: proc(this: Lexer, start: Position): Token {
  value=''
  if (isLetter(this.cc)) {
    value=value + this.cc
    advance(this)
  }
  while (isLetterOrDigit(this.cc)) {
    value=value + this.cc
    advance(this)
  }
  end=makePosition(this)
  // TODOgdon't allow leading __

  if value == 'true' {
    return makeToken(Type_TRUE, start, end, value)
  } elif value == 'false' {
    return makeToken(Type_FALSE, start, end, value)
  }

  i=0 while i < length(KEYWORDS) do i = i + 1 {
    if value == KEYWORDS[i] {
      return makeToken(Type_KEYWORD, start, end, value)
    }
  }

  return makeToken(Type_VARIABLE, start, end, value)
}

makeInt: proc(this: Lexer, start: Position): Token {
  value=0
  value_as_string = ''
  while this.cc >= '0' and this.cc <= '9' do advance(this) {
    value=value * 10 + (asc(this.cc) - asc('0'))
    value_as_string = value_as_string + this.cc
  }
  end=makePosition(this)
  token = new Token
  token.type = Type_INT
  token.start = start
  token.end = end
  token.value = value_as_string
  token.int_value = value
  return token
}

makeSymbol: proc(this: Lexer, start: Position): Token {
  oc=this.cc
  if oc == '=' {
    return startsWithEq(this, start)
  } elif oc == '<' {
    return startsWithLt(this, start)
  } elif oc == '>' {
    return startsWithGt(this, start)
  } elif oc == '+' {
    advance(this)
    return makeToken(Type_PLUS, start, start, oc)
  } elif oc == '-' {
    advance(this)
    return makeToken(Type_MINUS, start, start, oc)
  } elif oc == '(' {
    advance(this)
    return makeToken(Type_LPAREN, start, start, oc)
  } elif oc == ')' {
    advance(this)
    return makeToken(Type_RPAREN, start, start, oc)
  } elif oc == '*' {
    advance(this)
    return makeToken(Type_MULT, start, start, oc)
  } elif oc == '/' {
    return startsWithSlash(this, start)
  } elif oc == '%' {
    advance(this)
    return makeToken(Type_MOD, start, start, oc)
  } elif oc == '&' {
    advance(this)
    return makeToken(Type_AND, start, start, oc)
  } elif oc == '|' {
    advance(this)
    return makeToken(Type_OR, start, start, oc)
  } elif oc == '!' {
    return startsWithNot(this, start)
  } elif oc == '^' {
    advance(this)
    return makeToken(Type_XOR, start, start, oc)
  } elif oc == '{' {
    advance(this)
    return makeToken(Type_LBRACE, start, start, oc)
  } elif oc == '}' {
    advance(this)
    return makeToken(Type_RBRACE, start, start, oc)
  } elif oc == '[' {
    advance(this)
    return makeToken(Type_LBRACKET, start, start, oc)
  } elif oc == ']' {
    advance(this)
    return makeToken(Type_RBRACKET, start, start, oc)
  } elif oc == ':' {
    advance(this)
    return makeToken(Type_COLON, start, start, oc)
  } elif oc == chr(34) or oc == chr(39) {
    return makeStringToken(this, start, oc)
  } elif oc == ',' {
    advance(this)
    return makeToken(Type_COMMA, start, start, oc)
  } elif oc == '.' {
    advance(this)
    return makeToken(Type_DOT, start, start, oc)
  } else {
    error = 'Unknown character:' + this.cc + ' ASCII code: ' + toString(asc(this.cc))
    exit error
  }
}

startsWithSlash: proc(this: Lexer, start: Position): Token {
  advance(this) // eat the first slash
  if (this.cc == '/') {
    advance(this) // eat the second slash
    while (this.cc !='\n' and this.cc != '') {
      advance(this)
    }
    if (this.cc != '') {
      advance(this)
    }
    this.line=this.line + 1
    this.col=0
    return nextToken(this)
  }
  return makeToken(Type_DIV, start, start, '/')
}

startsWithNot: proc(this: Lexer, start: Position): Token {
  oc=this.cc
  advance(this)
  if (this.cc == '=') {
    end=makePosition(this)
    advance(this)
    return makeToken(Type_NEQ, start, end, '!=')
  }
  return makeToken(Type_NOT, start, start, oc)
}

startsWithGt: proc(this: Lexer, start: Position): Token {
  oc=this.cc
  advance(this)
  if (this.cc == '=') {
    end=makePosition(this)
    advance(this)
    return makeToken(Type_GEQ, start, end, '>=')
  }
  t = new Token
  t.type = Type_GT
  t.start = start
  t.value = oc
  return t
}

startsWithLt: proc(this: Lexer, start: Position): Token {
  oc=this.cc
  advance(this)
  if (this.cc == '=') {
    end=makePosition(this)
    advance(this)
    return makeToken(Type_LEQ, start, end, '<=')
  }
  return makeToken(Type_LT, start, end, oc)
}

startsWithEq: proc(this: Lexer, start: Position): Token {
  oc=this.cc
  advance(this)
  if (this.cc == '=') {
    end=makePosition(this)
    advance(this)
    return makeToken(Type_EQEQ, start, end, '==')
  }
  return makeToken(Type_EQ, start, start, oc)
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

makeStringToken: proc(this: Lexer, start: Position, first: String): Token {
  advance(this) // eat the tick/quote
  sb=''
  // TODO: fix backslash-escaping
  while this.cc != first and this.cc !='' {
    sb = sb + this.cc
    advance(this)
  }

  if (this.cc == '') {
    exit 'Unclosed string literal at ' + toString(this.line)
  }

  advance(this) // eat the closing tick/quote
  end=makePosition(this)
  return makeToken(Type_STRING, start, end, sb)
}

makePosition: proc(this: Lexer): Position {
  pos = new Position
  pos.line = this.line
  pos.col = this.col
  return pos
}

makeToken: proc(type: int, start: Position, end: Position, text: String): Token {
  token = new Token
  token.type = type
  token.start = start
  token.end = end
  token.value = text
  return token
}

printToken: proc(token:Token) {
  if token.type == Type_EOF {
    println 'Token: EOF'
  } elif token.type == Type_INT {
    println 'Int token: ' + toString(token.int_value)
  } elif token.type == Type_STRING {
    println 'String token: ' + chr(39) + token.value + chr(39)
  } elif token.type == Type_KEYWORD {
    println 'Keyword token: ' + token.value
  } else {
    println 'Token: ' + token.value + ' (type: ' + toString(token.type) + ')'
  }
}

