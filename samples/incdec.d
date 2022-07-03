a=3
a = a + 1  // 4
println a
b=4
b = b - 1 // 3
println b
c = 5
c = 1 + c // 6
println c

p:proc(n:int):int {
  // n = 4
  m = n + 1 + 6 // 11
  print m // print 11
  n = n + 1 // 5
  m = 1 + m // 12
  return n + m // 17
}

pa = p(a)
println pa // print 17
if pa != 17 {
  exit "Unexpected result"
}

