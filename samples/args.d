len = length(args)
print "length is " println len

i = 0 while i < len do i = i + 1 {
  a=args[i]
  print "global arg " print i println " is " + a
}

printargs: proc {
  i = 0 while i < len do i = i + 1 {
    a=args[i]
    print "inside proc, arg " print i println " is " + a
  }
}

printargs()

