n1 = 0
n2 = 1
nth=0
i=1 while i <= 10 {
  nth = n1 + n2
  n1 = n2
  n2 = nth
  print i 
  print "th fib: "
  print nth print "\n"
  i=i+1
}
print "Final fib should be 89: "
print nth print "\n\n"

print "\nWhile with continue and break:\n"
i=0 while i < 20 {
  if i < 5 {
    print "Skipping " print i print "...\n"
    i = i + 1
    continue   // this skips the i=i+2, unlike the "do" version
  }
  if i > 10 {
    print "Stopping at 10\n"
    break
  }
  print i print "\n"
  i = i + 2
}
print "after while should be 10: " print i print "\n"

print "\nWhile with do:\n"
i=0 while i < 20 do i = i + 2 {
  if i < 5 {
    print "Skipping " print i print "...\n"
    i = i + 1
    continue   // this runs the i=i+2, unlike the non-"do" version
  }
  if i > 10 {
    print "Stopping at 10\n"
    break
  }
  print i print "\n"
}
print "after while do should be 10: " print i print "\n"

print "\nNested whiles:\n"
i=0 while i < 5 do i = i + 1 {
  j = 0 while j < 5 do j = j + 1 {
     if j > 2 {
       print " break j\n"
       break
     }
    print " j=" print j 
  }
  if i > 2 {
    print "Not printing because i is too big\n"
    continue
  }
  print "i=" print i print "\n"
}
