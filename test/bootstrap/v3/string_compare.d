isDigit: proc(c: string): bool {
  return c >= '0' and c <= '9'
}

print "Should be false: " println isDigit('no')
print "Should be false: " println isDigit('N')
print "Should be false: " println isDigit('@')
print "Should be false: " println isDigit(']')
print "Should be true: " println isDigit('0')
print "Should be true: " println isDigit('3')
print "Should be true: " println isDigit('9')
