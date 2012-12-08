all: problem2

problem2:
	javac problem2.java

server: 
	javac Server.java

clean:
	rm -f ./*.class

clean-all: clean
