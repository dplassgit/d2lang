x=3.14
println "global 3.14"
println x
//println 3
//println 3.000001

//g()
println '"3."'
println 3.
f(3.)
println '"3.0"'
println 3.0
f(3.0)
println '"3.1"'
println 3.1
f(3.1)
println '"3.14"'
println 3.14
f(3.14)
println '"3.000001"'
println 3.000001
f(3.000001)


f:proc(z:double) {
  println z
}

g:proc {
  y:double
  y=6.28
  println y
}
