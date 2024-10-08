package ott.j4jg_gateway.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ott.j4jg_gateway.model.dto.CustomOAuth2User;
import ott.j4jg_gateway.model.dto.GoogleResponse;
import ott.j4jg_gateway.model.dto.KakaoResponse;
import ott.j4jg_gateway.model.dto.OAuth2Response;
import ott.j4jg_gateway.model.entity.User;
import ott.j4jg_gateway.model.entity.UserAddInfo;
import ott.j4jg_gateway.model.enums.USERROLE;
import ott.j4jg_gateway.repository.UserRepository;
import ott.j4jg_gateway.repository.UserAddInfoRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Random;

@Service
@Transactional
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private static final Logger logger = LoggerFactory.getLogger(CustomOAuth2UserService.class);

    private final UserRepository userRepository;
    private final UserAddInfoRepository userAddInfoRepository;

    public CustomOAuth2UserService(UserRepository userRepository, UserAddInfoRepository userAddInfoRepository) {
        this.userRepository = userRepository;
        this.userAddInfoRepository = userAddInfoRepository;
    }

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        logger.info("OAuth2 사용자 요청 로드: {}", userRequest);

        OAuth2User oAuth2User = super.loadUser(userRequest);
        logger.info("OAuth2 사용자 세부 정보: {}", oAuth2User.getAttributes());

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        logger.info("제공자 등록 ID: {}", registrationId);

        OAuth2Response oAuth2Response = getOAuth2Response(oAuth2User, registrationId);
        if (oAuth2Response == null) {
            logger.error("지원하지 않는 OAuth2 제공자: {}", registrationId);
            throw new OAuth2AuthenticationException("지원하지 않는 OAuth2 제공자: " + registrationId);
        }

        User user;
        try {
            user = saveOrUpdateUser(oAuth2Response);
            logger.info("저장/업데이트된 사용자 정보: {}", user);
        } catch (Exception e) {
            logger.error("사용자 저장 또는 업데이트 중 오류 발생: ", e);
            throw new RuntimeException("사용자 저장 실패", e);
        }

        Collection<GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority(user.getRole().name())
        );

        CustomOAuth2User customOAuth2User = new CustomOAuth2User(
                authorities,
                oAuth2User.getAttributes(),
                oAuth2Response.getProvider(),
                user.getUserId(),
                oAuth2Response.getEmail(),
                oAuth2Response.getPhoneNumber(),
                getUserIdAttributeName(registrationId)
        );

        logger.info("Custom OAuth2 사용자: {}", customOAuth2User);

        return customOAuth2User;
    }

    public User saveOrUpdateUser(OAuth2Response oAuth2Response) {
        logger.info("saveOrUpdateUser 메서드 시작");

        Optional<User> optionalUser = userRepository.findByUserEmailAndProvider(oAuth2Response.getEmail(), oAuth2Response.getProvider());

        User user = optionalUser.map(existingUser -> {
            existingUser.setUserPhoneNumber(oAuth2Response.getPhoneNumber() != null ? oAuth2Response.getPhoneNumber() : "");
            existingUser.setUpdatedAt(LocalDateTime.now());
            logger.info("기존 사용자 업데이트: {}", existingUser);
            return existingUser;
        }).orElseGet(() -> {
            // 새로운 사용자를 추가할 때 user_id가 이미 존재하는 경우를 방지
            Optional<User> userById = userRepository.findById(oAuth2Response.getProviderId());
            if (userById.isPresent()) {
                // 이미 존재하는 user_id라면 기존 사용자의 정보를 업데이트하도록 함
                logger.info("기존 사용자 정보로 업데이트: {}", userById.get());
                User existingUser = userById.get();
                existingUser.setUserEmail(oAuth2Response.getEmail());
                existingUser.setUserPhoneNumber(oAuth2Response.getPhoneNumber() != null ? oAuth2Response.getPhoneNumber() : "");
                existingUser.setUpdatedAt(LocalDateTime.now());
                return existingUser;
            } else {
                User newUser = User.builder()
                        .userId(oAuth2Response.getProviderId())
                        .userEmail(oAuth2Response.getEmail())
                        .userPhoneNumber(oAuth2Response.getPhoneNumber() != null ? oAuth2Response.getPhoneNumber() : "")
                        .provider(oAuth2Response.getProvider())
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .role(USERROLE.ROLE_UNKNOWN) // 기본값으로 ROLE_UNKNOWN 설정
                        .build();
                logger.info("새 사용자 생성: {}", newUser);

                // UserAddInfo 생성 및 랜덤 닉네임 부여
                String randomNickname = generateUniqueRandomNickname();
                UserAddInfo userAddInfo = UserAddInfo.builder()
                        .user(newUser)
                        .userNickname(randomNickname)
                        .surveyResponse("") // 기본값
                        .build();
                userAddInfoRepository.save(userAddInfo);

                return newUser;
            }
        });

        User savedUser = userRepository.save(user);
        logger.info("DB에 저장된 사용자: {}", savedUser);
        return savedUser;
    }

    private OAuth2Response getOAuth2Response(OAuth2User oAuth2User, String registrationId) {
        if ("kakao".equals(registrationId)) {
            return new KakaoResponse(oAuth2User.getAttributes());
        } else if ("google".equals(registrationId)) {
            return new GoogleResponse(oAuth2User.getAttributes());
        }
        logger.error("지원하지 않는 OAuth2 제공자: {}", registrationId);
        return null;
    }

    private String getUserIdAttributeName(String registrationId) {
        switch (registrationId.toLowerCase()) {
            case "google":
                return "sub";
            case "kakao":
                return "id";
            default:
                throw new IllegalArgumentException("지원하지 않는 OAuth2 제공자: " + registrationId);
        }
    }

    private String generateUniqueRandomNickname() {
        String[] prefixes = {"노력맨", "열정맨", "행복맨", "도전맨", "취업맨", "부정맨"};
        String nickname = "";
        boolean isUnique = false;

        for (int i = 0; i < 1000 && !isUnique; i++) {
            int randomNumber = new Random().nextInt(1000);
            nickname = prefixes[new Random().nextInt(prefixes.length)] + randomNumber;
            isUnique = !userAddInfoRepository.findByUserNickname(nickname).isPresent();
        }

        if (!isUnique) {
            throw new RuntimeException("Unique nickname could not be generated");
        }

        return nickname;
    }
}
