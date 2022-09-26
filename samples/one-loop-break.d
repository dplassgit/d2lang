oneLoopBreakDCO:proc(n:int):int {
  sum = 0
  i = 0 
  while i < 10 do i = i + 1 {
    sum = sum + 1
    break
  }
  return sum
}

println oneLoopBreakDCO(10)

