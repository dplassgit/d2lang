 left=504
right=1711

mult: proc(left: int, right: int): int {
  return left * right
}

print "left * right: s/b 862344: "
println mult(left, right)
print "left * left: s/b 254016: "
println mult(left, left)
print "right * right: s/b 2927521: "
println mult(right, right)
print "left * 10: s/b 5040: "
println mult(left, 10)
print "700 * right: s/b 1197700: "
println mult(700, right)
print "-700 * right: s/b -1197700: "
println mult(-700, right)
