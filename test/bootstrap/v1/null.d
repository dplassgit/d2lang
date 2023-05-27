rt: record {f:int}
r=new rt
if r == null {
  println "This should never happen"
}
if r != null {
  println "Should print this."
}

rt2: record {b:bool}
r2=new rt2
if null == r2 {
  println "This should never happen"
}
if null != r2 {
  println "Should print this."
}
println "Should have printed 'Should print this.' twice"
