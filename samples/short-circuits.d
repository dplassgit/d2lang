
//f:proc{a=true b=true c=a %s b print c d=b %s a print d} f()
g:proc{
  a=false b=true 
  println "In g:, should be false false"
  c=a and b println c 
  d=b and a println d
} 
g()

f:proc(a:bool, b:bool) {
  println "In f:"
  c=a and b println c 
  d=b and a println d
}
//f(true, false)
//f(false, true)
//f(true, true)

