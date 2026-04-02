glob = 3

retvoid: proc {
  glob=4
}

anotherproc: proc(i:int) {
  return retvoid()
}

anotherproc(glob)

