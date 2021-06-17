isDigit: proc(c: string): bool {
  return c >= '0' & c <= '9'
}

str2int: proc(s: string): int {
  val = 0
  i = 0 while isDigit(s[i]) do i = i + 1 {
    c = asc(s[i]) - asc('0')
    val = val * 10 + c
  }
  return val
}

main {
  println "Should be 314159:"
  pi = str2int("314" + "159 ")
  println pi
}
