package com.sorisonsoon.user.service;

import java.io.UnsupportedEncodingException;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.messaging.MessagingException;
import org.springframework.stereotype.Service;

import jakarta.mail.Message;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.SneakyThrows;



@Service
public class MailService {
    @Autowired
    JavaMailSender emailsender; // Bean 등록해둔 MailConfig 를 emailsender 라는 이름으로 autowired

    public static final String ePw =createKey(); // 인증번호

    // 메일 내용 작성

    @SneakyThrows
    public MimeMessage createMessage(String to) throws MessagingException, UnsupportedEncodingException {
        MimeMessage message = emailsender.createMimeMessage();

        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to)); // 받는 사람 설정
        message.setSubject("소리손순 회원가입 이메일 인증"); // 제목 설정

        String msgg = "";
        msgg += "<div style='margin:100px;'>";
        msgg += "<h1> 안녕하세요. 소리손순 입니다</h1>";
        msgg += "CODE : <strong>";
        msgg += ePw + "</strong><div><br/> ";
        msgg += "</div>";
        message.setText(msgg, "utf-8", "html");// 내용, charset 타입, subtype
        // 보내는 사람의 이메일 주소, 보내는 사람 이름
        message.setFrom(new InternetAddress("4ignmuscat@gmail.com", "소리손순"));// 보내는 사람

        return message;
    }

    // 랜덤 인증 코드 생성
    public static String createKey() {
        StringBuffer key = new StringBuffer();
        Random rnd = new Random();

        for (int i = 0; i < 8; i++) { // 인증코드 8자리
            int index = rnd.nextInt(3); // 0~2 까지 랜덤, rnd 값에 따라서 아래 switch 문이 실행됨

            switch (index) {
                case 0:
                    key.append((char) ((int) (rnd.nextInt(26)) + 97));
                    // a~z (ex. 1+97=98 => (char)98 = 'b')
                    break;
                case 1:
                    key.append((char) ((int) (rnd.nextInt(26)) + 65));
                    // A~Z
                    break;
                case 2:
                    key.append((rnd.nextInt(10)));
                    // 0~9
                    break;
            }
        }

        return key.toString();
    }

    // 메일 발송
    // sendSimpleMessage 의 매개변수로 들어온 to 는 곧 이메일 주소가 되고,
    // MimeMessage 객체 안에 내가 전송할 메일의 내용을 담는다.
    // 그리고 bean 으로 등록해둔 javaMail 객체를 사용해서 이메일 send!!

    public UserMailResponse sendSimpleMessage(String to) throws Exception {
        // 랜덤 인증번호 생성
        MimeMessage message = createMessage(to); // 메일 발송
        try {// 예외처리
            emailsender.send(message);
        } catch (MailException es) {
            es.printStackTrace();
            throw new IllegalArgumentException();
        }

        return new UserMailResponse(ePw); // 메일로 보냈던 인증 코드를 서버로 반환
    }
}
