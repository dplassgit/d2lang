globa = true
globb = globa and true
globc = globb and false
globd = true == true
globe = globa == globb
globf = 3 == 3
globg = 4 != 4


p:proc(x:int) {
  a = true
  b = a and true
  c = b and false
  d = true == a
  e = a == b
  f = x == x
  g = x != x
  println a
  println b
  println c
  println d
  println e
  println f
  println g
  println a<b
  println a<=b
  println a>=b
  println a>b
}

println globa
println globb
println globc
println globd
println globe
println globf
println globg
println ''
p(3)
