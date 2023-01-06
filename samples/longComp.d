f:proc(a:long, b:long) {
  print a
  print " >? " 
  println b

  if a > b {
    println "Yes"
  } else {
    println "No"
  }
}

al=123L
bl=21474836478900L
f(al, bl)
