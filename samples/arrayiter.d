
array:int[4]
i=0 while i < 4 do i = i + 1 {
  array[i] = (i+1)*2
}

println "Should print 2,4,6,8"
i=0 while i < length(array) do i = i + 1 {
  print array[i]
  if (i != 3) {
    print ","
  }

}
println ""

println "Should be c:"
println "abcde"[2]

