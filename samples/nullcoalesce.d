a='good' b=null c=b??a
println "Should be good:" 
println c

rt: record{f:string}
ar=new rt
ar.f="good"
br:rt
br=null
cr=br??ar
println "Should be good:" 
println cr.f

// WEIRD this is an error, because of the 'f' in the record!!!
fn: proc(r1: rt, r2: rt) {
  // weird, this isn't shadowing the global c...
  mycr=r1??r2
  if mycr != null {
    println "Should be good:" 
    println mycr.f
  } else {
    println "It's null" 
  }
}

fn(ar, ar)
fn(ar, br)
fn(br, ar)
fn(br, br)

