f:proc:int {
    return 3
    println 'Hello world'
}

f2:proc:int {
  if true {
    return 4
    println 'Hello world'
  }
  return 5
}

println "Should print 3:"
println f()
println "Should print 4:"
println f2()
