# History

## Pre-history

From the time I took a compiler class in college in 1989, I wanted to write a compiler.

Here are some notes from the first iteration of D (the original name; it was a play on "C"
and because my name starts with D):

```
2/91 Mac Wrote in Think C: lex, RD [recursive-descent] parser with no attributes.
Worked for simple programs. No comparison ops in grammar. Precedence was wrong (didn't know it)

4/91 Realized I needed attributes. Rewrote parser to use attributes and
started type check. Thought about type inference as a cool feature

7/91? Computer crashed (Jove) and lost some of type checker
(backup was too old.)  Got sad & depressed and didn't do much.

1991: Started @ NYU. Wanted to write a demand virtual paging system.
Then an interpreter for Icode, then an optimizer, then a compiler. A cha!

12/91 Found yacc for Mac. Rewrote grammar in yacc. Started writing attributes.
Gave up.

.
... (did nothing between 1/92 and ~2/99)
.

1999 Moved to Win98/VC++. Rewrote lex, parser in C++ as class hierarchy for easy expansion and
modifyability. Token hierarchy. Compiler phase hierarchy. Designed for expandability and reuse.
New ideas:
        prototypes for procedures
        links (&allocate/deallocate)
        records for user-defined types
        bool a built-in type

4/99: Lex is 95% done. Parser is 90% done.
4/99: Type checker is ~90% done. No type inference yet. No support for records or links or arrays yet.
4/11/99: Procedure call typechecking done (no inferences yet)
4/18/99: Finished first cut of generic command-line processor
5/3/99: array definition & usage done (parser & type check)
Mid-99: Second child born. All work stopped
```

Nearly from the start, I envisioned a completely type-optional (type-inferred) language, which
would "just figure out" what the type of everything would be.

The syntax of D(1) was different than the final D2 syntax; here's a sample:

```
# comment
c: int;

proc foo() returns int
{
  i: int;
  i = 0;

  while i < 10
  {
    i++;
    if i < 7 then
      i += 2;
  }
  return i;
}

proc main(bar: int)
{
  c: int;
  c = bar + 3;  # 3 forces bar: int
  i = bar;    # forces i: int
}
```

I envisioned a pointer-like data type called a 'link' which was kind of like C++ references, but
smarter. This didn't make it into D2.

The VC++ version had no unit tests and I was constantly writing and running code samples to see
what the compiler would barf on.

In mid 1999, my 2nd kid was born and I stopped all work on it. FOR TWENTY YEARS.

## Recent history

Then in early 2021 I watched David Callahan's EXCELLENT YouTube series
[Make YOUR OWN Programming Language in Python](https://www.youtube.com/playlist?list=PLZQftyCk7_SdoVexSmwy_tBgs7P0b97yD)
in which he wrote a BASIC-like programming language interpreter from scratch in Python. It was
inspiring to watch something go from zero to a language in just 14 easy steps. I ate it up and
thought, "hey, I could totally do that for D!"

So in May 2021 I restarted D, as D2, but instead of VC++ or Python, I wrote it in Java. And instead
of no tests, I wrote unit and integration tests as I went along. Some of the concepts for the lexer
and parser I borrowed from David's videos.

As of today (Oct 2022), I have a working, optimizing compiler, with excellent test coverage and
tons of examples. The language and compiler are complete enough that it can be self-hosted; the
lexer-in-d is working and I started the parser.
