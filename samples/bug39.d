a:string

p: proc() {
  print a
}

setup: proc() {
 a = "hi"
}

setup()
p()
