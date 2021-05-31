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

// Define recursive fibonacci
recursive_fib: proc(n: int) : int {
  if n <= 1 {
    return n
  } else {
    return recursive_fib(n - 1) + recursive_fib(n - 2)
  }
}


main {
  n = 10

  iterative = iterative_fib(n)
  print "iterative=" println iterative

  recursive = recursive_fib(n)
  print "recursive=" println recursive 

  if recursive != iterative {
    println "Oops, they're different..."
  } else {
    println "Yay, same!"
  }
}

