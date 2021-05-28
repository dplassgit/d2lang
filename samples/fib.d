// Define recursive fibonacci
recursive_fib: proc(n: int) : int {
  if n <= 1 {
    return n
  } else {
    // forward call
    return recursive_fib(n - 1) + iterative_fib(n - 2)
  }
}

// Define iterative fibonacci
iterative_fib:proc(n: int): int {
  n1 = 0
  n2 = 1
  i=1 while i < n do i = i + 1 {
    nth = n1 + n2
    n1 = n2
    n2 = nth
  }
  return nth
}

main {
  recursive = recursive_fib(5)
  iterative = iterative_fib(5)
  if recursive != iterative {
    println "Oops, they're different..."
    print "recursive=" print recursive 
    print ", iterative=" println iterative
  } else {
    print "Yay, same: " println recursive
  }
}

