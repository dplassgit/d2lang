a=[23, 971]

orit:proc(x:int, y:int) {
  print x print " | " print y print " = " println x | y
}
andit:proc(x:int, y:int) {
  print x print " & " print y print " = " println x & y
}
xorit:proc(x:int, y:int) {
  print x print " ^ " print y print " = " println x ^ y
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
