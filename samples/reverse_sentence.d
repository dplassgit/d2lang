
reverse_word_order:proc(s:string):string {
  news = '' // heh
  word = ''
  i = 0 while i < length(s) do i = i + 1 {
    if s[i] == ' ' {
      // new word
      news = word + ' ' + news
      word = ''
    } else {
      word = word + s[i]
    }
  }
  news = word + ' ' + news
  return news
}

println reverse_word_order ( "--------- Ice and Fire ------------" )
println reverse_word_order ( "" )
println reverse_word_order ( "fire, in end will world the say Some" )
println reverse_word_order ( "ice. in say Some" )
println reverse_word_order ( "desire of tasted I've what From" )
println reverse_word_order ( "fire. favor who those with hold I" )
println reverse_word_order ( "" )
println reverse_word_order ( "... elided paragraph last ..." )
println reverse_word_order ( "" )
println reverse_word_order ( "Frost Robert -----------------------" )
