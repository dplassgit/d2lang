isDigit: proc(c: string): bool {
  return c >= '0' and c <= '9'
}

main {
  println isDigit('no')
  println isDigit('3')
}
