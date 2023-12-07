btoi: extern proc(b:byte): int
btos: extern proc(b:byte): string
b=0yf4
print "should be 0yf4: " println btos(b)
i=btoi(b)
print "should be -12: " println i

itos: extern proc(i: int): string
i=-123456789
print "should be -123456789: " println itos(i)

ltod: extern proc(el: long): double
ltos: extern proc(el: long): string
el=123456789012L
d=ltod(el)
print "should be 123456789012L " println d
print "should be 123456789012 " println ltos(el)


main:proc {
  // test locals
  itod: extern proc(i: int): double
  i=12345
  d2=itod(i)
  print "should be 12345: " println d2
}

main()
