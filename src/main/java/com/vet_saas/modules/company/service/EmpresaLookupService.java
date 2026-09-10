package com.vet_saas.modules.company.service;

import com.vet_saas.core.exceptions.types.ResourceNotFoundException;
import com.vet_saas.modules.company.model.Empresa;
import com.vet_saas.modules.company.repository.EmpresaRepository;
import com.vet_saas.modules.user.model.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EmpresaLookupService {

    private final EmpresaRepository empresaRepository;

    @Cacheable(value = "empresasByPropietario", key = "#usuario.id")
    @Transactional(readOnly = true)
    public Empresa getEmpresaFromUsuario(Usuario usuario) {
        return empresaRepository.findByUsuarioPropietarioId(usuario.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Empresa", "propietarioId", usuario.getId()));
    }

    @Cacheable(value = "empresasByPropietario", key = "#usuarioId")
    @Transactional(readOnly = true)
    public Optional<Empresa> getEmpresaByUsuarioId(Long usuarioId) {
        return empresaRepository.findByUsuarioPropietarioId(usuarioId);
    }

    @Cacheable(value = "empresasById", key = "#empresaId")
    @Transactional(readOnly = true)
    public Empresa getEmpresaById(Long empresaId) {
        return empresaRepository.findById(empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa", "id", empresaId));
    }
}
