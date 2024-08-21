msg="hi"
ga=7
gb=ga+1

p: proc(a:int): int {
  gb = a*5
  c = gb + 1
  print msg print ":"
  return (a+gb)*c+c
}

print p(5)
