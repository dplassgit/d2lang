
java_library(name="lexer",
	     srcs=["Lexer.java", "ScannerException.java",],
	     deps=[":tokens"],
	     visibility=["//:__subpackages__"],
	     )

java_library(name="tokens",
	     srcs=[
                   "BoolToken.java",
                   "KeywordToken.java",
                   "IntToken.java", 
                   "Token.java", 
	     ],
	     deps=["//src/com/plasstech/lang/d2/common:position",
	     ],
	     visibility=["//:__subpackages__"],
	     )
