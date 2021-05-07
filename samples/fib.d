// Define recursive fibonacci
proc fib(n: int) returns int {
  if n <= 1 {
    return n
  } else {
    // forward call
    return fib(n-1) + fib2(n-2)
  }
}

// Define iterative fibonacci
proc fib2(n: int) returns int {
  n1 = 0
  n2 = 1
  i=1 while i < n do i += 1 {
    nth = n1 + n2
    n1 = n2
    n2 = nth
  }
  return nth
}

main {
  f1 = fib(5)
  f2 = fib2(5)
  if f1 != f2 {
    print "Oops, they're different..."
    print "f1=%d, f2=%d",f1,f2
  } else {
    print "Yay, same: %d", f1
  }
}

