// Based on outpost.prg
// https://www.commodoregames.net/Outpost_Commodore_PET_437.html
// Original author unknown
// Python version Copyright 2023 David Plass
// D version Copyright 2025 David Plass

sleep: extern proc(secs: int): void
sqrt: extern proc(x: double): double
log: extern proc(d: double): double
lround: extern proc(d: double): int
// returns random number between 0 and 32767
rand: extern proc: int
srand: extern proc(seed: int): void
time: extern proc(ignored: int): int
toupper: extern proc(c:int): int

srand(time(0))

// from dlib:
itod: extern proc(i: int): double
// From dlib:
itos: extern proc(i: int): string

// 20 DEFA(X) = INT(RND(1)*X+1)
randint: proc(x:int): int {
  return rand() % x + 1
}

round: proc(f: double): int {
  return lround(f)
}

isqrt: proc(x: int): double {
  return sqrt(itod(x))
}

min: proc(x: int, y: int): int {
  if x < y { return x} return y
}

max: proc(x: int, y: int): int {
  if x > y { return x} return y
}


Outpost: record {
  // shipType(0..5) = enemy type
  // note: 5 = Supply
  ship_type:int[6]
  // EX, ey =  enemy x, y
  ex:int[6]
  ey:int[6]
  // ED = enemy dist
  distance:int[6]
  // EH = enemy prob
  prob:int[6]
  // EE = enemy energy
  enemy_energy:int[6]

  // E = your energy
  energy: int
  // C = your computer value
  comp: int
  // M = your main
  mains: int
  // S = your sec
  secondary: int
  // T = your torpedo
  torps: int

  // score
  score: int
  // high score
  hs: int
}


// distance to "G" (note it ignores Z
// 30 DEFFNB(Z) = INT(SQR((ex(G)-6)^2+(ey[G]-6)^2))
calc_distance: proc(self: Outpost, G: int): int {
  dx = (self.ex[G] - 6) * (self.ex[G] - 6)
  dy = (self.ey[G] - 6) * (self.ey[G] - 6)
  return round(isqrt(dx + dy))
}


// 40 DEFFNC(Z) = INT(1/(distance[G])*100+(C/2))
// probability of being hit depends on distance and "C" - our computer value, note it ignores Z
calc_prob: proc(self: Outpost, G: int): int {
  // TODO: this might need to be more floaty
  return round(1.0 / itod(self.distance[G]) * 100.0 + (itod(self.comp) / 2.0))
}


// start off with one ship (G = 1); give initial supply
// 50 C = 99:G = 1:GOSUB5110:GOSUB2000
initialize: proc(self: Outpost) {
  self.comp = 99
  resupply(self)
  self.ship_type = [0, 0, 0, 0, 0, 0]
  //ex, ey =  enemy x, y
  self.ex = [0, 0, 0, 0, 0, 0]
  self.ey = [0, 0, 0, 0, 0, 0]
  //ED = enemy dist
  self.distance = [0, 0, 0, 0, 0, 0]
  //EH = enemy prob
  self.prob = [0, 0, 0, 0, 0, 0]
  //EE = enemy energy
  self.enemy_energy = [0, 0, 0, 0, 0, 0]
  self.score = 0
  make_enemy_ship(self, 1)
}


// 200 GOSUB5000:GOSUB1000:GOSUB6000:GOSUB3000:GOSUB4000
// 210 GOTO200
play: proc(self: Outpost) {
  while True {
    initialize(self)
    while True {
      new_ship(self) // 5k
      print_board(self)  // 1k
      get_input(self) // 6k
      if move(self) { break } // 3k
      if enemy_attack(self) { break } // 4k
    }
    if not print_score(self) { break }
  }
}


// 1000 print"␓␑ENEMY 1  2  3  4"
// 1020 print"TYPE ";
// 1030 FORG = 1 TO 4
// 1040 IF ET(G) = 0 THEN print"--- ";
// 1050 IF ET(G) = 1 THEN print"LGT ";
// 1060 IF ET(G) = 2 THEN print"MDM ";
// 1070 IF ET(G) = 3 THEN print"HVY ";
// 1080 NexT
// 1100 print:print"DIST":print"PROB        ":print"ENGY        "
// 1120 FORG = 1 TO 4
// 1122 X = 1+G*4
// 1124 print"‘‘‘‘"
// 1126 PRINT SPC(X) ED(G)
// 1128 PRINT SPC(X) EH(G)
// 1130 PRINT SPC(X) EE(G)
// 1140 NexTG
// 1300 print:printlnSPC(12);"STATUS"
// 1305 PRINT SPC(12)" ÄÄÄÄÄÄ"
// 1310 PRINT SPC(11)" ENGY:  ���";E
// 1320 PRINT SPC(11)" COMP:  ���";C
// 1330 PRINT SPC(11)" MAIN:  ���";M
// 1340 PRINT SPC(11)" SECN:  ���";S
// 1350 PRINT SPC(11)" TORP:";T
// 1360 PRINT SPC(11)"  VP :";VP
// 1400 print:PRINT SPC(11);"  C = CHARGE":print:print
// 1500 print"␓␑␑␑␑␑␑"
// 1510 A = 0
// 1520 FORY = 1 TO 11
// 1530 FORX = 1 TO 11
// 1540 FORG = 1 TO 5
// 1550 IF Y <> ey(G)THEN1620
// 1560 IF X <> ex(G)THEN1620
// 1570 A = 1:IFG = 1THENprint"1";
// 1580 IF G = 2 THEN print"2";
// 1590 IF G = 3 THEN print"3";
// 1600 IF G = 4 THEN print"4";
// 1610 IF G = 5 THEN print"S";
// 1620 NexT
// 1630 IF X = 6 AND Y = 6THENprint"Ñ";:A = 1
// 1640 IF A = 1THENA = 0:GOTO1660
// 1650 print"+";
// 1660 NexTX
// 1670 print
// 1680 NexTY
// 1690 print"␓␑␑␑␑␑␑"

ship_names=["---", "LGT", "MDM", "HVY"]

print_board: proc(self: Outpost) {
  print chr(27) print "[H" // move home
  print chr(27) print "[2J" // clear screen
  println("\nENEMY 1   2   3   4")
  print("TYPE  ")
  G=1 while G < 5 do G++ {
    print ship_names[self.ship_type[G]]
    print " "
  }
  println ""

  print "DIST  "
  G=1 while G < 5 do G++ {
    print self.distance[G]
    if self.distance[G] >= 10 { print "  " }
    else {print "   "}
  }
  println("")

  print "PROB  "
  G=1 while G < 5 do G++ {
    print self.prob[G]
    if self.prob[G] >= 10 { print "  " }
    else {print "   "}
  }
  println("")

  print "ENGY  "
  G=1 while G < 5 do G++ {
    print self.enemy_energy[G]
    if self.enemy_energy[G] >= 10 { print "  " }
    else {print "   "}
  }
  println("\n")

  y=1 while y < 12 do y++ {
    x=1 while x < 12 do x++ {
      if x == 6 and y == 6 {
        print "O"
      } else {
        printed = false
        G=1 while G < 6 do G++ {
          // There may be two ships here, whoopsie
          if y != self.ey[G] { continue }
          if x != self.ex[G] { continue }
          if G != 5 {
            print G
            printed = true
          } else {
            print "S"
            printed = true
          }
        }
        if not printed {print "+" }
      }
    }
    print(" ")
      if y == 1 { println("STATUS") }
    elif y == 2 { println("========") }
    elif y == 3 { print("ENGY: ") println(self.energy) }
    elif y == 4 { print("COMP: ") println(self.comp) }
    elif y == 5 { print("MAIN: ") println(self.mains) }
    elif y == 6 { print("SECN: ") println(self.secondary) }
    elif y == 7 { print("TORP: ") println(self.torps) }
    elif y == 8 { print(" PTS: ") println(self.score) }
    else { println("") }
  }
  println("")
}


// resupply/recharge
// 2000 ET(5) = 0: ex(5) = 0:ey(5) = 0
// 2010 energy = 99: M = 99:secondary = 99
// 2020 torps = torps+5: IF torps > 9 THEN torps = 9
// 2030 RETURN
resupply: proc(self: Outpost) {
  temp = self.ship_type temp[5] = 0
  temp = self.ex temp[5] = 0
  temp = self.ey temp[5] = 0
  self.energy = 99
  self.mains = 99
  self.secondary = 99
  self.torps = min(9, self.torps + 5)
}


// move ships positions, test for supply ship, test for die
// 3000 FORG = 1 TO 5: IF ET(G) > 0 THEN3100
// 3010 NexTG: RETURN
// 3100 IF G < 5 AND FNA(9) > 5 THEN3010
// 3200 IF ex(G) > 6 THEN ex(G) = ex(G)-1
// 3210 IF ex(G) < 6 THEN ex(G) = ex(G)+1
// 3220 IF EY(G) < 6 THEN ey(G) = ey(G)+1
// 3230 IF EY(G) > 6 THEN ey(G) = ey(G)-1
// 3240 IF ET(5) = 5 AND ey(5) = 6 AND ex(5) = 6 THENGOSUB2000
// 3250 IF EY(G) = 6 AND ex(G) = 6THEN9500
// 3265 IF G < 5 AND ex(G) = ex(5) AND ey(G) = ey(5) THEN ET(5) = 0: ex(5) = 0:ey(5) = 0
// 3300 ED(G) = FNB(1)
// 3330 EH(G) = FNC(0): IF EH(G) > 99 THEN EH(G) = 99
// 3400 GOTO 3010
move: proc(self: Outpost): bool {
  G=1 while G < 6 do G++ {
    if self.ship_type[G] > 0 {
      if G < 5 and randint(9) > 5 { continue }
      // move towards 6, 6
      if self.ex[G] > 6 {
        temp = self.ex temp[G]  = temp[G] - 1
      }
      if self.ey[G] > 6 {
        temp = self.ey temp[G] = temp[G] - 1
      }
      if self.ex[G] < 6 {
        temp = self.ex temp[G] = temp[G] + 1
      }
      if self.ey[G] < 6 {
        temp = self.ey temp[G] = temp[G] + 1
      }
      if self.ship_type[G] == 5 and self.ey[5] == 6 and self.ex[5] == 6 {
        resupply(self) // the supply ship got here
      } else {
        if self.ey[G] == 6 and self.ex[G] == 6 { return True } // die, they got me
      }
      // enemy killed supply ship
      if G < 5 and self.ex[G] == self.ex[5] and self.ey[G] == self.ey[5] {
        temp = self.ship_type temp[5] = 0
        temp = self.ex temp[5] = 0
        temp = self.ey temp[5] = 0
      }
      temp = self.distance temp[G] = calc_distance(self, G) // recompute distance
      temp = self.prob temp[G] = min(99, calc_prob(self, G)) // probability
    }
  }
  return False
}


// enemy attack (moving is really in 3k)
// 4000 println"‘ENEMY FIRING & MOVING"
// 4010 FOR G = 1 TO 4: IF ET(G) <> 0 THEN 4100
// 4020 NexT G: RETURN
// 4100 IF FNA(99) > (EE(G)+FNA(30)) OR EE(G) < 10 THEN 4020
// 4110 E = E-FNA(5)*ET(G)
// 4150 EE(G) = EE(G)-FNA(10)
// 4160 IF FNA(10) = 1 THEN C = C-FNA(25): IF C < 1 THEN 9500
// 4170 IF FNA(10) = 1 THEN M = M-FNA(25): IF M < 0 THEN M = 0
// 4180 IF FNA(10) = 1 THEN S = S-FNA(25): IF S < 0 THEN S = 0
// 4200 IF E < 0 THEN 9500
// 4210 GOTO 4020
enemy_attack: proc(self: Outpost): bool {
  println("ENEMY FIRING & MOVING")
  G=1 while G < 5 do G++ {
    if self.ship_type[G] != 0 {
      if randint(99) > (self.enemy_energy[G] + randint(30)) or self.enemy_energy[G] < 10 { continue }
      self.energy = self.energy - randint(5) * self.ship_type[G]  // decrease our energy by randomness * this enemy"s energy
      temp = self.enemy_energy
      temp[G] = temp[G] - randint(10)  // decrease enemy"s energy by random
      if randint(10) == 1 {
        self.comp = self.comp - randint(25)
        if self.comp < 1 { return True }  // decrease comp - might die
      }
      if randint(10) == 1 {
        self.mains = max(0, self.mains - randint(25)) // decrease mains
      }
      if randint(10) == 1 {
        self.secondary = max(0, self.secondary - randint(25)) // decrease secondary
      }
      if self.energy < 0 { return True }   // died, our energy too low
    }
  }
  return False
}


// make ship appear
// 5000 G = FNA(5)
// 5005 IF G = 5 AND ET(5) = 0 AND FNA(4) > 1 THEN ET(5) = 5: GOTO5160
// 5010 IF G = 5 OR ET(G) <> 0 OR FNA(9) > 4 THEN 5400
new_ship: proc(self: Outpost) {
  i = randint(5)
  if i == 5 and self.ship_type[i] == 0 and randint(4) > 1 {
    // add a supply ship
    temp = self.ship_type temp[5] = 5
    make_any_ship(self, i)
  } else {
    if i == 5 or self.ship_type[i] != 0 or randint(9) > 4 {
      // no ship
      return
    } else {
      make_enemy_ship(self, i)
    }
  }
}


// 5110 A = 4-INT(LOG(FNA(50)+2))
// 5120 ET(G) = A: EE(G) = 99
make_enemy_ship: proc(self: Outpost, i: int) {
  ship_type = 5 - round(log(itod(randint(50) + 2)))
  if ship_type > 3 { ship_type = 3 }
  temp = self.ship_type temp[i] = ship_type
  temp = self.enemy_energy temp[i] = 99
  make_any_ship(self, i)
}


// 5160 ex(G) = FNA(11)
// 5170 ey(G) = FNA(11)
// 5180 A = FNA(4): IF A = 1THEN ey(G) = 1
// 5190 IF A = 2 THEN ey(G) = 11
// 5200 IF A = 3 THEN ex(G) = 11
// 5210 IF A = 4 THEN ex(G) = 1
// 5300 ED(G) = FNB(1)
// 5320 EH(G) = FNC(0): IF EH(G) > 99 THEN EH(G) = 99
// 5400 RETURN
make_any_ship: proc(self: Outpost, i: int) {
  temp = self.ex temp[i] = randint(11)
  temp = self.ey temp[i] = randint(11)
  // pick starting quadrant
  q = randint(4)
  if q == 1   { temp = self.ey temp[i] = 1 }
  elif q == 2 { temp = self.ey temp[i] = 11 }
  elif q == 3 { temp = self.ex temp[i] = 1 }
  elif q == 4 { temp = self.ex temp[i] = 11 }
  // calculate distance
  temp = self.distance temp[i] = calc_distance(self, i)
  // calculate probability
  temp = self.prob temp[i] = min(calc_prob(self, i), 99)
}


isdigit: proc(s: int): bool {
  return s >= asc("0") and s <= asc("9")
}

// get input and fire weapons
// 6000 print "WEAPON:           "
// 6010 GET A$: IF A$ = "" THEN 6010
// 6020 IF A$ = "M" AND M > 0 THEN A = 6: M = M-FNA(5): IF M < 0 THEN M = 0
// 6025 IF A$ = "C" THEN E = E+FNA(20) : IF E > 99 THEN E = 99
// 6030 IF A$ = "C" THEN RETURN
// 6035 IF A$ = "S" AND S > 0 THEN A = 4: S = S-FNA(5): IF S < 0 THEN S = 0
// 6040 IF A$ = "T" AND T > 0 THEN A = 9: T = T-1
// 6060 IF A < 3THENprintn"‘BAD INPUT! WEAPON: ": GOTO6010
// 6100 print"‘TARGET NO:       "
// 6120 GET B$: IFB$ = ""THEN6120
// 6125 B = VAL(B$)
// 6130 IF ET(B) = 0 THEN print "‘BAD DATA! TARGET: ": GOTO6120
// 6200 IF FNA(99) > EH(B) THEN print "‘MISSED!      ": FORZ = 1 TO 1000: NexT:RETURN
// 6210 EE(B) = INT(EE(B)-((A*FNA(15))/ET(B)))
// 6215 print"‘TARGET HIT!      ": FORZ = 1TO1000: NexT
// 6220 IF EE(B) < 1THEN6500
// 6230 E = E-FNA(5)
// 6300 RETURN
// 6500 VP = VP+ET(B)
// 6505 ex(B) = 0: ey(B) = 0
// 6510 ET(B) = 0: EH(B) = 0: ED(B) = 0:EE(B) = 0
// 6570 print"‘␒  TARGET DESTROYED!  "
// 6575 FOR A = 1TO 1000: NexTA
// 6580 RETURN
get_input: proc(self: Outpost) {
  damage = 0
  while True {
    damage = 0
    print("WEAPON (MSTC): ")
    weapon = input
    if length(weapon) == 0 {
      println("BAD INPUT!")
      continue
    }
    weapon = chr(toupper(asc(weapon[0])))
    // charge - increase energy
    if weapon == "C" {
      self.energy = min(99, self.energy + randint(20))
      return
    }
    // shoot mains at one of the ships if we can
    if weapon == "M" and self.mains > 0 {
      damage = 6
      self.mains = max(self.mains - randint(5), 0)
    }
    // shoot at secondaries at one of the ships if we can
    if weapon == "S" and self.secondary > 0 {
      damage = 4
      self.secondary = max(self.secondary - randint(5), 0)
    }
    // fire torps if we can
    if weapon == "T" and self.torps > 0 {
      damage = 9
      self.torps = self.torps - 1
    }
    if damage < 3 {
      println("BAD INPUT!")
      continue
    }
    break
  }
  target = -1
  while True {
    print("TARGET NO: ")
    b = input
    if length(b) == 0 {
      println("BAD DATA!")
      continue
    }
    if not isdigit(asc(b)) {
      println("BAD DATA!")
      continue
    }
    target = asc(b) - asc("0")
    if self.ship_type[target] == 0 {
      println("BAD DATA!")
      continue
    }
    break
  }
  if randint(99) > self.prob[target] {
    println("\n*******")
    println("MISSED!")
    println("*******")
    sleep(1)
    return
  }
  // A is 6 for main, 4 for secondary, 9 for torps
  temp = self.enemy_energy 
  temp[target] = self.enemy_energy[target] - round(itod(damage * randint(15)) / itod(self.ship_type[target]))
  println("\n***********")
  println("TARGET HIT!")
  println("***********")
  sleep(1)
  if self.enemy_energy[target] < 1 {
    println("\n*********************")
    println("  TARGET DESTROYED!  ")
    println("*********************")
    // increase score based on type of ship
    self.score = self.score + self.ship_type[target]
    //  erase ship
    temp = self.ex temp[target] = 0
    temp = self.ey temp[target] = 0
    temp = self.ship_type temp[target] = 0
    temp = self.prob temp[target] = 0
    temp = self.distance temp[target] = 0
    temp = self.enemy_energy temp[target] = 0
    sleep(1)
  } else {
    // decrease our energy if we didn"t destroy him
    self.energy = self.energy - randint(5)
  }
  return
}


// 9500 POKE36879, 110
// 9510 print "    DESTROYED!!!!!"
// 9550 print "  SCORE =  ";score: print: print
// 9560 IF score > HS THEN HS = score
// 9580 print "*****************"
// 9590 print  "HIGH SCORE =  ";HS
// 9600 print  "*****************"
// 9605 print "  ANOTHER GAME?"
// 9610 GETA$: IF A$ = "" THEN 9610
// 9620 IF A$ = "Y" THEN  RUN
// 9630 STOP
print_score: proc(self: Outpost): bool {
  println("**********************")
  println("YOU ARE DESTROYED!!!!!")
  println("**********************")
  print("\nSCORE =  ") println( self.score)
  self.hs = max(self.hs, self.score)
  print("HIGH SCORE =  ") println(self.hs)
  println("\nAnother game?")
  another = input
  if length(another) == 0 { return false }
  return another[0] == "Y" or another[0] == "y"
}

outp = new Outpost
play(outp)

