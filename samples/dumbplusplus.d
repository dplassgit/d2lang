f:proc(a:int): int {
    a++
    a++
    a++
    return a
}

g:proc(a:int): int {
    b=a
    b=b+4
    b++
    return b
}

println f(2)

println g(3)
