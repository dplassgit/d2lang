PlanetType: record {
  status:int 
  assets:double[5] 
}
EMPIRE=2 
f:proc:PlanetType { 
    p = new PlanetType 
    p.status = EMPIRE 
    assets = p.assets 
    assets[0] = 123.4 // npe
    return p 
}
p = f() 
print "Should be 2: " println p.status
print "Should be 123.4: " println p.assets[0]
