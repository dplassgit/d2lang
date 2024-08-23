b:int[]
f:proc(a:int[]) {
  b=a
  println b[0]
}

println "Should be 1:"
f([1,2,3])
