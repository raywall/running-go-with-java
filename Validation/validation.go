package main

import (
	"encoding/json"
	"flag"
	"fmt"
	"os"
	"regexp"
	"strings"
)

// Struct que representa os dados da requisição
type UserRequest struct {
	Name  string `json:"name"`
	Email string `json:"email"`
	Idade int    `json:"idade"`
}

// Resposta de validação simplificada
type ValidationResult struct {
	Valid   bool     `json:"valid"`
	Message string   `json:"message,omitempty"`
	Errors  []string `json:"errors,omitempty"`
}

// Validadores específicos
func validateName(name string) (bool, string) {
	name = strings.TrimSpace(name)
	if name == "" {
		return false, "Nome é obrigatório"
	}
	if len(name) < 2 {
		return false, "Nome deve ter pelo menos 2 caracteres"
	}
	return true, ""
}

func validateEmail(email string) (bool, string) {
	email = strings.TrimSpace(email)
	if email == "" {
		return false, "E-mail é obrigatório"
	}

	emailRegex := regexp.MustCompile(`^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$`)
	if !emailRegex.MatchString(email) {
		return false, "E-mail deve ter formato válido"
	}
	return true, ""
}

func validateIdade(idade int) (bool, string) {
	if idade < 18 {
		return false, "Idade deve ser maior ou igual a 18 anos"
	}
	if idade > 120 {
		return false, "Idade deve ser realista (máximo 120 anos)"
	}
	return true, ""
}

// Função principal de validação
func validateUserRequest(user UserRequest) ValidationResult {
	var errors []string

	// Validar nome
	if valid, msg := validateName(user.Name); !valid {
		errors = append(errors, msg)
	}

	// Validar email
	if valid, msg := validateEmail(user.Email); !valid {
		errors = append(errors, msg)
	}

	// Validar idade
	if valid, msg := validateIdade(user.Idade); !valid {
		errors = append(errors, msg)
	}

	// Retornar resultado
	if len(errors) == 0 {
		return ValidationResult{
			Valid:   true,
			Message: "Usuário válido",
		}
	}

	return ValidationResult{
		Valid:  false,
		Errors: errors,
	}
}

func main() {
	var mode = flag.String("mode", "stdin", "Modo: stdin ou single")
	var input = flag.String("input", "", "JSON input para modo single")
	flag.Parse()

	switch *mode {
	case "stdin":
		// Lê JSON do stdin
		var inputJson string
		_, _ = fmt.Scanln(&inputJson)
		processJson(inputJson)

	case "single":
		// Processa JSON passado como parâmetro
		if *input == "" {
			fmt.Fprintf(os.Stderr, "Input JSON é obrigatório no modo single\n")
			os.Exit(1)
		}
		processJson(*input)

	default:
		fmt.Fprintf(os.Stderr, "Modo inválido: %s\n", *mode)
		os.Exit(1)
	}
}

func processJson(jsonInput string) {
	var user UserRequest

	// Parse do JSON
	if err := json.Unmarshal([]byte(jsonInput), &user); err != nil {
		result := ValidationResult{
			Valid:   false,
			Message: "JSON inválido: " + err.Error(),
		}
		output, _ := json.Marshal(result)
		fmt.Println(string(output))
		os.Exit(1)
	}

	// Validar dados
	result := validateUserRequest(user)

	// Retornar resultado como JSON
	output, _ := json.Marshal(result)
	fmt.Println(string(output))
}
