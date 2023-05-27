isPrime:proc(n:int):bool {
  m = 2
  while m <= n/2 do m = m + 1 {
    other = n/m
    if other * m == n {
      // this may fail because d only works with ints...
      return false
    }
  }
  return true
}

println "Primes between 1 and 100:"
i=1 while i < 100 do i = i + 1 {
  if isPrime(i) {
    println i
  }
}
