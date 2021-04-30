package com.karmanchik.chtotibtelegrambot.jpa;

import com.karmanchik.chtotibtelegrambot.entity.Group;
import com.karmanchik.chtotibtelegrambot.entity.Teacher;
import com.karmanchik.chtotibtelegrambot.entity.User;
import com.karmanchik.chtotibtelegrambot.model.IdTeacherName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public interface JpaTeacherRepository extends JpaRepository<Teacher, Integer> {
    Optional<Teacher> getByName(@NotNull String name);

    @Query(nativeQuery = true,
            value = "SELECT t.id, t.name FROM teacher t")
    List<IdTeacherName> getAllNames();

    @Query(nativeQuery = true,
            value = "SELECT t.id, t.name FROM teacher t " +
            "WHERE lower(t.name) LIKE '%' || lower(:name) || '%' " +
            "ORDER BY t.name")
    List<IdTeacherName> findAllByName(@Param("name") String name);

    @Query("SELECT g FROM Teacher g " +
            "WHERE :user member of g.users")
    Optional<Teacher> findByUsers(User user);
}
