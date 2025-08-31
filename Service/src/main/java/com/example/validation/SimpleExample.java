package com.example.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

// Resposta simplificada do Go
class ValidationResult {
    private boolean valid;
    private String message;
    private List<String> errors;

    public ValidationResult() {
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }
}

// Serviço simplificado que chama Go
class UserValidationService {
    private final ObjectMapper objectMapper;
    private final String goExecutablePath;
    private final long timeoutMs;

    public UserValidationService() {
        this("./validator", 5000);
    }

    public UserValidationService(String goExecutablePath, long timeoutMs) {
        this.objectMapper = new ObjectMapper();
        this.goExecutablePath = goExecutablePath;
        this.timeoutMs = timeoutMs;
    }

    public ValidationResult validateUser(String userJson) {
        try {
            ProcessBuilder pb = new ProcessBuilder(goExecutablePath, "-mode", "single", "-input", userJson);
            pb.redirectErrorStream(true);

            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }

            boolean finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                return createErrorResult("Timeout na validação");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                return createErrorResult("Erro na execução: " + output.toString());
            }

            return objectMapper.readValue(output.toString(), ValidationResult.class);

        } catch (Exception e) {
            return createErrorResult("Erro interno: " + e.getMessage());
        }
    }

    public ValidationResult validateUser(Map<String, Object> userData) {
        try {
            String userJson = objectMapper.writeValueAsString(userData);
            return validateUser(userJson);
        } catch (JsonProcessingException e) {
            return createErrorResult("Erro ao converter dados para JSON: " + e.getMessage());
        }
    }

    private ValidationResult createErrorResult(String message) {
        ValidationResult result = new ValidationResult();
        result.setValid(false);
        result.setMessage(message);
        return result;
    }
}

public class SimpleExample {
    public static void main(String[] args) {
        System.out.println("=== Teste de Validação de Usuário ===");

        UserValidationService validationService = new UserValidationService();

        // Teste 1: Usuário válido
        System.out.println("\n--- Teste 1: Usuário Válido ---");
        Map<String, Object> validUser = new HashMap<>();
        validUser.put("name", "João Silva");
        validUser.put("email", "raywall@usp.br");
        validUser.put("idade", 25);

        ValidationResult result1 = validationService.validateUser(validUser);
        printResult("Usuário Válido", result1);

        // Teste 2: Usuário inválido (menor de idade)
        System.out.println("\n--- Teste 2: Usuário Menor de Idade ---");
        Map<String, Object> minorUser = new HashMap<>();
        minorUser.put("name", "Ana");
        minorUser.put("email", "ana@usp.br");
        minorUser.put("idade", 16);

        ValidationResult result2 = validationService.validateUser(minorUser);
        printResult("Usuário Menor", result2);

        // Teste 3: JSON direto (igual ao seu exemplo)
        System.out.println("\n--- Teste 3: JSON Direto ---");
        String jsonRequest = "{\"name\":\"nome\",\"email\":\"raywall@usp.br\",\"idade\":18}";

        ValidationResult result3 = validationService.validateUser(jsonRequest);
        printResult("JSON Direto", result3);
    }

    private static void printResult(String testName, ValidationResult result) {
        System.out.println(testName + ":");
        System.out.println("Válido: " + result.isValid());

        if (result.getMessage() != null && !result.getMessage().isEmpty()) {
            System.out.println("Mensagem: " + result.getMessage());
        }

        if (result.getErrors() != null && !result.getErrors().isEmpty()) {
            System.out.println("Erros:");
            for (String error : result.getErrors()) {
                System.out.println("     - " + error);
            }
        }
    }
}