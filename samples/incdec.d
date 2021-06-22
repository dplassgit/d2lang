a=3
a = a + 1
b=4
b = b - 1
c = b + 1

p:proc(n:int):int {
  n = n + 1
  return n
}

print p(a)

