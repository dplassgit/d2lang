generic: record<S, T> {
  first: S
  second: T
}


specific = new generic<int, string>
specific.first = 3
specific.second = 'hi'

      
