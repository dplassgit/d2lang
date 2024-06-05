main: proc {
  a="123456"
  b=a[0:3]
  println b

  b=a[2:4]
  println b

  b=a[3:length(a)]
  println b
}

main()
