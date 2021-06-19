globa = true
globb = globa & true
globc = globb and false
globd = true == true
globe = globa == globb
globf = 3 == 3
globg = 4 != 4


p:proc() {
  a = true
  b = a & true
  c = b and false
  d = true == true
  e = a == b
  f = 3 == 3
  g = 4 != 4
}

p()
