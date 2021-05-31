
fact: proc(n: int):int {
  if n == 1 {
    return 1
  } else {
    return n * fact(n-1)
  }
}

print fact(10)
