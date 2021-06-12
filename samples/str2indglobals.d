isDigit: proc(c: string): bool {
  return c >= '0' & c <= '9'
}

str2int: proc(s: string): int {
  val = 0
  i = 0 while isDigit(s[i]) do i = i + 1 {
    val = val * 10
    c = s[i]
    if c == '1' {val = val + 1}
    if c == '2' {val = val + 2}
    if c == '3' {val = val + 3}
    if c == '4' {val = val + 4}
    if c == '5' {val = val + 5}
    if c == '6' {val = val + 6}
    if c == '7' {val = val + 7}
    if c == '8' {val = val + 8}
    if c == '9' {val = val + 9}
  }
  return val
}

main {
  print "Should be 314159:"
  pi = str2int("314" + "159 ")
  print pi
}
