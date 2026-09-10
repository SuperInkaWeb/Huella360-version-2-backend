package com.vet_saas.modules.catalog.repository;

import com.vet_saas.modules.catalog.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    Optional<Categoria> findByIdAndActivoTrue(Long id);

    List<Categoria> findByActivoTrueOrderByOrdenAsc();

    List<Categoria> findByPadreIsNullAndActivoTrueOrderByOrdenAsc();

    List<Categoria> findByPadreIdAndActivoTrueOrderByOrdenAsc(Long padreId);
}