nestedif:proc() {
  a=3
  if (a==3) {
    if a==2 {
      print a
    }
  } elif a==3 {
    print a
  } else {
    a=4
  }

  if true {
    print 'true'
  }
  if false {
    print 'You should never see this'
    a = a / 0
  }
}

nestedif()
