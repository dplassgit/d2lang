a=[true, false]

orit:proc(x:bool, y:bool) {
  print x print " or " print y print " = " println x or y
}
andit:proc(x:bool, y:bool) {
  print x print " and " print y print " = " println x and y
}
xorit:proc(x:bool, y:bool) {
  print x print " xor " print y print " = " println x xor y
}

i=0 while i < 2 do i++ {
  j=0 while j < 2 do j++ {
    andit(a[i], a[j])
  }
}
i=0 while i < 2 do i++ {
  j=0 while j < 2 do j++ {
    orit(a[i], a[j])
  }
}
i=0 while i < 2 do i++ {
  j=0 while j < 2 do j++ {
    xorit(a[i], a[j])
  }
}
