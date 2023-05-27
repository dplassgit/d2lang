// "Towers of Hanoi" in D2. Ported from toy (http://www.graysage.com/cg/Compilers/Toy/hanoi.toy)

PEGS = ["", "left", "center", "right"]

printPeg: proc(peg:int) {
  print PEGS[peg]
}

hanoi: proc(n: int, fromPeg: int, usingPeg: int, toPeg: int) {
  if n != 0 {
    hanoi(n - 1, fromPeg, toPeg, usingPeg)
    print "Move disk from "
    printPeg(fromPeg)
    print " peg to "
    printPeg(toPeg)
    println " peg"
    hanoi(n - 1, usingPeg, fromPeg, toPeg)
  }
}

n=5
hanoi(n, 1, 2, 3)
