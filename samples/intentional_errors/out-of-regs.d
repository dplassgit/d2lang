fun:proc(a:int, b:int, cc:int, dd:int) {
  // this passes
  // c=((a+b)*((a+b)*((a+b)*((a+b)*((a+b)*((a+b)*((a+b)*((a+b)*((a+b)*((a+b)))))))))))
  // This fails
  // c=((a+b)*((a+b)*((a+b)*((a+b)*((a+b)*((a+b)*((a+b)*((a+b)*((a+b)*((a+b)*((a+b))))))))))))
  // This of course fails too
  c=((a+b)*((a+b)*((a+b)*((a+b)*((a+b)*((a+b)*((a+b)*((a+b)*((a+b)*((a+b)*((a+b)*((a+b)*((a+b)*(a+b))))))))))))))
  println c
}
fun(1,2,3,4)

