/////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////
// GALACTIC EMPIRED
/////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////

atoi: extern proc(s:string):int
lround: extern proc(x: double): int
round: extern proc(x: double): double
sqrt: extern proc(d: double): double
time: extern proc(ignored: int): int

/////////////////////////////////////////////////////////
// ENUMERATIONS
/////////////////////////////////////////////////////////

// These are both for assets and production ratios
FOOD=0 FUEL=1 PARTS=2 DRAFTABLE=3 MONEY=4

ASSET_TYPE=["Food", "Fuel", "Parts", "Draftable", "Money"]

INDEPENDENT=0 OCCUPIED=1 EMPIRE=2
STATUSES=["Independent", "Occupied", "Empire"]

PRIMITIVE=0 LIMITED=1 ADVANCED=2 SUPERIOR=3
CIV_LEVELS=["Primitive", "Limited", "Advanced", "Superior"]

// Game status
IN_PROGRESS=0 WON=1 LOST=-1 QUIT=-2

SPACE = 0 LAND = 1

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
galmap: int[SIZE*SIZE]    // 0=no planet, else the ascii of the planet


/////////////////////////////////////////////////////////
// TYPES
/////////////////////////////////////////////////////////

PlanetType: record {
  id: int
  name: string
  x: int y: int        // location (0-50)

  status: int          // occupied, empire, independent
  civ_level: int       // primitive, limited, advanced, etc.
  occupied_on: int     // date planet was occupied
  population: double   // in millions

  assets: double[5]    // amount of each type on hand: food, fuel, parts, draftable, money
  prod_ratio: int[5]   // ratio of each type of asset production
  // TODO: maybe these should be doubles
  prices: int[2]       // food, fuel (note can only buy if status=empire)

  sats_arrive: int[3]  // arrival date (in DAYS) of each satellite
  sats_orbit: int      // # of satellites in orbit
  sats_enroute: int    // # of satellites en route

  // TODO: maybe these should be doubles
  troops: int          // # of troops on surface, or # of occupation troops
  fighters: int        // # of fighters in orbit, or # of occupation fighters
}

FleetType: record {
  location: PlanetType  // pointer to planet struct
  assets: double[5]     // amount of each type on hand: food, fuel, parts, draftable (SKIP), money
  carriers: int[2]      // # of food, fuel carriers; they carry 1000 units each
  etrans: int           // empty transports (full transports = troops)
  satellites: int       // in inventory

  // TODO: Maybe these should be doubles
  troops: int           // # troops
  fighters: int         // # of fighters
}

GameInfoType: record {
  date: int       // days since start. 100 days per "year"
  level: int      // difficulty level
  leveld: double  // level, as a double
  status: int     // in progress, lost, won

  // num_planets: int[3]   // # independent, occupied, empire
  num_empire: int       // # of empire, independent, occupied
  num_independent: int
  num_occupied: int
}


/////////////////////////////////////////////////////////
// INITIALIZERS
/////////////////////////////////////////////////////////

initPlanets: proc {
  i = 0 while i < NUM_PLANETS do i = i + 1 {
    p = new PlanetType
    planets[i] = p

    p.name = NAMES[i]

    // 1. assign location randomly on grid
    p.x = random(SIZE)
    p.y = random(SIZE)

    // set galmap[x, y] to planet name letter
    galmap[p.x+SIZE*p.y] = asc(p.name)

    // TODO: Randomize all of these +/- level*2 %
    if i != 0 {
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
        prod_ratio[DRAFTABLE] = 33
      } elif p.civ_level == LIMITED {
        // if limited base is 25 (min 12)
        prod_ratio[FOOD] = 25
        prod_ratio[MONEY] = 25
        prod_ratio[DRAFTABLE] = 25
        prod_ratio[FUEL] = 25
      } else { // advanced, superior
        // else base is 20 (min 10)
        prod_ratio[FOOD] = 20
        prod_ratio[MONEY] = 20
        prod_ratio[PARTS] = 20
        prod_ratio[DRAFTABLE] = 20
        prod_ratio[FUEL] = 20

        p.fighters = (random(400)+10)*(p.civ_level+1)/10 + 2*random(10*gameinfo.level)/10
      }

      // 7. set troops, fighters based on level & civ_type (no fighters if <ADVANCED)
      p.troops = (random(400)+10)*(p.civ_level+1)/10 + 2*random(10*gameinfo.level)/10

    } else {
      // Galactica
      // 2. assign population based on level/ the higher the level the lower the pop
      p.population = 35.0+10.0*(10.0-gameinfo.leveld)+tod(random(50))/10.0

      // 3. set civ level to advanced
      p.civ_level = ADVANCED

      // 4. set status = empire
      p.status = EMPIRE

      // 5. set all percentages=20
      prod_ratio = p.prod_ratio
      prod_ratio[FOOD] = 20
      prod_ratio[MONEY] = 20
      prod_ratio[PARTS] = 20
      prod_ratio[DRAFTABLE] = 20
      prod_ratio[FUEL] = 20

      // 6. set satellites=3; this way the 'display planet'
      // code doesn't have to special-case galactica!
      p.sats_orbit = 3
      // fun fact, galactica starts with 0 troops and 0 fighters (!)
    }

    // 8. set food price, fuel price based on level
    // TODO: prices go up and down with the market,
    // TODO: start with random factor
    prices = p.prices
    prices[FOOD] = toi(4.5+gameinfo.leveld/2.0)
    prices[FUEL] = toi(2.5+gameinfo.leveld/2.0)

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
    assets[FOOD] = abc(3.3973, -75.85, 432.41, gameinfo.leveld)

    // money -457.4*level+3583
    // money 27.268x2-635.19x+3799
    assets[MONEY] = abc(27.268, -635.19, 3799.0, gameinfo.leveld)

    // if < advanced, no parts
    if (p.civ_level >= ADVANCED) {
      // planet->assets[PARTS] = ax_b(level, -68, 506)
      // parts 4.4802x2-97.611x+541.88
      assets[PARTS] = abc(4.4802, -97.611, 541.88, gameinfo.leveld)
    }

    // DRAFTABLE/TROOPS -17.9x + 133.5
    // troops 1.2727x2 - 26.198x + 143.58
    // round up to nearest double
    // this should be based on population, no?
    assets[DRAFTABLE] = round(abc(1.2727, -26.198, 143.58, gameinfo.leveld) + 0.5)

    // if primitive, no fuel
    if (p.civ_level > PRIMITIVE) {
      // fuel -29.6x + 233.4
      // fuel 1.7437x2 - 40.969x + 247.21
      assets[FUEL] = abc(1.7437, -40.969, 247.21, gameinfo.leveld)
    }
  }
}

initFleet: proc {
  fleet = new FleetType
  fleet.location = planets[0]

  // TODO: RANDOMIZE +/- 1% * level

  // initialize based on level
  assets = fleet.assets
  // money = -150*level+2650;
  assets[MONEY] = abc(0.0, -150.0, 2650.0, gameinfo.leveld)

  // troops=10*(11-level)=110-10*level;
  fleet.troops = toi(abc(0.0, -10.0, 110.0, gameinfo.leveld) + 0.5)

  // freighters=(-3*level+43)/8;
  carriers = fleet.carriers
  carriers[FOOD]=(43-3*gameinfo.level)/8

  // fuel_ships=(-6*level+77)/16;
  carriers[FUEL]=(77-6*gameinfo.level)/16

  // food=freighters*1000-7000*(level-1)/256;
  foods_amt = carriers[FOOD]*1000-(7000*(gameinfo.level-1))/256 // a fine foods amount
  assets[FOOD] = tod(foods_amt)

  // fuel=fuel_ships*1000-7000*(level-1)/256;
  fuel_amt = carriers[FUEL]*1000-(7000*(gameinfo.level-1))/256
  assets[FUEL] = tod(fuel_amt)

  // Empty transports. "Filled" transports = troops
  fleet.etrans = ax_b(gameinfo.level, -10, 110)

  fleet.fighters = fleet.etrans + 100
  fleet.satellites = 11 - gameinfo.level/2
}


/////////////////////////////////////////////////////////
// MATHS
/////////////////////////////////////////////////////////

DS=[0.0, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0]

// int to double.
tod: proc(i: int): double {
  neg = false
  if i < 0 {neg = true i = -i}
  d=0.0
  place = 1.0
  while i > 0 {
    last = i%10
    // I hate this
    d = d + place * DS[last]
    place = place * 10.0
    i = i / 10
  }
  if neg {return -d}
  return d
}


// double to int
toi: proc(d: double): int {
  return lround(d)
}

// a*x+b
ax_b: proc(x: int, a: int, b: int): int { return a*x+b }

// a*x*x+b*x+c
abc: proc(a: double, b: double, c: double, x: double): double {
  return a*x*x+b*x+c
}

min: proc(a: int, b: int): int { if a < b { return a } return b }
max: proc(a: int, b: int): int { if a > b { return a } return b }


calc_distance: proc(p1: PlanetType, p2: PlanetType): double {
  xd = tod(p1.x-p2.x)
  yd = tod(p1.y-p2.y)
  dist = xd*xd+yd*yd
  return sqrt(dist)
}

// TOOD: use rand()
RANDOM_MOD=65535

// Get a random number from 0 to 'range' exclusive
random: proc(range: int): int {
  return (next_random() * range)/RANDOM_MOD
}

last_rand: int
// returns a random number from 0 to RANDOM_MOD (exclusive)
next_random: proc: int {
  // m=modulus, a=multiplier, c=increment
  a=75 c=74
  last_rand = ((last_rand * a) + c) % RANDOM_MOD
  return last_rand
}


/////////////////////////////////////////////////////////
// UTILITIES
/////////////////////////////////////////////////////////

calculate_game_status: proc: int {
  if gameinfo.num_empire == 20 { return WON }
  // quit
  if gameinfo.status != IN_PROGRESS { return gameinfo.status }
  if gameinfo.date > (5000*100) { return LOST }
  return IN_PROGRESS
}

toUpper: proc(ss: string): string {
  s = asc(ss)
  if s >= asc('a') and s <= asc('z') { return chr(s - (asc('a') - asc('A'))) }
  return ss[0]
}

trim: proc(s: string): string {
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
    val = chr((i % 10) + asc('0')) + val
  }
  return val
}

format_date: proc(d: int): string {
  year = d/100
  days = d%100
  result = toString(year) + "."
  if days < 10 {result = result + "0"}
  result = result + toString(days)
  return result
}


// Count the # of planets with this status
count_planets: proc(status: int): int {
  count=0
  i = 0 while i < NUM_PLANETS do i = i + 1 {
    p = planets[i]
    if p.status == status {
      count = count + 1
    }
  }
  return count
}

find_planet: proc(planet: string): PlanetType {
  planet = toUpper(planet)
  first = asc(planet[0]) - asc('A')
  if first < 0 or first > 25 {
    return null
  }
  index = IDS[first]
  if index == -1 {
    return null
  } else {
    return planets[index]
  }
}

calc_food_needed: proc(dist: double): double {
  food = fleet.troops * 5 + fleet.fighters
  return tod(food) * dist / 10.0
}

calc_fuel_needed: proc(dist: double): double {
  fuel = fleet.troops + fleet.etrans + fleet.fighters +
         fleet.carriers[FUEL] + fleet.carriers[FOOD] + fleet.satellites
  return tod(fuel) * dist / 10.0
}

move_sats: proc(p: PlanetType) {
  first = true
  if p.sats_enroute > 0 {
    j = 0 while j < 3 do j = j + 1 {
      sats_arrive = p.sats_arrive
      if sats_arrive[j] > 0 and sats_arrive[j] < gameinfo.date {
        // it arrived!
        if first {
          println ""
          first = false
        }
        print "Satellite arrived at " println p.name
        sats_arrive[j] = 0
        p.sats_enroute = p.sats_enroute - 1
        p.sats_orbit = p.sats_orbit + 1
      }
    }
  }
}

// Assumes planet is occupied
rebel: proc(p: PlanetType, days: double) {
  if (p.troops > 0) {
    attrition = 0.01 * gameinfo.leveld * (days/100.0) * tod(p.troops)
    // can't go negative
    p.troops = max(p.troops - lround(attrition + 0.5), 0)
  }
  if (p.fighters > 0) {
    attrition = 0.01 * gameinfo.leveld * (days/100.0) * tod(p.fighters)
    p.fighters = max(p.fighters - lround(attrition + 0.5), 0)
  }
}


maybe_join_empire: proc(planet: PlanetType) {
  if planet.troops > 0 and
    ((planet.civ_level >= ADVANCED and planet.fighters > 0) or planet.civ_level < ADVANCED) {

    // troops & fighters are > 0
    // (if limited/primitive the fighters can be 0...)
    years_since = (gameinfo.date - planet.occupied_on)/100
    if years_since > random(4 * gameinfo.level) {
      println "\n=============== NEWS FLASH ===============\n"
      print planet.name println " joined the empire!!!"
      println "\n=============== NEWS FLASH ===============\n"

      // and planet has been occupied for random(4*level)
      // years, it joins the empire;
      set_status(planet, EMPIRE)

      // reset all assets
      planet.troops = 0
      planet.fighters = 0
      a = planet.assets
      i = 0 while i < 5 do i = i + 1 {
        a[i] = a[i] / 2.0
      }
      a[DRAFTABLE] = round(a[DRAFTABLE])

      planet.sats_orbit = 3
      planet.sats_enroute = 0
    }
  }
}


adjust_planet_counts: proc(status:int, direction: int) {
  if status == INDEPENDENT {
    gameinfo.num_independent = gameinfo.num_independent + direction
  } elif status == OCCUPIED {
    gameinfo.num_occupied = gameinfo.num_occupied + direction
  } else {
    gameinfo.num_empire = gameinfo.num_empire + direction
  }
}

set_status: proc(p: PlanetType, new_status: int) {
  if p.status != new_status {
    adjust_planet_counts(new_status, +1)
    adjust_planet_counts(p.status, -1)
    p.status = new_status
  }
}


// TODO(bug#155): we can't write fleet.assets[DRAFTABLE] = fleet.assets[DRAFTABLE] + 1.0
add_assets:proc(assets:double[], index: int, amount:double): double {
  // don't let it go negative
  assets[index] = assets[index] + amount
  if assets[index] < 0.5 {
    assets[index] = 0.0
  }
  return assets[index]
}


// increase the population of the planet
// in 1000 years we want to increase population by, say, 10*level percent.
// so in 1 year it increases by 10*level/1000 percent.
// = level/100 percent
// = 0.01 * (level / 100) * population per year
// = 0.01 * (level / 100) * population * days/100
// = (0.01 / (100*100)) * level * population * days
grow: proc(p: PlanetType, days: int) {
  increase = (0.01 / (100.0*100.0)) * gameinfo.leveld * tod(days) * p.population
  p.population = p.population + increase
}


build_army: proc(p: PlanetType, days: int) {
  assets = p.assets
  draftees= 0.005 * gameinfo.leveld * (tod(days)/100.0) * assets[DRAFTABLE]
  if (draftees >= 0.5) {
    idraftees = lround(draftees + 0.5)
    p.troops = p.troops + idraftees
    assets[DRAFTABLE] = assets[DRAFTABLE] - tod(idraftees)
  }
}


// Buy one type of product at fleet's planet.
buy_one_type: proc(fleet:FleetType, index: int): bool {
  planet = fleet.location
  available = lround(planet.assets[index])
  if available > 0 {
    carry = lround(tod(fleet.carriers[index] * 1000) - fleet.assets[index])
    afford = lround(fleet.assets[MONEY] / tod(planet.prices[index]))

    while true {
      // TODO(bug #142): Use % syntax
      print planet.name print " has " print available print " " print ASSET_TYPE[index] println " units to buy."
      print "You can afford " print afford println " units."
      print "You can carry " print carry println " units."
      print "How many units to purchase (number or 'max')? "
      samount = input
      trimmed = trim(samount)
      if trimmed == 'max' or trimmed == 'MAX' {
        famount = min(available, min(afford, carry))
        print "Buying max: " println famount
      } else {
        famount = atoi(samount)
      }
      if famount == 0 {
        break
      }
      if famount > available {
        println "Can't buy that much. Try again."
      } elif famount > carry {
        println "Can't carry that much. Try again."
      } elif famount > afford {
        println "Can't afford that much. Try again."
      } elif famount < 0 {
        println "You're killing me smalls."
      } elif famount > 0 {
        print "\nA fine " print ASSET_TYPE[index] print "s amount: " println famount
        print "This cost: " println famount * planet.prices[index]
        println ""
        buy_transaction(fleet, index, famount)
        elapse(25)
        return true
      }
      println ""
    }
  } else {
    print "None " print ASSET_TYPE[index] print " available for purchase at " println planet.name
  }
  return false
}

// Complete the buy transaction by updating amounts of the fleet and planet.
buy_transaction: proc(fleet: FleetType, index: int, amount: int) {
  planet = fleet.location
  //   a. increase fleet assets by value
  add_assets(fleet.assets, index, tod(amount))
  //   b. decrease assets by value
  add_assets(planet.assets, index, -tod(amount))
  // note, do not increase money of planet, otherwise we can just collect taxes and it would be free...
  //   c. decrease money (!) - this may have been a bug in the .asm
  add_assets(fleet.assets, MONEY, tod(-amount* planet.prices[index]))
}


/////////////////////////////////////////////////////////
// COMMANDS
/////////////////////////////////////////////////////////

elapse: proc(days: int) {
  gameinfo.date = gameinfo.date + days

  i = 1 while i < NUM_PLANETS do i = i + 1 {
    p = planets[i]
    if p.status != EMPIRE {
      move_sats(p)
    }
    grow(p, days)

    // TODO: produce

    if p.status == INDEPENDENT {
      build_army(p, days)
    }

    if p.status == OCCUPIED {
      rebel(p, tod(days))
    }

    // update status
    if p.status == OCCUPIED and p.troops == 0 and p.fighters == 0 {
      println "\n=============== NEWS FLASH ===============\n"
      print p.name println " rebelled and is independent again!"
      println "\n=============== NEWS FLASH ===============\n"

      // back to independent
      set_status(p, INDEPENDENT)

      // TODO: this will be moved to "build_rebellion" eventually
      // draft half the draftable people
      p.troops = toi(p.assets[DRAFTABLE] / 2.0)
      add_assets(p.assets, DRAFTABLE, -tod(p.troops))

      // TODO: make some fighters
    } elif p.status == OCCUPIED {
      maybe_join_empire(p)
    }
  }
}


cheat: proc() {
  i = 0 while i < NUM_PLANETS do i = i + 1 {
    p = planets[i]
    println "----------------------------------------------------------"
    print "Planet[" print i print "]: "
    show_planet(p, true)
  }
}


show_planet: proc(p: PlanetType, cheat: bool) {
  print "PLANET INFO: "
  sats = p.sats_orbit
  if cheat or fleet.location == p or p.status == OCCUPIED {
    // if the fleet is here, it's as if we've sent 3 sats
    sats = 3
  }
  print p.name
  if (cheat) {
    print " (" print p.x print ", " print p.y println ")"
  }
  println ""
  print "Status:      " println STATUSES[p.status]

  // 2. if satellites > 0 draw civ level;
  // 3. If satellites > 1 draw population;
  // 4. b. if status=empire, make assets visible, troops, fighters & sats invisible;
  //    c.
  //       i. if status=occupied or indep, make assets invisible, fighters troops sats status visible;
  //       ii. if satellites > 2 draw troops, fighters, sats;

  if sats > 0 {
    print "Civ level:   " println CIV_LEVELS[p.civ_level]
  }
  if sats > 1 {
    print " Population: " println p.population
  }
  if cheat or p.status == EMPIRE {
    println " Assets:     "
    print "   Food:      " println p.assets[FOOD]
    print "   Fuel:      " println p.assets[FUEL]
    print "   Parts:     " println p.assets[PARTS]
    print "   Draftable: " println p.assets[DRAFTABLE]
    print "   Money:     " println p.assets[MONEY]
  }
  if cheat or (p.status != EMPIRE and sats > 2) {
    print " Troops:     " println p.troops
    print " Fighters:   " println p.fighters
  }
  if cheat or p.status == EMPIRE {
    print " Prices:     " println p.prices
  }
  if cheat or p.status != EMPIRE {
    // put the date of the next satellite arrival
    print " Sats:       " print p.sats_orbit
    if p.sats_enroute > 0 {
      print " (" print p.sats_enroute print " enroute @"
      j = 0 while j < 3 do j = j + 1 {
        if p.sats_arrive[j] > 0 {
          print format_date(p.sats_arrive[j])
          break
        }
      }
      println ")"
    } else {
      println ""
    }
  }

  if fleet.location != p {
    distance = calc_distance(fleet.location, p)
    print "Distance:        " print distance println " ly"
    print "Est travel time: " print distance * 10.0 println " days"
    print "Estimated food:  " println calc_food_needed(distance)
    print "Estimated fuel:  " println calc_fuel_needed(distance)
  }
}

// Shows info about the fleet
show_fleet: proc(fleet: FleetType) {
  print "FLEET AT: " println fleet.location.name
  i = 0 while i < 5 do i = i + 1 {
    if i != DRAFTABLE {
      print " " print ASSET_TYPE[i] print ": " println fleet.assets[i]
    }
  }
  print "Food carriers:    " println fleet.carriers[FOOD]
  print "Fuel carriers:    " println fleet.carriers[FUEL]
  print "Empty transports: " println fleet.etrans
  print "Troops:           " println fleet.troops
  print "Fighters:         " println fleet.fighters
  print "Satellites:       " println fleet.satellites

  elapse(10)
}

// Shows the galaxy around the given planet in a 24x12 window
map: proc(planet: PlanetType) {
  left_top_x = max(0, planet.x-24)
  left_top_y = max(0, planet.y-12)
  // left_top_x, y cannot be within 24 of the size
  left_top_x = min(left_top_x, SIZE-48)
  left_top_y = min(left_top_y, SIZE-24)
  //print "at " print left_top_x print ", " println left_top_y
  print "+"
  x = 0 while x < 48 do x = x + 1 {
    print "-"
  }
  println "+"
  y = 0 while y < 24 do y = y + 1 {
    print "|"
    x = 0 while x < 48 do x = x + 1 {
      cell = galmap[x + left_top_x+ SIZE*(y+left_top_y)]
      if cell == 0 {print " "} else {
        print chr(cell)
      }
    }
    println "|"
    // TODO: show information about the planet(s) on this line
  }
  print "+"
  x = 0 while x < 48 do x = x + 1 {
    print "-"
  }
  println "+"
  print "Empire planets:   " println count_planets(EMPIRE)
  print "Occupied planets: " println count_planets(OCCUPIED)
  print "Indep. planets:   " println count_planets(INDEPENDENT)

  elapse(10)
}

// print info about the given planet
info: proc(p: PlanetType) {
  show_planet(p, false)
  elapse(10)
}

sat: proc(p: PlanetType) {
  if p.status == EMPIRE {
    println "Don't need to send satellites to an empire planet!"
    return
  }
  if p.sats_orbit + p.sats_enroute == 3 {
    print "Already sent 3 satellites to " println p.name
    return
  }
  if fleet.satellites == 0 {
    println "No satellites in inventory!"
    return
  }
  fleet.satellites = fleet.satellites - 1

  distance = calc_distance(fleet.location, p)
  // round distance up
  idistance = toi(distance+0.5)
  when = gameinfo.date + gameinfo.level*idistance + 10*random(gameinfo.level)
  sa = p.sats_arrive
  // which satellite?
  nsat = p.sats_enroute + p.sats_orbit
  sa[nsat] = when
  p.sats_enroute = p.sats_enroute + 1
  print "Sent! Estimated arrival: " println format_date(when)
  elapse(10)
}


// Embark, if we have enough fuel and food
embark: proc(p: PlanetType) {
  if fleet.location == p {
    print "Already at " println p.name
    return
  }

  distance = calc_distance(fleet.location, p)
  food_needed = calc_food_needed(distance)
  fuel_needed = calc_fuel_needed(distance)

  // make sure we have enough food
  fleet_assets = fleet.assets
  if fleet_assets[FOOD] >= food_needed {
    // make sure we have enough fuel!
    if fleet_assets[FUEL] >= fuel_needed {
      // ok, we have enough
      // TODO: randomly take +/- 1%
      fleet_assets[FUEL] = fleet_assets[FUEL] - fuel_needed
      fleet_assets[FOOD] = fleet_assets[FOOD] - food_needed

      // 3. move fleet;
      fleet.location = p

      // 4. elapse time;
      // multiply distance by 10 to go from ly to years;
      // TODO: randomly take +/1 1%
      // distance * (0.99 + tod(random(2))/100.0)
      elapse(toi(distance)*10)

      println "\n=============== NEWS FLASH ===============\n"
      print "The Imperial Fleet arrived at " print p.name print " @ " println format_date(gameinfo.date)
      println "\n=============== NEWS FLASH ===============\n"

      show_planet(p, false)
    } else {
      print "Not enough fuel to get to " print p.name print ". Need " println fuel_needed
    }
  } else {
    print "Not enough food to get to " print p.name print ". Need " println food_needed
  }
}


occupy: proc(location: PlanetType, should_elapse: bool) {
  if location.status != OCCUPIED {
    println "Cannot occupy an independent or empire planet."
    return
  }

  print location.name println " occupation status:\n"
  if location.civ_level >= ADVANCED {
    print "Fighters: " println location.fighters
    print "Troops:   " println location.troops

    fighters = 0
    while true {
      // get fighters
      print "How many fighters to deploy? (there are " print fleet.fighters println " available): "
      fstring = input
      fighters = atoi(fstring)
      if fighters < 0 or fighters > fleet.fighters {
        println "Out of range, try again."
      } else {
        break
      }
    }
    location.fighters = location.fighters + fighters
    fleet.fighters = fleet.fighters - fighters
  } else {
    print "Troops: " println location.troops
  }

  // get troops
  troops = 0
  while true {
    print "How many troops to deploy? (there are " print fleet.troops println " available): "
    tstring = input
    troops = atoi(tstring)
    if troops < 0 or troops > fleet.troops {
      println "Out of range, try again."
    } else {
      break
    }
  }
  location.troops = location.troops + troops
  fleet.troops = fleet.troops - troops
  // we now have more empty transports. I think this was a bug in the old c code
  fleet.etrans = fleet.etrans + troops

  if troops > 0 or fighters > 0 {
    print "Updated " print location.name println " occupation status:\n"
    if location.civ_level >= ADVANCED {
      print "Fighters: " println location.fighters
      print "Troops:   " println location.troops
    } else {
      print "Troops: " println location.troops
    }
  }

  if should_elapse {
    elapse(50)
  }
}

attack: proc(location: PlanetType) {
  if location.status != INDEPENDENT {
    println "Can only attack an independent planet."
    return
  }
  print "Attacking " println location.name println ""
  battle_location=LAND
  if location.civ_level >= ADVANCED {
    println "Space battle commencing..."
    battle_location=SPACE
  } else {
    println "Land battle commencing..."
  }
  running = true
  iters = 0
  casualties = 0.0
  while running {
    if battle_location == SPACE {
      us = fleet.fighters
      them = location.fighters
    } else {
      us = fleet.troops
      them = location.troops
    }

    effective_them = them
    // superior fights 50% better!
    // advanced fights the same
    // limited fights 75% as well
    // primitive fights half as well
    // so adjust the 'effective' size of their fleet.

    if location.civ_level == SUPERIOR { effective_them = 3*them/2 }
    elif location.civ_level == LIMITED { effective_them = 3*them/4 }
    elif location.civ_level == PRIMITIVE { effective_them = them/2 }

    if us > 0 and them > 0 {
      // probability of each one-on one-battle of being won by empire is
      // # of empire ships/(# of empire ships+# of enemy ships)
      // use 'effective_them' to determine probability
      // but just 'them' every where else
      prob = (10000*us)/(100*(us + effective_them))

      if (prob > random(100)) {
        // then we win!
        them = them - 1
      } else {
        // then we lose
        us = us - 1
      }
      casualties = casualties + 1.0

      // update the number in memory
      if battle_location == SPACE {
        fleet.fighters = us
        location.fighters = them
      } else {
        fleet.troops = us
        location.troops = them
      }

      // draw the #s
      if (iters%10) == 0 {
        print "Empire forces: " print us
        print ". Enemy forces: " print them
        print ". Winning probability: " print prob println "%"
      }

      // TODO: every few iterations, ask if the user wants to continue.
      // TODO: delay a little bit, for dramatic effect

    } elif us >= 0 and them == 0 {
      if (battle_location == SPACE) {
        println "\nSpace battle won! Proceeding to land battle...\n"
        battle_location = LAND
      } else {
        println "\nLand battle won!"

        // should this be after the "elapse"?
        location.occupied_on = gameinfo.date
        set_status(location, OCCUPIED)
        occupy(location, false)
        break
      }
    } elif us == 0 {
      println "\nYou lost."
      break
    }
    iters = iters + 1
  }

  if (casualties > 0.0) {
    elapse(lround(casualties/10.0+100.0))
  } else {
    elapse(10)
  }
}

draft: proc(location: PlanetType) {
  if location.status != EMPIRE{
    println "Can only draft on an empire planet."
    return
  }

  print location.name print " has " print location.assets[DRAFTABLE] println " available."
  print "The fleet has room for " print fleet.etrans println " additional troops."
  draftees = 0
  while true {
    print "How many troops to draft? "
    dstring = input
    draftees = atoi(dstring)
    if draftees < 0 or draftees > fleet.etrans or tod(draftees) > location.assets[DRAFTABLE] {
      println "Out of range, try again."
    } else {
      break
    }
  }
  if draftees > 0 {
    // a. decrease etrans by value;
    fleet.etrans = fleet.etrans - draftees
    // b. increase troops by value;
    fleet.troops = fleet.troops + draftees
    // c. decrease draftable on planet by value;
    add_assets(location.assets, DRAFTABLE, -tod(draftees))

    elapse(50)
  } else {
    elapse(10)
  }
}


decommission: proc(location: PlanetType) {
  print "The fleet has " print fleet.troops println " troops."
  discharged = 0
  while true {
    print "How many troops to decommission? "
    dstring = input
    discharged = atoi(dstring)
    if discharged < 0 or discharged > fleet.troops {
      println "Out of range, try again."
    } else {
      break
    }
  }
  if discharged > 0 {
    print "Decommissioning " print discharged println " troops."
    // we now have more empty transports. I think this was a bug in the old c code
    fleet.etrans = fleet.etrans + discharged
    fleet.troops = fleet.troops - discharged
    if (fleet.location.status == EMPIRE) {
      // we get half of them back
      re_uppers = toi(tod(discharged)/2.0)
      print re_uppers println " will be available for drafting later."
      add_assets(fleet.location.assets, DRAFTABLE, -tod(re_uppers))
    }
    elapse(50)
  } else {
    elapse(10)
  }
}


tax:proc(planet:PlanetType) {
  if planet.status != EMPIRE {
    println "Cannot collect taxes on non-empire planet"
    return
  }
  // 2. get planetary money
  money = planet.assets[MONEY]
  if money > 0.0 {
    print "Collecting $" print money print " from " println planet.name
    // if planetary money =0, don't do anything!;
    // 3. add planetary money to fleet money
    add_assets(fleet.assets, MONEY, money)

    // 4. set planetary money to 0.
    add_assets(planet.assets, MONEY, -money)

  }
  // 5. update statuses;
  elapse(10)
}

buy:proc(planet:PlanetType) {
  if planet.status != EMPIRE {
    println "Cannot buy materials on non-empire planet"
    return
  }

  // 1. buy food
  bought = buy_one_type(fleet, FOOD)

  // 2. if appropriate, buy fuel.
  if planet.civ_level >= LIMITED {
    bought = bought or buy_one_type(fleet, FUEL)
  }
  if not bought {
    elapse(10)
  }
}


help: proc {
  println
"
MAP: Show the map near the given planet
FLEet: Show info about the fleet
SLEep: Time elapses, Each planet produces resources, Occupied planets rebel and/or join, Satellites arrive at destination
INFo: get info about a planet, its distance, and estimated fuel & time to get there
GALactica: get info about Galactica
SATellites: Send satellites to non-empire planets
EMBark to another planet (if have enough fuel)
ATTack the planet where the fleet is. (only on independent planets)
OCCupy: Set occupation fighters and troops (only on occupied planets)
DRAft troops (only on empire planets)
TAXes: Collect taxes (only on empire planets)
BUY food, fuel (only on empire planets)
DECommission troops (only on empire planets)
*CONstruct ships (only on empire planets)
*PROduction ratios: update production ratios (only on empire planets)
*SCRap ships (only on empire planets)
QUIt

*=not implemented yet
"
}

execute: proc(command: string, full_command: string) {
  if command=='QUI' {
    gameinfo.status=QUIT
  } elif command == 'SLE' {
    // sleep
    // 1. find the space. if no space, sleep 10
    i = 3 while i < length(full_command) do i = i + 1 {
      if full_command[i] == ' ' {
        if i + 1 < length(full_command) {
          // get the next number after the space
          nc = full_command[i+1]
          d = asc(nc) - asc('0')
          // get the next number
          if d >= 0 and d <= 9 {
            print "Sleeping for " print 10*d println " days"
            elapse(10*d)
            return
          }
        }
      }
    }
    // No space, or no next number
    println "Sleeping for 10 days"
    elapse(10)

  } elif command=="MAP" {
    if length(full_command) < 5 {
      map(fleet.location)
    } else {
      p = find_planet(full_command[4])
      if p == null {
        p = planets[0]
      }
      map(p)
    }
  } elif command=="FLE" {
    show_fleet(fleet)
  } elif command=="CHE" {
    cheat()
  } elif command=="HEL" {
    help()
  } elif command=="ATT" {
    attack(fleet.location)
  } elif command=="OCC" {
    occupy(fleet.location, true)
  } elif command=="DRA" {
    draft(fleet.location)
  } elif command=="DEC" {
    decommission(fleet.location)
  } elif command=="TAX" {
    tax(fleet.location)
  } elif command=="GAL" {
    info(planets[0])
  } elif command=="BUY" {
    buy(fleet.location)
  } elif command=="INF" {
    if length(full_command) < 6 {
      info(fleet.location)
    } else {
      p = find_planet(full_command[5])
      if p == null {
        println "Unknown planet"
      } else {
        info(p)
      }
    }
  } elif command=="SAT" {
    if length(full_command) < 5 {
      println "Must give planet name for SAT, e.g., 'SAT Ootsi'"
    } else {
      p = find_planet(full_command[4])
      if p == null {
        println "Unknown planet"
      } else {
        sat(p)
      }
    }
  } elif command=="EMB" {
    if length(full_command) < 5 {
      println "Must give planet name for EMB, e.g., 'EMB Ootsi'"
    } else {
      p = find_planet(full_command[4])
      if p == null {
        println "Unknown planet"
      } else {
        embark(p)
      }
    }
  } else {
    println "Don't know how to do that, sorry. Try HELP"
  }
}


mainLoop: proc {
  gameinfo.status = IN_PROGRESS
  while gameinfo.status==IN_PROGRESS {
    print "\nToday is " println format_date(gameinfo.date)
    print "\nYour command: "

    full_command = input
    command=trim(full_command)
    println "\n----------------------------------------------------------"
    execute(command, full_command)
    println "----------------------------------------------------------\n"
    gameinfo.status = calculate_game_status() // Calc if won or lost
  }
  if gameinfo.status==WON { println "You won!" }
  if gameinfo.status==LOST { println "You lost!" }
  if gameinfo.status==QUIT { println "You quit" }
}


main {
  gameinfo = new GameInfoType
  gameinfo.num_empire=1
  gameinfo.num_independent=NUM_PLANETS-1
  gameinfo.date = 4000*100

  // TODO: Ask for difficulty level
  gameinfo.level = 5
  gameinfo.leveld = tod(gameinfo.level)
  print "Difficulty level is " println gameinfo.level

  seed=time(0)
  seed = 1661383298
  print "Random seed is " println seed
  last_rand=seed

  help()
  initPlanets()
  initFleet()
  mainLoop()
}

