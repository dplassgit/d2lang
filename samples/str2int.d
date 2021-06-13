isDigit: proc(c: string): bool {
  return c >= '0' & c <= '9'
}

str2int: proc(s: string): int {
  val = 0
  i = 0 while isDigit(s[i]) do i = i + 1 {
    val = val * 10
    c = s[i]
    if c == '1' {val = val + 1}
    elif c == '2' {val = val + 2}
    elif c == '3' {val = val + 3}
    elif c == '4' {val = val + 4}
    elif c == '5' {val = val + 5}
    elif c == '6' {val = val + 6}
    elif c == '7' {val = val + 7}
    elif c == '8' {val = val + 8}
    elif c == '9' {val = val + 9}
  }
  return val
}

main {
  print "Should be 314159:"
  pi = str2int("314" + "159 ")
  print pi
}
