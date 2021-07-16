a=3
a = a + 1
b=4
b = b - 1
c = b + 1

p:proc(n:int):int {
  m = n + 1 + 6
  print m
  n = n + 1
  return n
}

pa = p(a)
println pa
if pa != 5 {
  exit "Unexpected result"
}

