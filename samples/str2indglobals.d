isDigit: proc(c: string): bool {
  return c >= '0' & c <= '9'
}

lexer_text: string // full text
lexer_loc: int  // location inside text
lexer_cc: string // current character

advance: proc() {
  if (lexer_text[lexer_loc] != '$') {
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
    if lexer_cc == '1' { value = value + 1 }
    elif lexer_cc == '2' { value = value + 2 }
    elif lexer_cc == '3' { value = value + 3 }
    elif lexer_cc == '4' { value = value + 4 }
    elif lexer_cc == '5' { value = value + 5 }
    elif lexer_cc == '6' { value = value + 6 }
    elif lexer_cc == '7' { value = value + 7 }
    elif lexer_cc == '8' { value = value + 8 }
    elif lexer_cc == '9' { value = value + 9 }
  }
  return value
}

new_lexer: proc(text: string) {
  lexer_text = text
  lexer_loc = 0
  lexer_cc = ''
  advance()
}

main {
  new_lexer("314" + "159 $")
  print "Should be 314159:"
  print makeInt()
}
