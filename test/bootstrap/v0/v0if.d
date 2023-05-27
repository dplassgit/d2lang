if true {
  print "Should print this\n"
}
if false {
  print "Should not print this\n"
  exit
}

if 1<2 {
  print "Should print this because 1<2\n"
}

a=3
if 4>a {
  print "Should print this because 4>a\n"
}

if a!=a {
  print "Should not print this \n"
  exit
} else {
  print "Should print this because a=!a is false\n"
}


if a==1 {
  print "Should not print this 1\n"
  exit
} elif a==2 {
  print "Should not print this 2\n"
  exit
} elif 3==a {
  print "Should print this because 3==a\n"
} else {
  print "Should not print this 3\n"
  exit
}

