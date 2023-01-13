i=100

p:proc() {
  j:int // local
  i = i * 3
  j=i + 10
  println j
}

println "Should be 100*3+10=310:"
p()
