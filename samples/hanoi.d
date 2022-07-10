// hanoi.d - standard "Towers of Hanoi" in D(2). Ported from toy (http://www.graysage.com/cg/Compilers/Toy/hanoi.toy)

PEGS = ["", "left", "center", "right"]

printIt: proc(index:int, arr:string[]) {
  print arr[index]
}

printPeg: proc(peg:int) {
  printIt(peg, PEGS)
}

hanoi: proc(n: int, fromPeg: int, usingPeg: int, toPeg: int) {
  if n != 0 {
    hanoi(n - 1, fromPeg, toPeg, usingPeg)
    print("Move disk from ")
    printPeg(fromPeg)
    print(" peg to ")
    printPeg(toPeg)
    print(" peg.\n")
    hanoi(n - 1, usingPeg, fromPeg, toPeg)
  }
}

main {
  n=5
  hanoi(n, 1, 2, 3)
}
