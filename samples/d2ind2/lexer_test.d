nextToken_singleSymbols:proc() {
  lexer = new_lexer("+-*/%()! =<>|&{}:")
  token = nextToken(lexer)
  assertThat(token.type == Type_PLUS)
  token = nextToken(lexer)
  assertThat(token.type == Type_MINUS)
  token = nextToken(lexer)
  assertThat(token.type == Type_MULT)
  token = nextToken(lexer)
  assertThat(token.type == Type_DIV)
  token = nextToken(lexer)
  assertThat(token.type == Type_MOD)
  token = nextToken(lexer)
  assertThat(token.type == Type_LPAREN)
  token = nextToken(lexer)
  assertThat(token.type == Type_RPAREN)
  token = nextToken(lexer)
  assertThat(token.type == Type_NOT)
  token = nextToken(lexer)
  assertThat(token.type == Type_EQ)
  token = nextToken(lexer)
  assertThat(token.type == Type_LT)
  token = nextToken(lexer)
  assertThat(token.type == Type_GT)
  token = nextToken(lexer)
  assertThat(token.type == Type_OR)
  token = nextToken(lexer)
  assertThat(token.type == Type_AND)
  token = nextToken(lexer)
  assertThat(token.type == Type_LBRACE)
  token = nextToken(lexer)
  assertThat(token.type == Type_RBRACE)
  token = nextToken(lexer)
  assertThat(token.type == Type_COLON)
}


nextToken_doubleSymbols:proc() {
  lexer = new_lexer("== <= >= !=")
  token = nextToken(lexer)
  assertThat(token.type == Type_EQEQ)
  token = nextToken(lexer)
  assertThat(token.type == Type_LEQ)
  token = nextToken(lexer)
  assertThat(token.type == Type_GEQ)
  token = nextToken(lexer)
  assertThat(token.type == Type_NEQ)
}


nextToken_int:proc() {
  lexer = new_lexer("3")
  Inttoken = (IntToken) nextToken(lexer)
  assertThat(token.type == Type_INT)
  assertThat(token.value == 3)
}


nextToken_longerint:proc() {
  lexer = new_lexer("1234")
  token = nextToken(lexer)
  assertThat(token.type == Type_INT)
  assertThat(token.text == "1234")
}


nextToken_eof:proc() {
  lexer = new_lexer("1")
  nextToken(lexer)
  token = nextToken(lexer)
  assertThat(token.type == Type_EOF)
}


nextToken_twoNumbers:proc() {
  lexer = new_lexer("1 2")
  token = nextToken(lexer)
  assertThat(token.type == Type_INT)
  assertThat(token.text == "1")

  token = nextToken(lexer)
  assertThat(token.type == Type_INT)
  assertThat(token.text == "2")
}


nextToken_whiteSpace:proc() {
  lexer = new_lexer("1\n\t 23")
  token = nextToken(lexer)
  assertThat(token.type == Type_INT)
  assertThat(token.text == "1")
  assertThat(token.value == 1)
  assertThat(token.start.line == 1)
  assertThat(token.start.column == 1)
  assertThat(token.end.line == 1)
  assertThat(token.end.column == 2)

  token = nextToken(lexer)
  assertThat(token.type == Type_INT)
  assertThat(token.text == "23")
  assertThat(token.value == 23)
  assertThat(token.start.line == 2)
  assertThat(token.start.column == 3)
  assertThat(token.end.line == 2)
  assertThat(token.end.column == 4)
}


nextToken_trueFalse:proc() {
  lexer = new_lexer("true false True FALSE")
  token = nextToken(lexer)
  assertThat(token.value)
  token = nextToken(lexer)
  assertThat(token.value == false)
  token = nextToken(lexer)
  assertThat(token.value)
  token = nextToken(lexer)
  assertThat(token.value == false)
}


nextToken_keyword:proc() {
  lexer = new_lexer(
          "print PrintLN IF Else elif do while break continue int bool proc return")

  token = nextToken(lexer)
  assertThat(token.keyword() == KeywordType_PRINT)
  token = (KeywordToken) nextToken(lexer)
  assertThat(token.keyword() == KeywordType_PRINTLN)
  token = (KeywordToken) nextToken(lexer)
  assertThat(token.keyword() == KeywordType_IF)
  token = (KeywordToken) nextToken(lexer)
  assertThat(token.keyword() == KeywordType_ELSE)
  token = (KeywordToken) nextToken(lexer)
  assertThat(token.keyword() == KeywordType_ELIF)
  token = (KeywordToken) nextToken(lexer)
  assertThat(token.keyword() == KeywordType_DO)
  token = (KeywordToken) nextToken(lexer)
  assertThat(token.keyword() == KeywordType_WHILE)
  token = (KeywordToken) nextToken(lexer)
  assertThat(token.keyword() == KeywordType_BREAK)
  token = (KeywordToken) nextToken(lexer)
  assertThat(token.keyword() == KeywordType_CONTINUE)
  token = (KeywordToken) nextToken(lexer)
  assertThat(token.keyword() == KeywordType_INT)
  token = (KeywordToken) nextToken(lexer)
  assertThat(token.keyword() == KeywordType_BOOL)
  token = (KeywordToken) nextToken(lexer)
  assertThat(token.keyword() == KeywordType_PROC)
  token = (KeywordToken) nextToken(lexer)
  assertThat(token.keyword() == KeywordType_RETURN)
}


nextToken_mixed:proc() {
  lexer = new_lexer("print 3 p")
  Keywordtoken = (KeywordToken) nextToken(lexer)
  assertThat(token.type == Type_KEYWORD)
  assertThat(token.keyword() == KeywordType_PRINT)

  IntToken intToken = (IntToken) nextToken(lexer)
  assertThat(intToken.type == Type_INT)
  assertThat(intToken.value() == 3)

  Token varToken = nextToken(lexer)
  assertThat(varToken.type == Type_VARIABLE)
  assertThat(varToken.text == "p")
}


nextToken_longNotKeyword:proc() {
  lexer = new_lexer("printed")
  token = nextToken(lexer)
  assertThat(token.type == Type_VARIABLE)
  assertThat(token.text == "printed")
}


nextToken_variable:proc() {
  lexer = new_lexer("prin")
  token = nextToken(lexer)
  assertThat(token.type == Type_VARIABLE)
  assertThat(token.text == "prin")
}


nextToken_variableAlnum:proc() {
  lexer = new_lexer("prin2")
  token = nextToken(lexer)
  assertThat(token.type == Type_VARIABLE)
  assertThat(token.text == "prin2")
}


nextToken_underscore:proc() {
  lexer = new_lexer("TYPE_token _token token_ _")
  token = nextToken(lexer)
  assertThat(token.type == Type_VARIABLE)
  assertThat(token.text == "TYPE_token")
  token = nextToken(lexer)
  assertThat(token.type == Type_VARIABLE)
  assertThat(token.text == "_token")
  token = nextToken(lexer)
  assertThat(token.type == Type_VARIABLE)
  assertThat(token.text == "token_")
  token = nextToken(lexer)
  assertThat(token.type == Type_VARIABLE)
  assertThat(token.text == "_")
}


nextToken_assign:proc() {
  lexer = new_lexer("a = 3")

  token = nextToken(lexer)
  assertThat(token.type == Type_VARIABLE)
  assertThat(token.text == "a")

  token = nextToken(lexer)
  assertThat(token.type == Type_EQ)

  token = nextToken(lexer)
  assertThat(token.type == Type_INT)
  assertThat(token.text == "3")
}


nextToken_assign_expr:proc() {
  lexer = new_lexer("a=3+4")

  token = nextToken(lexer)
  assertThat(token.type == Type_VARIABLE)
  assertThat(token.text == "a")

  token = nextToken(lexer)
  assertThat(token.type == Type_EQ)

  token = nextToken(lexer)
  assertThat(token.type == Type_INT)
  assertThat(token.text == "3")

  token = nextToken(lexer)
  assertThat(token.type == Type_PLUS)
  assertThat(token.text == "+")

  token = nextToken(lexer)
  assertThat(token.type == Type_INT)
  assertThat(token.text == "4")
}


nextToken_comment:proc() {
  lexer = new_lexer("1// ignored\na")

  token = nextToken(lexer)
  assertThat(token.type == Type_INT)
  assertThat(token.text == "1")

  token = nextToken(lexer)
  assertThat(token.type == Type_VARIABLE)
  assertThat(token.text == "a")

  token = nextToken(lexer)
  assertThat(token.type == Type_EOF)
}


nextToken_commentEof:proc() {
  lexer = new_lexer("1// ignored\n")

  token = nextToken(lexer)
  assertThat(token.type == Type_INT)
  assertThat(token.text == "1")

  token = nextToken(lexer)
  assertThat(token.type == Type_EOF)
}


nextToken_commentEol:proc() {
  lexer = new_lexer("1// ignored")

  token = nextToken(lexer)
  assertThat(token.type == Type_INT)
  assertThat(token.text == "1")

  token = nextToken(lexer)
  assertThat(token.type == Type_EOF)
}


nextToken_commentCrLf:proc() {
  lexer = new_lexer("1// ignored\r\n")

  token = nextToken(lexer)
  assertThat(token.type == Type_INT)
  assertThat(token.text == "1")

  token = nextToken(lexer)
  assertThat(token.type == Type_EOF)
}


nextToken_commentLfCr:proc() {
  lexer = new_lexer("1// ignored\n\r")

  token = nextToken(lexer)
  assertThat(token.type == Type_INT)
  assertThat(token.text == "1")

  token = nextToken(lexer)
  assertThat(token.type == Type_EOF)
}


nextToken_commentsEol:proc() {
  lexer = new_lexer("1// ignored\n// so is this...\nb")

  token = nextToken(lexer)
  assertThat(token.type == Type_INT)
  assertThat(token.text == "1")

  token = nextToken(lexer)
  assertThat(token.type == Type_VARIABLE)
  assertThat(token.text == "b")

  token = nextToken(lexer)
  assertThat(token.type == Type_EOF)
}


nextToken_stringTick:proc() {
  lexer = new_lexer("'Hi'")
  token = nextToken(lexer)
  assertThat(token.type == Type_STRING)
  assertThat(token.text == "Hi")
}


nextToken_stringQuotes:proc() {
  lexer = new_lexer('"Hi"')
  token = nextToken(lexer)
  assertThat(token.type == Type_STRING)
  assertThat(token.text == "Hi")
}

nextToken_stringEscapedQuotes:proc() {
  lexer = new_lexer("\"Hi\"")
  token = nextToken(lexer)
  assertThat(token.type == Type_STRING)
  assertThat(token.text == "Hi")
}


nextToken_stringEmpty:proc() {
  lexer = new_lexer("''")
  token = nextToken(lexer)
  assertThat(token.type == Type_STRING)
  assertThat(token.text == "")
}


nextToken_stringSpace:proc() {
  lexer = new_lexer("' '")
  token = nextToken(lexer)
  assertThat(token.type == Type_STRING)
  assertThat(token.text == " ")
}


nextToken_stringOpen_error:proc() {
  lexer = new_lexer("\"Hi")
  assertThrows(ScannerException.class, () -> nextToken(lexer))
}

assertThat: proc(expectedTrue: bool) {
  if !expectedTrue {
    error "Expected false to be true"
  }
}
