f:proc(a:int) {
  b=4 
  b=b << a 
  println a 
  a=a<<b 
  println a 
  c=a<<2 
  println c
} 
f(2)

