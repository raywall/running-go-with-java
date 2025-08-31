.PHONY: build run

build:
	@cd Validation && CGO_ENABLED=1 go build -o ../Service/validator validation.go
	@cd Service && mvn clean compile

test: build
	@cd Service && ./validator -mode single -input '{"name":"João","email":"joao@usp.br","idade":25}'

run: build
	@cd Service && mvn exec:java -Dexec.mainClass="com.example.validation.SimpleExample"