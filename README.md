# running-go-with-java

## 1. **Usando CGO e JNI (Java Native Interface)**

1. Crie uma biblioteca em Go que exporta funções C
1. Compile para uma biblioteca compartilhada (.so, .dll, .dylib)
1. Use JNI para chamar essas funções em Java

**Exemplo em Go:**

```go
package main

import "C"

//export add
func add(a, b int) int {
    return a + b
}

//export hello
func hello(name *C.char) *C.char {
    goName := C.GoString(name)
    result := "Hello, " + goName + "!"
    return C.CString(result)
}

func main() {} // necessário mas não usado
```

Compile com: `go build -buildmode=c-shared -o libexample.so`

## 2. **Usando JNA (Java Native Access)**

O JNA é mais simples que JNI puro. Você pode carregar a biblioteca Go diretamente:

```java
import com.sun.jna.Library;
import com.sun.jna.Native;

public interface GoLibrary extends Library {
    GoLibrary INSTANCE = Native.load("example", GoLibrary.class);
    
    int add(int a, int b);
    String hello(String name);
}
```

## 3. **Através de Comunicação HTTP/REST**

Uma alternativa mais moderna é criar um microserviço em Go:

```go
package main

import (
    "encoding/json"
    "net/http"
)

func addHandler(w http.ResponseWriter, r *http.Request) {
    // Lógica da biblioteca
    result := map[string]interface{}{
        "result": 42,
    }
    json.NewEncoder(w).Encode(result)
}

func main() {
    http.HandleFunc("/add", addHandler)
    http.ListenAndServe(":8080", nil)
}
```

E consumir do Java usando HTTP clients.

## 4. **Usando gRPC**

Para uma solução mais robusta, você pode usar gRPC:

1. Define um arquivo `.proto`
1. Gera código para Go e Java
1. Implementa o serviço em Go
1. Consome do Java usando o cliente gerado

## Recomendações:

- **Para bibliotecas simples**: Use CGO + JNA
- **Para serviços**: Use HTTP REST ou gRPC
- **Para alta performance**: Use JNI direto
- **Para facilidade de deployment**: Use HTTP/gRPC

A escolha depende dos seus requisitos específicos de performance, complexidade e arquitetura da aplicação.​​​​​​​​​​​​​​​​
