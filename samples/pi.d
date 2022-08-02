d = 1.0
// Initialize sum
sum = 0.0

i = 0 while i < 1000000 do i = i + 1 {
   // even index add to sum
   if i % 2 == 0 {
       sum  = sum + 4.0/d
   }
   else {
     // odd index subtract from sum
       sum = sum -  4.0/d
   }

  // increment denominator by 2
  d = d + 2.0

  print "Value of Pi is: " println sum
}
