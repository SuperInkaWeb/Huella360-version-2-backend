package com.vet_saas.security.jwt;

import org.springframework.security.authentication.AccountStatusException;

/** Identidad de Auth0 que no demostro ser duena del correo de una cuenta existente (H360-SEC-002). */
public class CorreoNoVerificadoException extends AccountStatusException {

    public CorreoNoVerificadoException(String msg) {
        super(msg);
    }
}
