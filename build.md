# Instruções de Build e Execução

## Pré-requisitos

- Go 1.19+
- Java 11+
- Maven 3.6+
- GCC (para versão CGO) - OPCIONAL

## Duas Abordagens Disponíveis

### Abordagem 1: CGO + JNA (Mais performática, mais complexa)

### Abordagem 2: Processo Separado (Mais simples, ligeiramente mais lenta)

## ABORDAGEM 2: Processo Separado (RECOMENDADA)

### 1. Compilar o executável Go

```bash
go build -o validator validation.go
```

### 2. Testar o executável

```bash
# Teste direto
./validator -mode single -input '{"data":{"email":"test@test.com"},"rules":[{"field":"email","type":"email","message":"Email inválido"}]}'
```

### 3. Usar no Java

Use a classe `ProcessValidationService` que chama o executável Go como processo separado.

**Vantagens:**

- ✅ Não precisa de CGO
- ✅ Fácil de debuggar
- ✅ Isolamento completo entre Java e Go
- ✅ Fácil deployment
- ✅ Cross-platform sem compilação específica

**Desvantagens:**

- ⚠️ Overhead de processo (poucos ms)
- ⚠️ Precisa gerenciar executável Go

---

## ABORDAGEM 1: CGO + JNA (Opcional)

### Resolver problemas de CGO

#### 1. Verificar se CGO está funcionando:

```bash
# Teste básico de CGO
cat > test_cgo.go << 'EOF'
package main

import "C"
import "fmt"

func main() {
    fmt.Println("CGO OK!")
}
EOF

go run test_cgo.go
```

#### 2. Se der erro "C compiler not found":

**Ubuntu/Debian:**

```bash
sudo apt update
sudo apt install build-essential
```

**CentOS/RHEL:**

```bash
sudo dnf groupinstall "Development Tools"
```

**macOS:**

```bash
xcode-select --install
```

**Windows:**

- Instale MinGW-w64 ou Visual Studio Build Tools
- Adicione ao PATH

#### 3. Verificar variáveis de ambiente:

```bash
go env CGO_ENABLED  # deve retornar "1"
go env CC          # deve mostrar o compilador C
```

#### 4. Se CGO_ENABLED = 0:

```bash
export CGO_ENABLED=1
# ou no Windows:
set CGO_ENABLED=1
```

## Configurando o Projeto Maven

### 1. `pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                             http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>java-go-validation</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>

    <properties>
        <maven.compiler.source>11</maven.compiler.source>
        <maven.compiler.target>11</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencies>
        <!-- JNA para integração com Go -->
        <dependency>
            <groupId>net.java.dev.jna</groupId>
            <artifactId>jna</artifactId>
            <version>5.13.0</version>
        </dependency>

        <!-- Jackson para JSON -->
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
            <version>2.15.2</version>
        </dependency>

        <!-- JUnit para testes -->
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>5.9.3</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
            </plugin>

            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.1.2</version>
            </plugin>

            <!-- Plugin para copiar biblioteca Go -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-resources-plugin</artifactId>
                <version>3.3.1</version>
                <executions>
                    <execution>
                        <id>copy-native-libs</id>
                        <phase>process-resources</phase>
                        <goals>
                            <goal>copy-resources</goal>
                        </goals>
                        <configuration>
                            <outputDirectory>${project.build.directory}/classes</outputDirectory>
                            <resources>
                                <resource>
                                    <directory>native</directory>
                                    <includes>
                                        <include>**/*.so</include>
                                        <include>**/*.dll</include>
                                        <include>**/*.dylib</include>
                                    </includes>
                                </resource>
                            </resources>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

### 2. Estrutura do projeto

```
projeto/
├── pom.xml
├── src/
│   └── main/
│       └── java/
│           └── com/
│               └── example/
│                   └── validation/
│                       ├── ValidationService.java
│                       └── ExampleUsage.java
├── native/
│   ├── libvalidation.so    (Linux/macOS)
│   └── validation.dll      (Windows)
├── validation.go
└── README.md
```

## Executando

### 1. Compilar o projeto Java

```bash
mvn clean compile
```

### 2. Executar o exemplo

```bash
mvn exec:java -Dexec.mainClass="com.example.validation.ExampleUsage"
```

### 3. Configurar o `java.library.path` (se necessário)

Se a JVM não encontrar a biblioteca:

```bash
java -Djava.library.path=./native -cp target/classes com.example.validation.ExampleUsage
```

## Teste de Viabilidade

### 1. Teste de Performance

Crie um teste para medir latência:

```java
long start = System.nanoTime();
ValidationResponse result = validationService.validateData(data, rules);
long end = System.nanoTime();
System.out.println("Tempo: " + (end - start) / 1_000_000.0 + " ms");
```

### 2. Teste de Concorrência

```java
// Teste com múltiplas threads
ExecutorService executor = Executors.newFixedThreadPool(10);
for (int i = 0; i < 1000; i++) {
    executor.submit(() -> {
        ValidationResponse result = validationService.validateData(data, rules);
        // verificar resultado
    });
}
```

### 3. Teste de Memória

Use ferramentas como JProfiler ou VisualVM para monitorar:

- Consumo de memória
- Garbage collection
- Vazamentos de memória

## Estratégia de Migração Gradual

### Fase 1: Validações Simples

- Migre validações básicas (required, email, regex)
- Mantenha validações complexas em Java
- Compare performance

### Fase 2: Validações de Negócio

- Migre regras de negócio específicas
- Implemente cache se necessário
- Monitore estabilidade

### Fase 3: Funcionalidades Completas

- Migre módulos inteiros
- Considere comunicação via HTTP/gRPC para maior isolamento
- Planeje rollback se necessário

## Troubleshooting

### Erro "Library not found"

- Verifique se a biblioteca está no `java.library.path`
- No Linux: `export LD_LIBRARY_PATH=./native:$LD_LIBRARY_PATH`
- No macOS: `export DYLD_LIBRARY_PATH=./native:$DYLD_LIBRARY_PATH`

### Erro de linking

- Instale GCC completo: `apt install build-essential` (Ubuntu)
- No macOS: `xcode-select --install`

### Performance Issues

- Use connection pooling se implementar via HTTP
- Considere batch processing para múltiplas validações
- Profile memory usage regularmente
