sz=2
arr:int[sz*5]
print "should be 10: " print length(arr) print "\n"


i=0 while i < length(arr) do i = i + 1 {
  arr[i] = -10 + i * 3
}
i=0 while i < length(arr) do i = i + 1 {
  print "arr[" print i print "]=" print arr[i] print "\n"
}

a=arr[1]
print "Should be -7: " print a print "\n"

f:proc() {
  // testing local arrays, and different types
  arrs:string[12]
  print "should be 12: " print length(arrs) print "\n"
  arrs[0] = "hi"
  arrs[1] = " "
  arrs[2] = "there"
  print "Should be hi there: " 
  i = 0 while i < 3 do i = i + 1 {print arrs[i]} print "\n"

  arrb:bool[2]
  print "should be 2: " print length(arrb) print "\n"
  arrb[0]=true
  arrb[1]=false
  print "Should be true false: " 
  i = 0 while i < 2 do i = i + 1 {print arrb[i] print " "} print "\n"

  // testing that we can access a global array from inside a proc
  print "should be 10: " print length(arr) print "\n"
}
f()
