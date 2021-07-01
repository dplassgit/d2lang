
f:proc(n:int):int {
   sum = 0
   i = 0 while i < n do i = i + 1 {
     x = n * 3
     sum = sum + i
   }
   return sum
}

val = f(10)
println val
if val != 45 {
  exit "Should have been 45"
}
