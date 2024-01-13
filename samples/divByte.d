div:proc(num:byte, denum:byte):byte {
  return num/denum
}

c=-0y73
d=0y06
result = div(c, d)
println result
print c print "/" print d print " = " print result
println " (s/b) = -19 / 0yed"

c=-c
result = div(c, d)
print c print "/" print d print " = " print result
println " (s/b) = 19 / 0y13"

d=-d
result = div(c, d)
print c print "/" print d print " = " print result
println " (s/b) = -19 / 0yed"
