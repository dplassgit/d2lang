kw=['a', 'b']


if mycmp('a', kw[0]) == 0 {
  println "Should print this"
} else {
  println "Should not print this"
  exit
}

mycmp:proc(s1:string, s2:string):int {
  if s1 == s2 { return 0 }
  return -1
}
