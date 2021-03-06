package io.licensemanager.backend.configuration.authentication.service;

import io.licensemanager.backend.entity.Token;
import io.licensemanager.backend.entity.User;
import io.licensemanager.backend.repository.TokenRepository;
import io.licensemanager.backend.util.TimeTokensParser;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.token.Sha512DigestUtils;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAmount;
import java.util.Optional;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class AuthorizationTokenService {

    private static final Logger logger = LoggerFactory.getLogger(AuthorizationTokenService.class);

    private final String AUTHORIZATION_HEADER = "Authorization";
    private final String TOKEN_TYPE = "Bearer";
    private final int TOKEN_BYTES = 256;

    private TemporalAmount TOKEN_TTL_VALUE;
    private final TokenRepository tokenRepository;

    @Value("${token.ttl:24h}")
    public void setTokenTTLValue(String tokenTTLValue) {
        this.TOKEN_TTL_VALUE = TimeTokensParser.parseTimeToken(tokenTTLValue);
    }

    public Optional<String> parseTokenFromRequest(HttpServletRequest request) {
        String authorizationHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (!StringUtils.isEmpty(authorizationHeader) && authorizationHeader.startsWith(TOKEN_TYPE)) {
            return Optional.of(StringUtils.removeStart(authorizationHeader, TOKEN_TYPE).trim());
        }

        return Optional.empty();
    }

    public Optional<Token> findTokenByValue(final String value) {
        return tokenRepository.findByValue(value);
    }

    public boolean isTokenValid(Token token) {
        return LocalDateTime.now()
                .isBefore(token.getExpirationDate());
    }

    public String getAuthorizationTokenForUser(Long userId, String userAgent) {
        if (StringUtils.isNotEmpty(userAgent)) {
            Optional<Token> token = tokenRepository.findByUserIdAndUserAgent(userId, userAgent);
            if (token.isPresent()) {
                Token foundToken = token.get();
                if (isTokenValid(foundToken)) {
                    renewToken(foundToken);
                    return token.get().getValue();
                } else {
                    tokenRepository.delete(foundToken);
                }
            }
        }

        return generateToken();
    }

    private String generateToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);

        return Sha512DigestUtils.shaHex(bytes);
    }

    private void renewToken(Token token) {
        logger.debug("Refreshing authorization token");
        LocalDateTime currentTimeStamp = LocalDateTime.now();
        token.setCreationDate(currentTimeStamp);
        token.setExpirationDate(currentTimeStamp
                .plus(TOKEN_TTL_VALUE));
        tokenRepository.save(token);
    }

    public void assignTokenToUser(String tokenValue, String userAgent, User user) {
        if (tokenRepository.findByValue(tokenValue).isEmpty()) {
            logger.debug("Assigning new authorization token to user with username {}", user.getUsername());
            Token token = new Token();
            token.setValue(tokenValue);
            LocalDateTime currentDate = LocalDateTime.now();
            token.setExpirationDate(currentDate.plus(TOKEN_TTL_VALUE));
            token.setCreationDate(currentDate);
            token.setUser(user);
            token.setUserAgent(userAgent);

            tokenRepository.save(token);
        } else {
            logger.debug("User got some old valid stored authorization token for its UA");
        }
    }

    @Transactional
    public void purgeAuthorizationToken(HttpServletRequest request) {
        Optional<String> authToken = parseTokenFromRequest(request);
        if (authToken.isPresent()) {
            if (tokenRepository.deleteByValue(authToken.get()) != 0) {
                logger.debug("Authorization token deleted successfully");
            } else {
                logger.debug("Failed to delete authorization token");
            }
        }
    }

    public void revokeTokenFromUser(User user) {
        logger.debug("Revoking token from user with username {}", user.getUsername());
        Optional<Token> token = tokenRepository.findByUserId(user.getId());
        token.ifPresent(tokenToDelete -> tokenRepository.delete(tokenToDelete));
    }
}
