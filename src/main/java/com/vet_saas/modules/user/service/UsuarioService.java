package com.vet_saas.modules.user.service;

import com.vet_saas.core.exceptions.types.BusinessException;
import com.vet_saas.core.exceptions.types.ForbiddenException;
import com.vet_saas.core.exceptions.types.ResourceNotFoundException;
import com.vet_saas.modules.user.dto.UserMeResponse;
import com.vet_saas.modules.user.model.Role;
import com.vet_saas.modules.user.model.Usuario;
import com.vet_saas.modules.user.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public Usuario findByCorreo(String correo) {
        return usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con correo: " + correo));
    }

    public Usuario findById(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con ID: " + id));
    }

    @Transactional
    public UserMeResponse updateMyRole(Usuario usuario, Role nuevoRol) {
        if (nuevoRol == Role.ADMIN) {
            throw new ForbiddenException("No puedes asignarte el rol ADMIN");
        }

        if (usuario.getRol() != null && usuario.getRol() != Role.CLIENTE) {
            throw new BusinessException("Tu rol ya está definido como " + usuario.getRol().name()
                    + " y no puede ser cambiado desde aquí");
        }

        usuario.setRol(nuevoRol);
        usuarioRepository.save(usuario);

        return new UserMeResponse(usuario.getId(), usuario.getCorreo(), nuevoRol);
    }
}
