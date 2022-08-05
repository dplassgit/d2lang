sum=3.0
n=2.0
sign=1.0

// #for loop to add terms
i = 0 while i < 1000000 do i = i + 1 {
   
   sum=sum+(sign*(4.0/((n)*(n+1.0)*(n+2.0))))
   
   // #for addition and subtraction of alternate terms
   sign=sign*(-1.0) 
   
   // #Increment by 2 according to formula
   n=n+2.0

   print "Value if pi:" println sum
}
