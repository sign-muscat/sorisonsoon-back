package com.sorisonsoon.user.domain.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.SQLDelete;
import org.springframework.data.annotation.CreatedDate;

import com.sorisonsoon.user.domain.type.UserProvider;
import com.sorisonsoon.user.domain.type.UserRole;
import com.sorisonsoon.user.domain.type.UserStatus;
import com.sorisonsoon.user.domain.type.UserType;
import com.sorisonsoon.user.dto.UserFormDto;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@AllArgsConstructor
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE  users SET status = 'WITHDRAW' , deleted_at = CURRENT_TIMESTAMP WHERE user_id = ? ")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(unique = true, name = "id")
    private String id;
    private String password;
    private String nickname;

    @Column(unique = true)
    private String email;

    @CreatedDate
    private LocalDateTime createdAt;
    private LocalDateTime deletedAt;

    @Enumerated(EnumType.STRING)
    private UserRole role = UserRole.FREE_USER;

    @Enumerated(EnumType.STRING)
    private UserType type;

    @Enumerated(EnumType.STRING)
    private UserStatus status = UserStatus.ACTIVATE;

    private String accessToken;
    private String refreshToken;

    @Enumerated(EnumType.STRING)
    private UserProvider provider;

    private String profileImage;

    public static User createUser(UserFormDto userFormDto) {
        User user = new User();
        
        user.setId(userFormDto.getId());
        user.setPassword(userFormDto.getPassword());
        user.setNickname(userFormDto.getNickname());
        user.setEmail(userFormDto.getEmail());
        user.setCreatedAt(userFormDto.getCreatedAt());
        user.setDeletedAt(userFormDto.getDeletedAt());
        if (userFormDto.getRole() != null) {
            user.setRole(userFormDto.getRole()); 
        } 
        user.setType(userFormDto.getType()); 
        if (userFormDto.getStatus() != null) {
            user.setStatus(userFormDto.getStatus()); 
        }

        user.setRefreshToken(userFormDto.getRefreshToken()); 
        user.setProfileImage(userFormDto.getProfileImage()); 
        
        // Determine provider based on email
        user.setProvider(determineProvider(userFormDto.getEmail()));
    
        return user;
    }
    
    private static UserProvider determineProvider(String email) {
        if (email.endsWith("@gmail.com")) {
            return UserProvider.GOOGLE;
        } else {
            return UserProvider.NONE;
        }
    }
}
