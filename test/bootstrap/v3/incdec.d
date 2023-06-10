a=1
a++
print "should be 2: " println a

f:proc(arg: int):int {
  arg++
  return arg
}

print "Should be 3: " println f(2)

g:proc(arg: int): int {
  local = arg
  local++
  return local
}
print "Should be 4: " println g(3)
