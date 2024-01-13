div:proc(num:int, denum:int):int {
  return num/denum
}

c=-1234560
d=2340
result = div(c, d)
print c print "/" print d print " = " print result
println " (s/b) = -527"

c=-c
result = div(c, d)
print c print "/" print d print " = " print result
println " (s/b) = 527"

d=-d
result = div(c, d)
print c print "/" print d print " = " print result
println " (s/b) = -527"
