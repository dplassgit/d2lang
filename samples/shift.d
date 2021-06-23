
m1:proc(a:int): int { return a>>3 }
m2:proc(a:int): int { return a<<4 }

println "Should be 2, 32, 1, 256"
println m1(16)
println m2(2)
println m1(1<<3)
println m2(256>>4)
