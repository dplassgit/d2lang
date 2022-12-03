f:proc(n:int) {
    println n
    a = 3 < n
    println a
    b = 3 == n
    println b
    c = 3 >= n
    println c

    a = n < 3
    println a
    b = n == 3
    println b
    c = n <= 3
    println c
}

f(1)
f(3)
f(5)
