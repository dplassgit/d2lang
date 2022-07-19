Token: record {
  type: int
  start: Token
  end: Token
  value: String
}

makeToken: proc(type: int, start: Token, end: Token, text: String): Token {
  print "Making a token of value: "
  println text
  token = new Token
  token.type = type
  token.start = start
  token.end = end
  token.value = text
  print "Made a token of value: "
  println token.value
  print "Made a token of type: "
  println token.type
  return token
}

t = makeToken(1, null, null, "keyword1")
