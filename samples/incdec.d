a=3
a = a + 1
b=4
b = b - 1
c = 5
c = 1 + c

p:proc(n:int):int {
  m = n + 1 + 6
  print m
  n = n + 1
  m = 1 + m
  return n + m
}

pa = p(a)
println pa
if pa != 5 {
  exit "Unexpected result"
}

