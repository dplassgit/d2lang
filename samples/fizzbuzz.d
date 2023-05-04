
fizzbuzz: proc() {
  i=1 while i < 100 do i = i + 1 {
    if (i%15) == 0 {
      print "Fizz Buzz, \n"
    } elif (i%3) == 0 {
      print "Fizz, \n"
    } elif (i%5) == 0 {
      print "Buzz, \n"
    } else {
      print i
      print ", \n"
    }
  }
}

fizzbuzz()
