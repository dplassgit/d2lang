f:proc(x:string) {
  if x == chr(10) {
    println "chr10"
  } else {
    println "not chr10"
  }
}
f('hi')
f('\n')