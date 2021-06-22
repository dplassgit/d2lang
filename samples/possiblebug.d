// Integers
p:proc() {
  a:int
  a=3
  a=-3
  a=--3
  a=-+-3
  a=+3+-3
  a=+3
  b=a // 3
  a=(3+a)*-b // (3+3)*-3 = 6*-3=-18, ruh roh.
  b=+a
  b=-a

  println a
  println 3+a*-b // 3+(-18*18)
  println (3+a)*-b
  println 4%6
}

main {
  p()
}
