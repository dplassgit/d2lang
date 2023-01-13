max = 0y10

// Define iterative fibonacci
iterative_fib:proc(n: byte): int {
  n1 = 0
  n2 = 1
  nth = 0
  i=0y01 while i < n do i++ {
    nth = n1 + n2
    n1 = n2
    n2 = nth
    println nth
  }
  return nth
}


iterative = iterative_fib(max)
print "iterative=" println iterative

