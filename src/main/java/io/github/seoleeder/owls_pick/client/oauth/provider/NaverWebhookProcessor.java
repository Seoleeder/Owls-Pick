package io.github.seoleeder.owls_pick.client.oauth.provider;

import io.github.seoleeder.owls_pick.global.response.CustomException;
import io.github.seoleeder.owls_pick.global.response.ErrorCode;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

@Component
public class NaverWebhookProcessor {
    // 복호화에 사용할 AES 알고리즘 설정 (CBC 블록 모드, PKCS5 패딩 적용)
    private static final String AES_ALGORITHM = "AES/CBC/PKCS5Padding";
    // 서명 검증(무결성 및 인증)에 사용할 HMAC 알고리즘 설정
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    /**
     * 네이버 웹훅 요청에 대해 서명 검증 및 providerId 복호화
     */
    public String process(String clientId, String encryptUniqueId, String timestamp, String signature, String clientSecret) throws Exception {
        // ClientSecret 값으로 비밀 키 생성
        byte[] keyBytes = generateKey(clientSecret);

        // 네이버가 보낸 요청이 위조되지 않았는지 서명 검증 수행
        if (!verifySignature(clientId, timestamp, keyBytes, signature)) {
            throw new CustomException(ErrorCode.INVALID_SIGNATURE);
        }

        // providerId 복호화 수행
        return decrypt(encryptUniqueId, keyBytes);
    }

    /**
     * 서명 검증과 복호화에 공통으로 사용될 16바이트 키 생성 메서드
     */
    private byte[] generateKey(String secret) throws Exception {
        // MD5 해싱 객체 생성
        MessageDigest md = MessageDigest.getInstance("MD5");
        // Secret 문자열을 바이트로 변환해 해싱 후, 16바이트 규격에 맞춤
        return Arrays.copyOfRange(md.digest(secret.getBytes(StandardCharsets.UTF_8)), 0, 16);
    }

    /**
     * 네이버가 보낸 서명(Signature)이 진짜인지 직접 만들어서 검증하는 메서드 (HMAC 연산)
     */
    private boolean verifySignature(String clientId, String timestamp, byte[] key, String signature) throws Exception {
        // 키 바이트 배열에 대해 HMAC용으로 래핑
        SecretKeySpec keySpec = new SecretKeySpec(key, HMAC_ALGORITHM);

        // HMAC-SHA256 연산을 수행할 객체 생성
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);

        // 객체에 키를 넣어서 초기 세팅
        mac.init(keySpec);

        // 해싱의 대상이 되는 평문 생성
        String message = clientId + timestamp;

        // 평문을 바이트로 변환하고 해싱 수행 -> 반환된 서명을 Base64 인코딩
        String calculated = Base64.getEncoder().encodeToString(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));

        // 직접 만들어낸 서명과 받은 서명이 일치하는지 확인
        return calculated.equals(signature);
    }

    /**
     * 네이버가 제공한 providerId 복호화(AES 연산)
     */
    private String decrypt(String encrypted, byte[] key) throws Exception {
        // 키 바이트 배열에 대해 AES용으로 래핑
        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");

        // AES 복호화를 수행할 객체 생성
        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);

        // DECRYPT_MODE 설정, keySpec과 첫 블록 연산용 초기화 벡터로 초기 세팅
        cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(key));

        // 암호화된 providerId 디코딩 -> 복호화 수행
        return new String(cipher.doFinal(Base64.getDecoder().decode(encrypted)), StandardCharsets.UTF_8);
    }
}
