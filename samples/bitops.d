p:proc(pa:int) {
  println pa
  pb=pa&640
  pc=pa|311
  pd=!pa
  pe=pa^456

  println pb
  println pc
  println pd
  println pe
}

p(1234567)
