unary:proc(i: int): int {
  i=-i
  j=-i
  return -j
}

print "negative 100: " println unary(100)
print "positive 100: " println unary(-100)
                   
