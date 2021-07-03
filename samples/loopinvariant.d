
loopinvariant:proc(n:int):int {
   sum = 0
   i = 0 while i < n do i = i + 1 {
     y = n + (n*4+5)/(n-1)  // can be lifted
     j = 1 while j < n do j = j + 1 {
       x = n * 3    // can be lifted
       k = 0 while k < n do k = k + 1 {
         z = 3      // can be lifted
         sum = sum + z
       }
     }
     sum = sum + i
   }
   return sum * z - x + y
}

val = loopinvariant(10)
println val
if val != 8220 {
  exit "Should have been 8220"
}
