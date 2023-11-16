g:proc:void{
  a=false b=true 
  println "In g:, should be false false"
  c=a and b println c 
  d=b and a println d
} 
g()

f:proc(a:bool, b:bool):void {
  println "In f:"
  print "a=" println a
  print "b=" println b
  c=a or b print "c=a or b" println c 
  d=b or a print "d=d or b" println d
}
f(true, false)
f(false, true)
f(true, true)

