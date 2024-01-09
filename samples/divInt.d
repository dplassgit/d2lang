div:proc(num:int, denum:int):int {
  return num/denum
}

c=1234560
d=2340

result = div(c, d)
mod = c - result * d
print "calculated = " println result
print "mod= " println mod
print "check = " println result * d + mod
