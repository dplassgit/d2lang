m3:proc(a:int): int { return a*3 }
m4:proc(a:int): int { return a*4 }
m8:proc(a:int): int { return a*8 }
d16:proc(a:int): int { return a/16 }
d168:proc(a:int): int { return a/168 }

println "Should be 15, 24, 20, 6"
println m3(5)
println m4(6)
println m8(10)
println d16(320)
println d168(1008)
