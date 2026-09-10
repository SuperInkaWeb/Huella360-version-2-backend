package com.vet_saas.modules.company.staff.dto;

public record StaffResponse(
        Long idStaff,
        Long idVeterinario,
        String nombres,
        String apellidos,
        String especialidad,
        String fotoPerfil,
        String rolInterno,
        boolean activo
) {}