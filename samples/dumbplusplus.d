f:proc(a:int): int {
    a=0
    a++
    return a
}

g:proc(b:int): int {
    b=b+1
    return b
}

println f(2)

println g(3)
