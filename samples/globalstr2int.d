isDigit: proc(c: string): bool {
  return c >= '0' and c <= '9'
}

lexer_text: string // full text
lexer_loc: int  // location inside text
lexer_cc: string // current character

advance: proc() {
  if lexer_loc < length(lexer_text) {
    lexer_cc=lexer_text[lexer_loc]
  } else {
    // Indicates no more characters
    lexer_cc=''
  }
  lexer_loc=lexer_loc + 1
}

makeInt: proc(): int {
  value=0
  while isDigit(lexer_cc) do advance() {
    value = value * 10
    c = asc(lexer_cc) - asc('0')
    value = value + c
  }
  return value
}

new_lexer: proc(text: string) {
  lexer_text = text
  lexer_loc = 0
  lexer_cc = ''
  advance()
}

new_lexer("314" + "159 $")
println "Should be 314159:"
pi = makeInt()
println pi
if pi != 314159 {
  exit "Bad result!"
}
