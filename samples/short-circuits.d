
g:proc{
  a=true b=true 
  println "In g:, should be true true"
  c=a and b println c 
  d=b and a println d
} 
g()

f:proc(a:bool, b:bool) {
  println "In f:"
  c=a and b println c 
  d=b and a println d
}
f(true, false)
f(false, true)
f(true, true)
