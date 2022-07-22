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
  it = new Lexer
  it.text = text
  it.line = 1
  it.cc = ''
  advance(it)
  return it
}

nextToken: proc(it: Lexer): Token {
  // skip unwanted whitespace
  while (it.cc == ' ' or it.cc == '\n' or it.cc == '\t' or it.cc == '\r') {
    if (it.cc == '\r') {
      it.line=it.line + 1
      it.col=0
    }
    advance(it)
  }

  start=makePosition(it)
  if (isDigit(it.cc)) {
    return makeInt(it, start)
  } elif (isLetter(it.cc)) {
    return makeText(it, start)
  } elif (it.cc != '') {
    return makeSymbol(it, start)
  }

  return makeToken(Type_EOF, start, start, '')
}

advance: proc(it: Lexer) {
  if it.loc < length(it.text) {
    // the parens are required for some stupid reason. wth?
    temptext=it.text
    it.cc=temptext[it.loc]
    it.col=it.col + 1
  } else {
    // Indicates no more characters
    it.cc=''
  }
  it.loc=it.loc + 1
}

isLetter: proc(c: string): bool {
  return (c >= 'a' and c <= 'z') or (c>='A' and c <= 'Z') or c=='_'
}

isDigit: proc(c: string): bool {
  return c >= '0' and c <= '9'
}

isLetterOrDigit: proc(c: string): bool {
  return isLetter(c) or isDigit(c)
}

makeText: proc(it: Lexer, start: Position): Token {
  value=''
  if (isLetter(it.cc)) {
    value=value + it.cc
    advance(it)
  }
  while (isLetterOrDigit(it.cc)) {
    value=value + it.cc
    advance(it)
  }
  end=makePosition(it)
  // TODO: don't allow leading __ 

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

makeInt: proc(it: Lexer, start: Position): Token {
  value=0
  value_as_string = ''
  while it.cc >= '0' and it.cc <= '9' do advance(it) {
    value=value * 10 + (asc(it.cc) - asc('0'))
    value_as_string = value_as_string + it.cc
  }
  end = makePosition(it)
  token = makeToken(Type_INT, start, end, value_as_string)
  token.int_value = value
  return token
}

makeSymbol: proc(it: Lexer, start: Position): Token {
  oc=it.cc
  if oc == '=' {
    return startsWithEq(it, start)
  } elif oc == '<' {
    return startsWithLt(it, start)
  } elif oc == '>' {
    return startsWithGt(it, start)
  } elif oc == '+' {
    advance(it)
    return makeToken(Type_PLUS, start, start, oc)
  } elif oc == '-' {
    advance(it)
    return makeToken(Type_MINUS, start, start, oc)
  } elif oc == '(' {
    advance(it)
    return makeToken(Type_LPAREN, start, start, oc)
  } elif oc == ')' {
    advance(it)
    return makeToken(Type_RPAREN, start, start, oc)
  } elif oc == '*' {
    advance(it)
    return makeToken(Type_MULT, start, start, oc)
  } elif oc == '/' {
    return startsWithSlash(it, start)
  } elif oc == '%' {
    advance(it)
    return makeToken(Type_MOD, start, start, oc)
  } elif oc == '&' {
    advance(it)
    return makeToken(Type_AND, start, start, oc)
  } elif oc == '|' {
    advance(it)
    return makeToken(Type_OR, start, start, oc)
  } elif oc == '!' {
    return startsWithNot(it, start)
  } elif oc == '^' {
    advance(it)
    return makeToken(Type_XOR, start, start, oc)
  } elif oc == '{' {
    advance(it)
    return makeToken(Type_LBRACE, start, start, oc)
  } elif oc == '}' {
    advance(it)
    return makeToken(Type_RBRACE, start, start, oc)
  } elif oc == '[' {
    advance(it)
    return makeToken(Type_LBRACKET, start, start, oc)
  } elif oc == ']' {
    advance(it)
    return makeToken(Type_RBRACKET, start, start, oc)
  } elif oc == ':' {
    advance(it)
    return makeToken(Type_COLON, start, start, oc)
  } elif oc == chr(34) or oc == chr(39) {
    return makeStringToken(it, start, oc)
  } elif oc == ',' {
    advance(it)
    return makeToken(Type_COMMA, start, start, oc)
  } elif oc == '.' {
    advance(it)
    return makeToken(Type_DOT, start, start, oc)
  } else {
    error = 'Unknown character:' + it.cc + ' ASCII code: ' + toString(asc(it.cc))
    exit error
  }
}

startsWithSlash: proc(it: Lexer, start: Position): Token {
  advance(it) // eat the first slash
  if (it.cc == '/') {
    advance(it) // eat the second slash
    while (it.cc !='\n' and it.cc != '') {
      advance(it)
    }
    if (it.cc != '') {
      advance(it)
    }
    it.line=it.line + 1
    it.col=0
    return nextToken(it)
  }
  return makeToken(Type_DIV, start, start, '/')
}

startsWithNot: proc(it: Lexer, start: Position): Token {
  oc=it.cc
  advance(it)
  if (it.cc == '=') {
    end=makePosition(it)
    advance(it)
    return makeToken(Type_NEQ, start, end, '!=')
  }
  return makeToken(Type_NOT, start, start, oc)
}

startsWithGt: proc(it: Lexer, start: Position): Token {
  oc=it.cc
  advance(it)
  if (it.cc == '=') {
    end=makePosition(it)
    advance(it)
    return makeToken(Type_GEQ, start, end, '>=')
  }
  return makeToken(Type_GT, start, start, oc)
}

startsWithLt: proc(it: Lexer, start: Position): Token {
  oc=it.cc
  advance(it)
  if (it.cc == '=') {
    end=makePosition(it)
    advance(it)
    return makeToken(Type_LEQ, start, end, '<=')
  }
  return makeToken(Type_LT, start, end, oc)
}

startsWithEq: proc(it: Lexer, start: Position): Token {
  oc=it.cc
  advance(it)
  if (it.cc == '=') {
    end=makePosition(it)
    advance(it)
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

makeStringToken: proc(it: Lexer, start: Position, first: String): Token {
  advance(it) // eat the tick/quote
  sb=''
  // TODO: fix backslash-escaping
  while it.cc != first and it.cc !='' {
    sb = sb + it.cc
    advance(it)
  }

  if (it.cc == '') {
    exit 'Unclosed string literal at ' + toString(it.line)
  }

  advance(it) // eat the closing tick/quote
  end=makePosition(it)
  return makeToken(Type_STRING, start, end, sb)
}

makePosition: proc(it: Lexer): Position {
  pos = new Position
  pos.line = it.line
  pos.col = it.col
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
  //println toString(token.type)
  if token.type == Type_EOF {
    println 'Token: EOF'
  } elif token.type == Type_INT {
    print 'Int token: '
    println token.int_value
  } elif token.type == Type_STRING {
    print 'String token: ' + chr(39)
    print token.value
    println chr(39)
  } elif token.type == Type_KEYWORD {
    print 'Keyword token: '
    println token.value
  } else {
    print 'Token: ' print token.value print ' (type: ' print token.type println ')'
  }
}

main {
  text = input
  lexer = new_lexer(text)

  count = 1
  token = nextToken(lexer)
  printToken(token)

  while token.type != Type_EOF do count = count + 1 {
    token = nextToken(lexer)
    printToken(token)
  }
  print 'Total number of tokens: '
  println count
}
