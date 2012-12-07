PROJECT=hw4
CFLAGS= -Wall

all: problem2
debug: client-debug server-debug

problem2:
	javac problem2.java

server: 
	javac server.java

clean:
	rm -f ./*.class

clean-all: clean
