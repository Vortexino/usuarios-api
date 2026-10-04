package com.uni.api.domain.exception;

public class MatriculaDuplicadaException extends RuntimeException {
    public MatriculaDuplicadaException(String matricula) {
        super("La matrícula " + matricula + " ya existe");
    }
}