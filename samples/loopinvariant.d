
f:proc(n:int):int {
   sum = 0
   i = 0 while i < n do i = i + 1 {
     y = n + (n*4+5)/(n-1)
     y = n + (n*4+5)/(n-2)
     j = 1 while j < n do j = j + 1 {
       x = n * 3 // can be lifted
     }
     sum = sum + i
   }
   return sum
}

val = f(10)
println val
if val != 45 {
  exit "Should have been 45"
}
