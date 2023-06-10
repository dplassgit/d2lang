f: proc(s:string) {
 println length(s)
}
print "Should be 2: " f("hi")
print "Should NPE: " f(null)
