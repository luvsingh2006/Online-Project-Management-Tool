package com.pmtool.dao;

import com.pmtool.model.Role;
import com.pmtool.model.User;
import com.pmtool.model.UserStatus;

import java.util.List;
import java.util.Optional;

public interface UserDao extends GenericDao<User, Long> {
    Optional<User> findByEmail(String email);
    List<User> findByRole(Role role);
    List<User> findActiveUsers();
    boolean updatePassword(Long userId, String passwordHash, String salt);
    boolean updateStatus(Long userId, UserStatus status);
    List<User> searchUsers(String query);
}
