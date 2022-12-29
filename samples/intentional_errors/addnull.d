a="hi"
b=a+null
b=null+a

f:proc {
  c=null
  d="hi"+c
  d=c+"hi"
  e="hi"+null
  println d
}
f()
