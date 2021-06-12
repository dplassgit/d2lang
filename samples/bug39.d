a:string

p: proc() {
  println "Should print 'hi':"
  print a
}

setup: proc() {
 a = "hi"
}

setup()
p()
