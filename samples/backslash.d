a="a\nb\'\"\rx\t\\"

println "Should be a, newline, b, tick, quote, carriage return, x, tab, backslash:"
println a

println "Each character:"
i = 0 while i < length(a) do i = i + 1 {
  println asc(a[i])
}
