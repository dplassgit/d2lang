
n=50
n1 = 0
n2 = 1
i=1 while i < n *2 do i = i+1 {
  if (i%2)==0 {
    continue
  }
  nth = n1 + n2
  n1 = n2
  n2 = nth
  print i/2 
  print " th fib: " // NOTE: starting the string with a t screws up the Interpreter... 
  println nth
}
println '' // NOTE: cannot just do PRINT (no expression)...
print "Final fib: "
println nth
