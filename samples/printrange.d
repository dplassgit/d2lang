a=1:2 
println "Should be 1:2"
print a[0] print ':' println a[1]

f:proc(x:range) {
  println "Should be 2:3"
  j=0
  print x[j] print ':' j++ println x[j]
}

f(2:3)
