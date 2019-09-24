build:
	javac src/com/github/cloudkj/*.java

test: build
	java -ea -cp src/ com.github.cloudkj.PriorityExpiryCacheTest

clean:
	rm -rf src/com/github/cloudkj/*.class
