
f: proc(factor:int): int {
   sum = 0
   i = 0 
   while i < 10 do i = i + 1 {
     sum = sum + factor*i
   }
   return sum
}

print f(2)
