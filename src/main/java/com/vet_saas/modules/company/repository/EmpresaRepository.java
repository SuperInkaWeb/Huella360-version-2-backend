package com.vet_saas.modules.company.repository;

import com.vet_saas.modules.company.model.Empresa;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmpresaRepository extends JpaRepository<Empresa, Long> {

    boolean existsByRuc(String ruc);

    boolean existsByUsuarioPropietarioId(Long usuarioId);

    Optional<Empresa> findByUsuarioPropietarioId(Long usuarioId);

    org.springframework.data.domain.Page<Empresa> findByEstadoValidacion(
            com.vet_saas.modules.veterinarian.model.VerificationStatus estadoValidacion,
            org.springframework.data.domain.Pageable pageable);

    // Busqueda del panel admin (nombre, RUC, correo de contacto o del propietario).
    // El estado se pasa como parametro: es un enum nativo de Postgres y un literal en JPQL falla.
    @Query(value = """
            SELECT e FROM Empresa e LEFT JOIN e.usuarioPropietario p
            WHERE (LOWER(e.nombreComercial) LIKE LOWER(CONCAT('%', :q, '%'))
                OR e.ruc LIKE CONCAT('%', :q, '%')
                OR LOWER(e.emailContacto) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(p.correo) LIKE LOWER(CONCAT('%', :q, '%')))
            """, countQuery = """
            SELECT COUNT(e) FROM Empresa e LEFT JOIN e.usuarioPropietario p
            WHERE (LOWER(e.nombreComercial) LIKE LOWER(CONCAT('%', :q, '%'))
                OR e.ruc LIKE CONCAT('%', :q, '%')
                OR LOWER(e.emailContacto) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(p.correo) LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    Page<Empresa> buscarParaAdmin(@Param("q") String q, Pageable pageable);

    @Query(value = """
            SELECT e FROM Empresa e LEFT JOIN e.usuarioPropietario p
            WHERE e.estadoValidacion = :estado AND (LOWER(e.nombreComercial) LIKE LOWER(CONCAT('%', :q, '%'))
                OR e.ruc LIKE CONCAT('%', :q, '%')
                OR LOWER(e.emailContacto) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(p.correo) LIKE LOWER(CONCAT('%', :q, '%')))
            """, countQuery = """
            SELECT COUNT(e) FROM Empresa e LEFT JOIN e.usuarioPropietario p
            WHERE e.estadoValidacion = :estado AND (LOWER(e.nombreComercial) LIKE LOWER(CONCAT('%', :q, '%'))
                OR e.ruc LIKE CONCAT('%', :q, '%')
                OR LOWER(e.emailContacto) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(p.correo) LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    Page<Empresa> buscarParaAdminPorEstado(@Param("q") String q,
            @Param("estado") com.vet_saas.modules.veterinarian.model.VerificationStatus estado,
            Pageable pageable);
}