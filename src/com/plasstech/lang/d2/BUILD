java_library(name="compiler",
	     srcs=glob(["**/*.java"], exclude=["*.java"]),
	     deps=[
           "@maven//:com_google_guava_guava",
       ],
	     visibility=["//visibility:public"],
)

java_binary(name="LexerDriver",
            srcs=["LexerDriver.java"],
            deps=[":compiler"],
            )

java_binary(name="ParserDriver",
            srcs=["ParserDriver.java"],
            deps=[":compiler"],
            )

java_binary(name="CheckerDriver",
            srcs=["CheckerDriver.java"],
            deps=[":compiler"],
            )

java_binary(name="CodeGenDriver",
            srcs=["CodeGenDriver.java"],
            deps=[":compiler"],
            )

java_binary(name="InterpreterDriver",
            srcs=["InterpreterDriver.java"],
            deps=[":compiler",
	                "@maven//:com_google_guava_guava",
            ],
            )
