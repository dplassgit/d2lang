// this is in dlib.asm!
// to compile: dcc samples/substring.d --libs=dlib/dlib.obj
substr: extern proc(s: string, start: int, end: int): string

main: proc {
  a="123456"
  b=substr(a, 0, 3)
  println b

  b=substr(a, 2, 4)
  println b
}

main()
