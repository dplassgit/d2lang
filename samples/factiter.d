
fact: proc(n: int):int {
  ans = 1
  i = 1 while i < n do i++ {
    ans = ans * i
  }
  return ans
}

println fact(10)
