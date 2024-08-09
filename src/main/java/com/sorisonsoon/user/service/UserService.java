package com.sorisonsoon.user.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sorisonsoon.user.domain.entity.User;
import com.sorisonsoon.user.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("존재 하지 않는 사용자입니다: " + email));

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .roles(user.getRole().name()) // 회원 권한을 role로 설정
                .build();
    }

    public User saveUser(User user) {
        validateDuplicateUser(user);
        return userRepository.save(user);
    }

    private void validateDuplicateUser(User user) {
        userRepository.findByEmail(user.getEmail())
                .ifPresent(existingUser -> {
                    throw new IllegalStateException("이미 가입된 이메일입니다.");
                });
                userRepository.findById(user.getId())
                .ifPresent(existingUser -> {
                    throw new IllegalStateException("이미 가입된 아이디입니다.");
                });
    }

    public boolean isIdAvailable(String id) {
        return userRepository.findById(id).isEmpty();
    }

    public boolean isEmailAvailable(String email) {
        return userRepository.findByEmail(email).isEmpty();
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));
    }

    @Transactional
    public void deleteUser(String email) {
        User user = findByEmail(email); // findByEmail에서 예외를 처리합니다.
        userRepository.delete(user);
    }
}
