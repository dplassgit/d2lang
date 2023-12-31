f:proc(a:int, b:bool, c:double, d:byte, e:int, f:bool, g:double, h:byte): string {
    a=a + e
    println a
    b=b xor f
    println b
    c=c / g
    println c
    local=93.0
    local=local / c
    println local
    g=g / c
    println g
    h=h * d
    println h
    if b {
      return "true"
    }
    return "false"
}

println f(1, true, 2.0, 0y03, 4, false, 5.0, 0y06)
println f(6, true, 5.0, 0y04, 3, true, 2.0, 0y01)
