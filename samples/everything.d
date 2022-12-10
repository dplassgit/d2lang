// Integers
p:proc() {
  a:int
  a=3
  a=-3
  a=-+-3
  a=+3+-3
  a=+3
  b=a // 3
  a=(3+a)*-b // (3+3)*-3 = 6*-3=-18
  b=+a
  b=-a

  // Comparisons. These will all be optimized out
  e=a==b
  e=a!=b
  e=a>3
  e=a<3
  e=a<=3
  e=a>=3
  e=(a>=3) or not (b<3)

  // Print
  println a
  println 3+a*-b // 3+(-18*18)
  println (3+a)*-b
  println 4%6

  // If, elif, else, nested.
  if a==3 {
    if a==2 {
      println a
    }
    println a
  } elif a==4 {
    println a+1
  } elif a==5 {
    println a*2
  } else {
    println 6
  }
}

// Boolean constants
c=true
c=not true
c=not not true
d=c
d=not c or false
d=not not c and c

x:string
x="hi"
x='hi'
z=""
z=" "
x=x+z
x=x+' there'
y=x
println ""
println "y = " + y
println "first letter of x = " + x[0]

main {
  f:bool
  p()
}
