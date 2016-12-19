package com.minion.persistence.dao;

import com.qanairy.persistence.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByEmail(String email);

    void delete(User user);

}
