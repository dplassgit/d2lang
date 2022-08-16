/////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////
// GALACTIC EMPIRED
/////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////


/////////////////////////////////////////////////////////
// ENUMERATIONS
/////////////////////////////////////////////////////////

// These are both for assets and production ratios
FOOD=0 FUEL=1 PARTS=2 TROOPS=3 MONEY=4

ASSET_TYPE=["Food", "Fuel", "Parts", "Troops", "Money"]

INDEPENDENT=0 OCCUPIED=1 EMPIRE=2
STATUSES=["Independent", "Occupied", "Empire"]

PRIMITIVE=0 LIMITED=1 ADVANCED=2 SUPERIOR=3
CIV_LEVELS=["Primitive", "Limited", "Advanced", "Superior"]

// Game status
IN_PROGRESS=0 WON=1 LOST=-1 QUIT=-2


/////////////////////////////////////////////////////////
// CONSTANTS
/////////////////////////////////////////////////////////
NAMES=[
  "Galactica",
  "Alhambra",
  "Bok",
  "Drassa2",
  "Eventide",
  "Farside",
  "Harkon",
  "Javiny",
  "Kgolta",
  "Llythll",
  "Moonsweep",
  "Novena",
  "Ootsi",
  "Proyc",
  "Sparta",
  "Twyrx",
  "Utopia",
  "Viejo",
  "Yang-tzu",
  "Zoe"
]
// given a letter from a-z, return the index into the planet array.
IDS=[
  1, // "Alhambra",
  2, // "Bok",
  -1, // c
  3, // "Drassa2",
  4, // "Eventide",
  5, // "Farside",
  0, // "Galactica",
  6, // "Harkon",
  -1, // i
  7, // "Javiny",
  8, // "Kgolta",
  9, // "Llythll",
  10, // "Moonsweep",
  11, // "Novena",
  12, // "Ootsi",
  13, // "Proyc",
  -1, // q
  -1, // r
  14, // "Sparta",
  15, // "Twyrx",
  16, // "Utopia",
  17, // "Viejo",
  -1, // w
  -1, // x
  18, // "Yang-tzu",
  19  // "Zoe"
]
NUM_PLANETS=20
SIZE=50


/////////////////////////////////////////////////////////
// GLOBALS
/////////////////////////////////////////////////////////

planets: PlanetType[NUM_PLANETS]
gameinfo: GameInfoType
fleet: FleetType
galmap:int[SIZE*SIZE]    // 0=no planet, else the ascii of the planet


/////////////////////////////////////////////////////////
// TYPES
/////////////////////////////////////////////////////////

PlanetType: record {
  id:int
  x:int y:int    // location (0-50)
  civ_level:int    // primitive, limited, advanced, etc.
  status:int     // 1=occupied, 2=empire, 0=independent
  status_changed:bool  // 1 if status just changed, 0 if not. WHY?!
  name:string
  abbrev:string    // first letter of planet
  population:double   // in millions
  assets:double[5]  // amount of each type on hand: food, fuel, parts, troops, money
  prod_ratio:int[5]   // ratio of each type of asset production
  prices:int[2]    // food, fuel (note can only buy if status=empire)
  sats_arrive:int[3]  // arrival date (in DAYS) of each satellite
  sats_orbit:int    // # of satellites in orbit
  sats_enroute:int  // # of satellites en route
  troops:int    // # of troops on surface, or # of occupation troops
  fighters:int     // # of fighters in orbit, or # of occupation fighters
  occupied_on:int    // date (years) planet was occupied
}

FleetType: record {
  location:PlanetType  // pointer to planet struct in global array
  assets:double[5]    // amount of each type on hand: food, fuel, parts, troops, money
  carriers:int[2]   // # of food, fuel carriers; they carry 1000 units each
  etrans:int    // empty transports (full transports = assets[TROOPS])
  fighters:int     // # of fighters
  satellites:int   // in inventory
}

GameInfoType: record {
  date:int  // days since start. 100 days per "year"
  level:int  // difficulty level
  status:int   // in progress, lost, won

  num_empire:int
  num_independent:int
  num_occupied:int
}


/////////////////////////////////////////////////////////
// INITIALIZERS
/////////////////////////////////////////////////////////

initPlanets:proc {
  i = 0 while i < NUM_PLANETS do i = i + 1 {
    p = new PlanetType
    planets[i] = p

//    name =
    p.name = NAMES[i]
    p.abbrev = p.name[0]

    // 1. assign location randomly on grid
    p.x = random(SIZE)
    p.y = random(SIZE)

    // set galmap[x,y] to planet name letter
    galmap[p.x+SIZE*p.y] = asc(p.abbrev)

    // TODO: Randomize all of these +/- level*2 %
    if (i != 0) {
      // 2. assign population based on level
      // the higher the level the more pop
      // min is 16, max is 100 to start
      p.population = 16.0 + tod(random(840))/10.0 + 3.0 * tod(random(10*gameinfo.level))/10.0

      // 3. assign civ level: 0-3
      // TODO: the higher the level the more superior & advanced planets
      p.civ_level = random(4)

      // 4. set status as independent
      p.status = INDEPENDENT

      prod_ratio = p.prod_ratio
      if p.civ_level == PRIMITIVE {
        // if primitive, base is 33 (min 16)
        prod_ratio[FOOD] = 34
        prod_ratio[MONEY] = 33
        prod_ratio[TROOPS] = 33
      } elif p.civ_level == LIMITED {
        // if limited base is 25 (min 12)
        prod_ratio[FOOD] = 25
        prod_ratio[MONEY] = 25
        prod_ratio[TROOPS] = 25
        prod_ratio[FUEL] = 25
      } else { // advanced, superior
        // else base is 20 (min 10)
        prod_ratio[FOOD] = 20
        prod_ratio[MONEY] = 20
        prod_ratio[PARTS] = 20
        prod_ratio[TROOPS] = 20
        prod_ratio[FUEL] = 20

        p.fighters = (random(400)+10)*(p.civ_level+1)/10 + 2*random(10*gameinfo.level)/10
      }

      // 7. set troops, fighters based on level & civ_type (no fighters if <ADVANCED)
      p.troops = (random(400)+10)*(p.civ_level+1)/10 + 2*random(10*gameinfo.level)/10

    } else {
      // Galactica
      // 2. assign population based on level/ the higher the level the lower the pop
      p.population = 35.0+10.0*(10.0-tod(gameinfo.level*10)/10.0)+tod(random(50))/10.0

      // 3. set civ level to advanced
      p.civ_level = ADVANCED

      // 4. set status = empire
      p.status = EMPIRE

      // 5. set all percentages=20
      prod_ratio = p.prod_ratio
      prod_ratio[FOOD] = 20
      prod_ratio[MONEY] = 20
      prod_ratio[PARTS] = 20
      prod_ratio[TROOPS] = 20
      prod_ratio[FUEL] = 20

      // 6. set satellites=3; this way the 'display planet'
      // code doesn't have to special-case galactica!
      p.sats_orbit = 3
      // fun fact, galactica starts with 0 troops and 0 fighters (!)
    }

    // 8. set food price, fuel price based on level
    // TODO: the higher the level the higher the price (until the price goes down (!))
    // plus random factor
    prices = p.prices
    prices[FOOD] = 7
    prices[FUEL] = 5

    // 9. set initial assets based on level
    // TODO: RANDOMIZE
    // food 3.3973x2-75.85x+432.41
    // money 27.268x2-635.19x+3799
    // parts 4.4802x2-97.611x+541.88
    // men 1.2727x2 - 26.198x + 143.58
    // fuel 1.7437x2 - 40.969x + 247.21

    // food -53.7*level+405.5
    // food 3.3973x2-75.85x+432.41
    assets = p.assets
    leveld = tod(gameinfo.level)
    assets[FOOD] = abc(3.3973, -75.85, 432.41, leveld)

    // money -457.4*level+3583
    // money 27.268x2-635.19x+3799
    assets[MONEY] = abc(27.268, -635.19, 3799.0, leveld)

    // if < advanced, no parts
    if (p.civ_level >= ADVANCED) {
      // planet->assets[PARTS] = ax_b(level, -68, 506)
      // parts 4.4802x2-97.611x+541.88
      assets[PARTS] = abc(4.4802, -97.611, 541.88, leveld)
    }

    // TROOPS -17.9x + 133.5
    // troops 1.2727x2 - 26.198x + 143.58
    assets[TROOPS] = abc(1.2727, -26.198, 143.58, leveld)

    // if primitive, no fuel
    if (p.civ_level > PRIMITIVE) {
      // fuel -29.6x + 233.4
      // fuel 1.7437x2 - 40.969x + 247.21
      assets[FUEL] = abc(1.7437, -40.969, 247.21, leveld)
    }
  }
}

initFleet:proc {
  fleet = new FleetType
  fleet.location = planets[0]

  // TODO: RANDOMIZE

  // initialize based on level
  leveld = tod(gameinfo.level)
  assets = fleet.assets
  // money = -150*level+2650;
  assets[MONEY] = abc(0.0, -150.0, 2650.0, leveld)

  // troops=10*(11-level)=110-10*level;
  assets[TROOPS] = abc(0.0, -10.0, 110.0, leveld)

  // freighters=(-3*level+43)/8;
  carriers = fleet.carriers
  carriers[FOOD]=(43-3*gameinfo.level)/8

  // fuel_ships=(-6*level+77)/16;
  carriers[FUEL]=(77-6*gameinfo.level)/16

  // food=freighters*1000-7000*(level-1)/256;
  foods_amt = carriers[FOOD]*1000-(7000*(gameinfo.level-1))/256 // a find foods amount
  assets[FOOD] = tod(foods_amt)

  // fuel=fuel_ships*1000-7000*(level-1)/256;
  fuel_amt = carriers[FUEL]*1000-(7000*(gameinfo.level-1))/256
  assets[FUEL] = tod(fuel_amt)

  // Empty transports. "Filled" transports = assets[TROOPS]
  fleet.etrans = ax_b(gameinfo.level, -10, 110)

  fleet.fighters = fleet.etrans + 100
  fleet.satellites = 11 - gameinfo.level/2
}


/////////////////////////////////////////////////////////
// MATHS
/////////////////////////////////////////////////////////

DS=[0.0,1.0,2.0,3.0,4.0,5.0,6.0,7.0,8.0,9.0]

// int to double.
tod: proc(i:int):double {
  neg = false
  if i < 0 {neg = true i = -i}
  d=0.0
  while i > 0 {
    last = i%10
    // I hate this, lol
    d = d * 10.0 + DS[last]
    i = i / 10
  }
  if neg {d = -d}
  return d
}

toi: proc(d:double):double {
  neg = false
  if i < 0 {neg = true i = -i}
  d=0.0
  while i > 0 {
    last = i%10
    // I hate this, lol
    d = d * 10.0 + DS[last]
    i = i / 10
  }
  if neg {d = -d}
  return d
}

// a*x+b
ax_b:proc(x:int, a:int, b:int):int { return a*x+b }

// a*x*x+b*x+c
abc:proc(a:double, b:double, c:double, x:double):double {
  return a*x*x+b*x+c
}

min:proc(a:int, b:int):int { if a < b { return a } return b }
max:proc(a:int,b:int):int { if a > b { return a } return b }

abs:proc(x:double):double {
  if x < 0.0 { return -x}
  return x
}

sqrt:proc(d:double):double {
  guess = d/4.0
  x100 = 1.0/100000.0
  iters=0
  while (abs(guess*guess-d) > (x100)) {
    xguess = d/guess
    guess = (guess + xguess)/2.0
    iters = iters + 1
    if iters > 100 {
      break
    }
  }

  return guess
}

calc_distance:proc(p1:PlanetType, p2:PlanetType): double {
  xd = tod(p1.x-p2.x)
  yd = tod(p1.y-p2.y)
  dist = xd*xd+yd*yd
  return sqrt(dist)
}


RANDOM_MOD=65535

// Get a random number from 0 to 'range' exclusive
random:proc(range:int):int {
  return (next_random() * range)/RANDOM_MOD
}

last_rand:int
// returns a random number from 0 to RANDOM_MOD (exclusive)
next_random:proc:int {
  // m=modulus, a=multiplier, c=increment
  a=75 c=74
  last_rand = ((last_rand * a) + c) % RANDOM_MOD
  return last_rand
}


/////////////////////////////////////////////////////////
// UTILITIES
/////////////////////////////////////////////////////////

calculate_game_status:proc:int {
  if gameinfo.status != IN_PROGRESS {return gameinfo.status}
  if gameinfo.date > (4100*100) {
    return LOST
  } else {
    // todo: when all are empire: WON
    return IN_PROGRESS
  }
}

toUpper:proc(ss:string):string{
  s = asc(ss)
  if s >= asc('a') and s <= asc('z') { return chr(s - (asc('a') - asc('A'))) }
  return ss[0]
}

trim:proc(s:string):string {
  r = ''
  i = 0 while i < min(length(s), 3) do i = i + 1 {
    c = s[i]
    if c != '\n' { r = r + toUpper(c) }
  }
  return r
}

toString: proc(i: int): string {
  if i == 0 {
    return '0'
  }
  val = ''
  while i > 0 do i = i / 10 {
    val = chr((i % 10) +asc('0')) + val
  }
  return val
}

format_date:proc(d:int):string {
  year = d/100
  days = d%100
  result = toString(year) + "."
  if days < 10 {result = result + "0"}
  result = result + toString(days)
  return result
}


/////////////////////////////////////////////////////////
// COMMANDS
/////////////////////////////////////////////////////////

sleep:proc(days:int) {
  gameinfo.date = gameinfo.date + days
  // TODO: in sleep if a planet changes status, update the map
}

cheat:proc() {
  i = 0 while i < NUM_PLANETS do i = i + 1 {
    p = planets[i]
    println "----------------------------------------------------------"
    print "Planet[" print i print "]: "
    showPlanet(p, true)
  }
}

showPlanet:proc(p:PlanetType, cheat:bool) {
  print "PLANET INFO:  "
  sats = p.sats_orbit
  if cheat or fleet.location == p {
    // if the fleet is here, it's as if we've sent 3 sats
    sats = 3
  }
  println p.name
  if (cheat) {
    print " (" print p.x print ", " print p.y print ")"
  }
  print "Status: " println STATUSES[p.status]

  // 2. if satellites > 0 draw civ level;
  // 3. If satellites > 1 draw population;
  // 4. b. if status=empire, make assets[ visible, troops, fighters & sats invisible;
  //    c.
  //       i. if status=occupied or indep, make assets[ invisible, fighters troops sats status visible;
  //       ii. if satellites > 2 draw troops, fighters, sats;

  if sats > 0 {
    print "Civ_level: " print CIV_LEVELS[p.civ_level]
  }
  if sats > 1 {
    print " population: " print p.population
  }
  if cheat or p.status == EMPIRE {
    print " assets:     " println p.assets
  }
  if cheat or (p.status != EMPIRE and sats > 2) {
    print " troops:     " print p.troops print " fighters: " print p.fighters
  }
  if cheat {
    print " prices:     " println p.prices
  }
  if cheat or p.status != EMPIRE {
    print " sats:       " print p.sats_orbit print " (" print p.sats_enroute println " enroute)"
  }

  distance = calc_distance(fleet.location, p)
  print "Distance:       " println distance
  print "Estimated food: " println calc_food_needed(distance)
  print "Estimated fuel: " println calc_fuel_needed(distance)
}

calc_fuel_needed: proc(dist:double): double {
  food = fleet.assets[TROOPS] * 5.0 + tod(fleet.fighters)
  return food*dist/10.0
}

calc_food_needed:proc(dist:double): double {
  fuel=fleet.assets[TROOPS] + tod(fleet.etrans + fleet.fighters +
           fleet.carriers[FUEL] + fleet.carriers[FOOD] + fleet.satellites)
  return fuel*dist/10.0
}


// Shows info about the fleet
showFleet:proc(fleet:FleetType) {
  println "FLEET STATUS:"
  print "Fleet is at: " println fleet.location.name
  println "Assets: "
  i = 0 while i < 5 do i = i + 1 {
    print " " print ASSET_TYPE[i] print ": " println fleet.assets[i]
  }
  print "Food carriers:    " println fleet.carriers[FOOD]
  print "Fuel carriers:    " println fleet.carriers[FUEL]
  print "Empty transports: " println fleet.etrans
  print "Fighters:         " println fleet.fighters
  print "Satellites:       " println fleet.satellites
}

// Shows the galaxy around the given planet.
map:proc(planet:PlanetType) {
  left_top_x = max(0, planet.x-12)
  left_top_y = max(0, planet.y-12)
  // left_top_x,y cannot be within 24 of the size
  left_top_x = min(left_top_x, SIZE-24)
  left_top_y = min(left_top_y, SIZE-24)
  //print "at " print left_top_x print ", " println left_top_y
  print "+"
  x = 0 while x < 24 do x = x + 1 {
    print "-"
  }
  println "+"
  y = 0 while y < 24 do y = y + 1 {
    print "|"
    x = 0 while x < 24 do x = x + 1 {
      cell = galmap[x + left_top_x+ SIZE*(y+left_top_y)]
      if cell == 0 {print " "} else {
        print chr(cell)
      }
    }
    println "|"
  }
  print "+"
  x = 0 while x < 24 do x = x + 1 {
    print "-"
  }
  println "+"
}

// print info about the given planet
info:proc(planet:string) {
  planet = toUpper(planet)
  first = asc(planet[0]) - asc('A')
  index = IDS[first]
  if index == -1 {
    print "Unknown planet " println planet
    return
  } else {
    p = planets[index]
    showPlanet(p, false)
  }
}

help: proc {
  println
"
MAP: Show the map near where the fleet is.
*NEAr: Show info about nearby planets
FLEet: Show info about the fleet
*STAtus: Show where the fleet is, # of planets in each category, info about current planet
INFo: get info about a planet, its distance, and estimated fuel & time to get there
GALactica: get info about Galactica
*CONstruct ships (only on empire planets)
*BUY food, fuel (only on empire planets)
*DRAft troops (only on empire planets)
*TRAvel to another planet (if have enough fuel), time elapses
*SATellites: Send sats to non-empire planets
*ATTack the planet where the fleet is. (Only on non-empire planets)
*SLEep: Time elapses, Each planet produces resources, Occupied planets rebel and/or join, Satellites arrive at destination
*OCCupy: Set occupation fighters and toops (only on non-empire planets)
*TAXes: Collect taxes (only on empire planets)
*PROduction ratios: update production ratios (only on empire planets)
*DECommission troops (only on empire planets)
*SCRap ships (only on empire planets)
QUIt

*=not implemented yet
"
}

execute:proc(command:string, full_command:string) {
  if command=='QUI' {
    gameinfo.status=QUIT
  } elif command=="MAP" {
    print "Fleet is at: " println fleet.location.name
    map(fleet.location)
  } elif command=="FLE" {
    showFleet(fleet)
  } elif command=="CHE" {
    cheat()
  } elif command=="HEL" {
    help()
  } elif command=="GAL" {
    info("G")
  } elif command=="INF" {
    if length(full_command) < 6 {
      println "Must give planet name for INFO, e.g., 'INFO Galactica'"
    } else {
      info(full_command[5])
    }
  } else {
    println "Don't know how to do that yet, sorry. Try HELP"
  }
}


mainLoop: proc {
  gameinfo.status = IN_PROGRESS
  while gameinfo.status==IN_PROGRESS {
    print "\nToday is " println format_date(gameinfo.date)
    print "Your command: "
    full_command = input
    command=trim(full_command)
    println "\n----------------------------------------------------------"
    execute(command, full_command)
    println "----------------------------------------------------------\n"
    sleep(10)
    gameinfo.status = calculate_game_status() // Calc if won or lost
  }
  if gameinfo.status==WON { println "You won" }
  if gameinfo.status==LOST { println "You lost" }
  if gameinfo.status==QUIT { println "You quit" }
}


main {
  gameinfo = new GameInfoType
  gameinfo.num_empire=1
  gameinfo.num_independent=NUM_PLANETS-1
  gameinfo.date = 4000*100

  // TODO: Ask for difficulty level
  gameinfo.level = 5
  print "Difficulty level is " println gameinfo.level

  // TODO: ask for seed
  seed=1337
  // print "Random seed is " println seed
  last_rand=seed

  help()
  initPlanets()
  initFleet()
  mainLoop()
}

