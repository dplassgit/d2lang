a='first' b='second' 

fun:proc(i:int, j:int) {
  e = (a+(b+(a[i]+(a[j]+(b[i]+(b[j]+(a[i:j]+(b[i:j]+(a+(b+(a[i]+(a[j]+(b[i]+(b[j]+(a[i:j]+(b[i:j]))))))))))))))))
  println e
}
fun(0, 2)
