f:proc(a:double, b:double):double {
    b = -a
    print "Should be -1: " println b
    a=a + 2.0
    x=-a
    a=-a
    print "Should be -3: " println a
    return x
}

println f(1.0, 0.0)

