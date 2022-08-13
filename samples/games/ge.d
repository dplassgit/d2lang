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
INDEPENDENT=0 OCCUPIED=1 EMPIRE=2
PRIMITIVE=0 LIMITED=1 ADVANCED=2 SUPERIOR=3
IN_PROGRESS=0 WON=1 LOST=-1 QUIT=-2


/////////////////////////////////////////////////////////
// CONSTANTS
/////////////////////////////////////////////////////////
NAMES=["Galactica", "Moonsweep", "Ootsi", "Farside", "Sparta", "Proyc", "Novena", "Drassa2", 
  "Kgolta", "Eventide", "Javiny", "Viejo", "Llythll", "Utopia", "Yang-tzu", "Zoe", "Twyrx", 
  "Harkon", "Bok", "Alhambra"]
NUM_PLANETS=20


/////////////////////////////////////////////////////////
// GLOBALS
/////////////////////////////////////////////////////////

planets: PlanetType[NUM_PLANETS]
gameinfo: GameInfoType
fleet: FleetType
SIZE=50
galmap:int[SIZE*SIZE]    // 0=no planet, else the ascii of the planet


/////////////////////////////////////////////////////////
// TYPES
/////////////////////////////////////////////////////////

PlanetType: record {
  id:int
  name:string
  abbrev:string    // first letter of planet
  x:int y:int    // location (0-50)
  population:double   // in millions
  status:int     // 1=occupied, 2=empire, 0=independent
  status_changed:bool  // 1 if status just changed, 0 if not. WHY?!
//  assets: double[5]  // amount of each type on hand: food, fuel, parts, troops, money
//  prod_ratio:int[5]   // ratio of each type of asset production
  civ_level:int    // primitive, limited, advanced, etc.
  troops:int    // # of troops on surface, or # of occupation troops
  fighters:int     // # of fighters in orbit, or # of occupation fighters
  sats_orbit:int    // # of satellites in orbit
  sats_enroute:int  // # of satellites en route
//  sats_arrive[MAX_SATS]:int  // arrival date (in DAYS) of each satellite
//  prices:int[2]    // food, fuel (note can only buy if status=empire)
  occupied_on:int    // date (years) planet was occupied
}

FleetType: record {
  location:PlanetType  // pointer to planet struct in global array
//  assets:int[5]    // amount of each type on hand: food, fuel, parts, troops, money
  etrans:int    // empty transports â€” WHERE ARE FULL TRANSPORTS?!
  fighters:int     // # of fighters
  satellites:int   // in inventory
//  carriers[2]:int   // # of food, fuel carriers; they carry 1000 units each
}

GameInfoType: record {
  date:int  // days since start. 100 days per â€œyearâ€�
  level:int  // difficulty level
  status:int
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
    name = NAMES[i]
    p.name = name
    p.abbrev = name[0]  // will this work? no.
    p.x = random(SIZE)
    p.y = random(SIZE)
    if (i != 0) {
      p.status = INDEPENDENT
      p.civ_level = random(4)
      p.population = 16.0 + tod(random(840))/10.0 + 3.0 * tod(random(10*gameinfo.level))/10.0
    } else {
      // Galactica
      p.civ_level = ADVANCED
      p.status = EMPIRE
      p.population = 35.0+10.0*(10.0-tod(gameinfo.level*10)/10.0)+tod(random(50))/10.0
      // code doesn't have to special-case galactica!
      p.sats_orbit = 3
    }
    // TODO: set up the assets, prod ratios, prices

    // set galmap[x,y] to planet name letter
    galmap[p.x+SIZE*p.y] = asc(p.abbrev)
    print "Planet[" print i print "] is " print name
    print " (" print p.x print ", " print p.y print ") population " print p.population
    print " civ_level: " println p.civ_level
    planets[i] = p
  }
}

initFleet:proc {
  fleet = new FleetType
  fleet.location = planets[0]
  // todo: set up fleet.assets
  fleet.etrans = ax_b(gameinfo.level, -10, 110)
  fleet.fighters = fleet.etrans + 100
  fleet.satellites = 11 - gameinfo.level/2
}


/////////////////////////////////////////////////////////
// MATHS
/////////////////////////////////////////////////////////

ds=[0.0,1.0,2.0,3.0,4.0,5.0,6.0,7.0,8.0,9.0]

// int to double.
tod: proc(i:int):double {
  neg = false
  if i < 0 {neg = true i = -i}
  d=0.0
  while i > 0 {
    last = i%10
    // I hate this, lol
    d = d*10.0 + ds[last]
    i = i / 10
  }
  if neg {d = -d}
  return d
}


ax_b:proc(x:int, a:int, b:int):int {
  return a*x+b
}

min:proc(a:int, b:int):int {
  if a < b { return a } return b
}

max:proc(a:int,b:int):int {
  if (a>b) { return a } return b
}

RANDOM_MOD=65535 
// get a random number from 0 to range
random:proc(range:int):int {
  return (next_random() * range)/RANDOM_MOD
}

last_rand:int
// returns a random number from 0 to RANDOM_MOD
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
  if gameinfo.date > (5000*100) {
    return LOST
  } else {
    // todo: when all are empire: WON
    return IN_PROGRESS
  }
}

toUpper:proc(s:int):int {
  if s >= asc('a') and s <= asc('z') { return s - (asc('a')-asc('A')) }
  return s
}

trim:proc(s:string):string {
  r = ''
  i = 0 while i < min(length(s), 3) do i = i + 1 {
    c = s[i]
    if c != '\n' { r = r + chr(toUpper(asc(c))) }
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
  print "Sleeping for " print days println " days"
  // TODO: in sleep if a planet changes status, update the map
}

// Shows the galaxy around the given planet.
map:proc(planet:PlanetType) {
  left_top_x = max(0, planet.x-12)
  left_top_y = max(0, planet.y-12)
  // TODO: left_top_x,y cannot be within 24 of the size
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

help: proc {
  println 
"
MAP: Show the map near where the fleet is.
*NEAr: show nearby planets (?)
*STAtus: Show where the fleet is, # of planets in each category, info about current planet
*INFo: get info about a planet, its distance, and estimated fuel & time to get there
*GALactica: get info about Galactica
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

execute:proc(command:string) {
  if command=='QUI' {
    gameinfo.status=QUIT
  } elif command=="MAP" {
    //i = 0 while i < NUM_PLANETS do i = i + 1 {
      //p = planets[i]
      //print "NEAR " println p.name
      //map(p)
    //}
    print "Fleet is at: " println fleet.location.name
    map(fleet.location)
  } elif command=="HEL" {
    help() 
  } else {
    println "Don't know how to do that yet, sorry. Try HELP"
  }
}


mainLoop: proc {
  gameinfo.status = IN_PROGRESS
  while gameinfo.status==IN_PROGRESS {
    print "Today is " println format_date(gameinfo.date)
    println "Your command:"
    command = input
    command=trim(command)
    execute(command)
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
  print "Random seed is " println seed
  last_rand=seed

  help()
  initPlanets()
  initFleet()
  mainLoop()
}

