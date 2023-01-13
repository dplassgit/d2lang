
n=0y28  // 40
n1 = 0
n2 = 1
nth = 0
i=0y1 while i <= n do i++ {
  nth = n1 + n2
  n1 = n2
  n2 = nth
  print i 
  print "th fib: "
  println nth
}
println '' // NOTE: cannot just do PRINT (no expression)...
print "Final fib: "
println nth
