package com.sorisonsoon.user.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sorisonsoon.user.domain.entity.User;
import com.sorisonsoon.user.dto.UserFormDto;
import com.sorisonsoon.user.service.MailService;
import com.sorisonsoon.user.service.Response;
import com.sorisonsoon.user.service.UserMailResponse;
import com.sorisonsoon.user.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @PostMapping("/new")
    public ResponseEntity<String> registerMember(@RequestBody @Valid UserFormDto userFormDto, BindingResult bindingResult) {
        logger.info("회원 가입 요청을 받았습니다.");

        if (bindingResult.hasErrors()) {
            logger.error("유효하지 않은 입력 데이터: {}", bindingResult.getAllErrors());
            return ResponseEntity.badRequest().body("입력 데이터가 올바르지 않습니다.");
        }

        if (!userService.isIdAvailable(userFormDto.getId())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("이미 사용 중인 아이디입니다.");
        }
        if (!userService.isEmailAvailable(userFormDto.getEmail())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("이미 사용 중인 이메일입니다.");
        }

        try {
            logger.info("받은 회원 데이터: {}", userFormDto);

            User user = User.createUser(userFormDto);
            user.setCreatedAt(LocalDateTime.now());
            user.setRefreshToken("");
            userService.saveUser(user);
            return ResponseEntity.ok("회원 가입이 완료되었습니다.");
        } catch (IllegalStateException e) {
            logger.error("회원 가입 오류: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.CONFLICT).body("회원 가입 오류: " + e.getMessage());
        } catch (Exception e) {
            logger.error("예상치 못한 오류: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("예상치 못한 오류가 발생했습니다.");
        }
    }

    @GetMapping("/check/id")
    public ResponseEntity<Map<String, Object>> checkIdAvailability(@RequestParam String id) {
        boolean available = userService.isIdAvailable(id);
        Map<String, Object> response = new HashMap<>();
        response.put("available", available);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/check/email")
    public ResponseEntity<Map<String, Object>> checkEmailAvailability(@RequestParam String email) {
        boolean available = userService.isEmailAvailable(email);
        Map<String, Object> response = new HashMap<>();
        response.put("available", available);
        return ResponseEntity.ok(response);
    }


    @PutMapping("/{email}")
    public ResponseEntity<String> updateMember(@PathVariable String email, @RequestBody @Valid UserFormDto userFormDto) {
        User user = userService.findByEmail(email);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        user.setPassword(passwordEncoder.encode(userFormDto.getPassword()));
        user.setNickname(userFormDto.getNickname());
        user.setProfileImage(userFormDto.getProfileImage());

        userService.saveUser(user);
        return ResponseEntity.ok("회원 정보가 업데이트되었습니다.");
    }

    @PostMapping("/mailConfirm")
    public ResponseEntity<Response> mailConfirm(@RequestBody Map<String, String> requestBody) {
        String email = requestBody.get("email");
        try {
            UserMailResponse userMailResponse = mailService.sendSimpleMessage(email);
            return ResponseEntity.ok(Response.success(userMailResponse));
        } catch (Exception e) {
            logger.error("메일 발송 오류: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Response.error("메일 발송에 실패했습니다."));
        }
    }

    @CrossOrigin(origins = "http://localhost:3000")
    @PostMapping("/verifyCode")
    public ResponseEntity<Response> verifyCode(@RequestBody Map<String, String> requestBody) {
        try {
            String code = requestBody.get("code");

            // 로그에 받은 인증 코드 출력
            logger.info("Received verification code: {}", code);

            // 인증 코드 검증
            boolean isCodeValid = mailService.ePw.equals(code);

            // 응답 데이터 생성
            Map<String, Object> response = new HashMap<>();
            response.put("valid", isCodeValid);

            // 검증 결과에 따라 성공 응답 또는 오류 응답 반환
            return ResponseEntity.ok(Response.success(response));
        } catch (Exception e) {
            // 오류 로그 및 오류 응답 반환
            logger.error("인증 코드 검증 오류: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Response.error("인증 코드 검증에 실패했습니다."));
        }
    }

    @DeleteMapping("/{email}/delete")
    public ResponseEntity<String> deleteMember(@PathVariable String email) {
        try {
            userService.deleteUser(email); // 이메일로 회원 삭제
            return ResponseEntity.ok("회원 탈퇴가 완료되었습니다.");
        } catch (IllegalArgumentException e) {
            logger.error("회원 탈퇴 오류: {}", e.getMessage(), e);
            return ResponseEntity.notFound().build(); // 회원이 존재하지 않을 경우
        } catch (Exception e) {
            logger.error("회원 탈퇴 오류: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("회원 탈퇴에 실패했습니다.");
        }
    }
}
