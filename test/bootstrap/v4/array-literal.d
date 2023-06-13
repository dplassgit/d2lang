KW_PRINT=0
KW_IF=1
KW_ELSE=2
KW_ELIF=3
KW_PROC=4
KW_RETURN=5
KW_WHILE=6
KW_DO=7
KW_BREAK=8
KW_CONTINUE=9
KW_INT=10   // int keyword
KW_BOOL=11  // bool keyword
KW_STRING=12  // string keyword
KW_NULL=13
KW_INPUT=14
KW_LENGTH=15
KW_CHR=16
KW_ASC=17
KW_EXIT=18
KW_AND=19 // boolean and
KW_OR=20  // boolean or
KW_NOT=21 // boolean not
KW_RECORD=22
KW_NEW=23
KW_DELETE=24
KW_PRINTLN=25
KW_LONG=26
KW_BYTE=27
KW_DOUBLE=28
KW_ARGS=29
KW_EXTERN=30
KW_XOR=31

KEYWORDS:string[32]
KEYWORDS[KW_PRINT]="print"
KEYWORDS[KW_IF]="if"
KEYWORDS[KW_ELSE]="else"
KEYWORDS[KW_ELIF]="elif"
KEYWORDS[KW_PROC]="proc"
KEYWORDS[KW_RETURN]="return"
KEYWORDS[KW_WHILE]="while"
KEYWORDS[KW_DO]="do"
KEYWORDS[KW_BREAK]="break"
KEYWORDS[KW_CONTINUE]="continue"
KEYWORDS[KW_INT]="int"
KEYWORDS[KW_BOOL]="bool"
KEYWORDS[KW_STRING]="string"
KEYWORDS[KW_NULL]="null"
KEYWORDS[KW_INPUT]="input"
KEYWORDS[KW_LENGTH]="length"
KEYWORDS[KW_CHR]="chr"
KEYWORDS[KW_ASC]="asc"
KEYWORDS[KW_EXIT]="exit"
KEYWORDS[KW_AND]="and"
KEYWORDS[KW_OR]="or"
KEYWORDS[KW_NOT]="not"
KEYWORDS[KW_RECORD]="record"
KEYWORDS[KW_NEW]="new"
KEYWORDS[KW_DELETE]="delete"
KEYWORDS[KW_PRINTLN]="println"
KEYWORDS[KW_LONG]="long"
KEYWORDS[KW_BYTE]="byte"
KEYWORDS[KW_DOUBLE]="double"
KEYWORDS[KW_ARGS]="args"
KEYWORDS[KW_EXTERN]="extern"
KEYWORDS[KW_XOR]="xor"

KEYWORDS2=[
    "print",
    "if",
    "else",
    "elif",
    "proc",
    "return",
    "while",
    "do",
    "break",
    "continue",
    "int",
    "bool",
    "string",
    "null",
    "input",
    "length",
    "chr",
    "asc",
    "exit",
    "and",
    "or",
    "not",
    "record",
    "new",
    "delete",
    "println",
    "long",
    "byte",
    "double",
    "args",
    "extern",
    "xor"
    ]

lk = length(KEYWORDS)
print "lk " println lk
lk2 = length(KEYWORDS2)
print "lk2 " println lk2
i=0 while i < lk do i++ {
  if KEYWORDS[i] != KEYWORDS2[i] {
    print "entry " print i print " is wrong."
    exit
  }
}
