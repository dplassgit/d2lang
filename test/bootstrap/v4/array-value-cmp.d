kw=['a', 'b']

stricmp: extern proc(s1:string, s2:string):int

if stricmp('A', kw[0]) == 0 {
  println "Should print this"
} else {
  println "Should not print this"
  exit
}
