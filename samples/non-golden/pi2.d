calc_pi: proc: double {
  pi=3.0
  n=2.0
  sign=1.0

  // #for loop to add terms
  i = 0 while i < 70 do i = i + 1 {
    pi=pi+(sign*(4.0/((n)*(n+1.0)*(n+2.0))))

    // #for addition and subtraction of alternate terms
    sign=sign*(-1.0) 

    // #Increment by 2 according to formula
    n=n+2.0

    println pi
  }

  return pi
}

println "Estimates of pi:" 
pi = calc_pi()
print "Final value: " println pi
