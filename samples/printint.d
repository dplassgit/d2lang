pl:proc(i:int) { println i}
p:proc(i:int) { print i}
pc:proc { print 3 println 4}

a=10
println "Print constant 10 then 11 then newline"
print a
println a+1
println "Constant 34 with a newline"
pc()
println "Print 6 without a newline"
p(6)
println ", then print 5 with a newline"
pl(5)
println "Last line"
