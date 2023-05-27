// Define iterative fibonacci
iterative_fib:proc(n: int): int {
  n1 = 0
  n2 = 1
  nth = 0
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


n = 10

iterative = iterative_fib(n)
print "iterative=" print iterative print "\n"

recursive = recursive_fib(n)
print "recursive=" print recursive print "\n"

if recursive != iterative {
  print "Oops, they're different...\n"
} else {
  print "Yay, same!\n"
}
