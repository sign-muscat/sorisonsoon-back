package com.sorisonsoon.user.dto;

import java.time.LocalDateTime;

import com.sorisonsoon.user.domain.type.UserProvider;
import com.sorisonsoon.user.domain.type.UserRole;
import com.sorisonsoon.user.domain.type.UserStatus;
import com.sorisonsoon.user.domain.type.UserType;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UserFormDto {
    private Long userId;

    private String id; //아이디
    private String password ; //비밀번호
    private String nickname ; //닉네임

    
    private String email ; //이메일

    private LocalDateTime createdAt; //가입일시
    private LocalDateTime deletedAt; //탈퇴일시

    private UserRole role; //권한

    private UserType type; //종류

    private UserStatus status; //상태

    private UserProvider provider;

    private String accessToken;
    private String refreshToken; //리프레시 토큰

    private String profileImage; //프로필 이미지
    
}
