
fizzbuzz: proc() {
  i=1 while i < 100 do i = i + 1 {
    if (i%15) == 0 {
      println "Fizz Buzz, "
    } elif (i%3) == 0 {
      println "Fizz, "
    } elif (i%5) == 0 {
      println "Buzz, "
    } else {
      print i
      println ', '
    }
  }
}

fizzbuzz()
