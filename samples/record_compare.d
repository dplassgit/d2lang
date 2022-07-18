rt: record {s:string i:int}

a=new rt
b=new rt
println "Should be false"
println a==b
println "Should be true"
println a!=b
println "Should be true"
println a==a
println "Should be true"
println b==b

c=a
println "Should be true"
println c==a
println "Should be false"
println c==b
println "Should be true"
println c!=b
