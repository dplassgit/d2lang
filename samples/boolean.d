globa = true
globb = globa and true
globc = globb and false
globd = true == true
globe = globa == globb
globf = 3 == 3
globg = 4 != 4


p:proc() {
  a = true
  b = a and true
  c = b and false
  d = true == true
  e = a == b
  f = 3 == 3
  g = 4 != 4
  println a
  println b
  println c
  println d
  println e
  println f
  println g
}

println globa
println globb
println globc
println globd
println globe
println globf
println globg
println ''
p()
