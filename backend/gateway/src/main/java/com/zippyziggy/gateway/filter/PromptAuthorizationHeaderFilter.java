package com.zippyziggy.gateway.filter;

import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zippyziggy.gateway.dto.JwtPayLoadResponseDto;
import com.zippyziggy.gateway.dto.JwtResponse;
import com.zippyziggy.gateway.model.Member;
import com.zippyziggy.gateway.repository.MemberRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.Base64Utils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static com.auth0.jwt.JWT.require;

@Slf4j
@Component
public class PromptAuthorizationHeaderFilter extends AbstractGatewayFilterFactory<PromptAuthorizationHeaderFilter.Config> {
	private static final Logger log = LoggerFactory.getLogger(AuthorizationHeaderFilter.class);
	private final ObjectMapper objectMapper = new ObjectMapper();
	private final String BEARER_PREFIX = "Bearer ";
	private final String jwtSecretKey;
	private final MemberRepository memberRepository;


	public PromptAuthorizationHeaderFilter(@Value("${jwt.secret.key}") String jwtSecretKey,
		MemberRepository memberRepository,
		Environment env) {
		super(Config.class);
		this.jwtSecretKey = jwtSecretKey;
		this.memberRepository = memberRepository;
	}

	public static class Config {

	}

	@Override
	public GatewayFilter apply(Config config) {
		return (exchange, chain) -> {
			ServerHttpRequest request = exchange.getRequest();

			if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
				exchange.getRequest()
					.mutate()
					.header("crntMemberUuid", "defaultValue")
					.build();
			}
			else {

				String token = resolveToken(request);
				if (token != null) {
					try {
						// accessToken인지 refreshToken인지 확인
						JwtPayLoadResponseDto jwtPayLoadResponseDto = checkToken(token);
						String tokenType = jwtPayLoadResponseDto.getSub();
						log.info("tokenType =  " + tokenType);
						// accessToken인 경우
						JwtResponse jwtResponse = validateRefreshToken(token);
						if (tokenType.equals("accessToken")) {
							//유효한 access토큰인지 확인
							log.info("accessTokenJwtResponse = " + jwtResponse);
						}
						// refreshToken인 경우
						else {
							//유효한 refresh토큰인지 확인
							log.info("refreshTokenJwtResponse = " + jwtResponse);
						}

						// 토큰이 유효하면 토큰으로부터 유저 정보를 받아온다.
						DecodedJWT verify = require(Algorithm.HMAC512(jwtSecretKey)).build().verify(token);
						String userUuid = verify.getClaim("userUuid").asString();

						exchange.getRequest()
							.mutate()
							.header("crntMemberUuid", userUuid)
							.build();

					} catch (JWTDecodeException e) {
						onError(exchange, "Can not decode token", HttpStatus.UNAUTHORIZED);
					} catch (TokenExpiredException e) {
						onError(exchange, "Token is expired", HttpStatus.UNAUTHORIZED);
					} catch (Exception e) {
						onError(exchange, "어떤어떤익셉션", HttpStatus.UNAUTHORIZED);
					}
				}
			}
			return chain.filter(exchange);
		};
	}



	private JwtResponse validateRefreshToken(String refreshToken) {
		System.out.println("refreshToken = " + refreshToken);
		try {
			// token 내용이 유효한지 확인
			boolean contentCheck = tokenContentCheck(refreshToken);

			if (!contentCheck) {
				throw new JWTDecodeException(JwtResponse.REFRESH_TOKEN_MISMATCH.getJwtResponse());
			}
			DecodedJWT verify = require(Algorithm.HMAC512(jwtSecretKey)).build().verify(refreshToken);
			// 만료시간이 지난 경우 새로운 refreshToken 생성
			if (verify.getExpiresAt().before(new Date())) {

				throw new TokenExpiredException("만료된 refresh 토큰입니다.", Instant.now());
			}
		} catch (TokenExpiredException e) {

			log.info("만료된 토큰입니다." + e);
			throw new TokenExpiredException("만료된 refresh 토큰입니다.", Instant.now());

		} catch (Exception e) {
			log.info("유효하지 않은 토큰입니다" + e);
			throw new JWTDecodeException(JwtResponse.REFRESH_TOKEN_MISMATCH.getJwtResponse());

		}
		return JwtResponse.REFRESH_TOKEN_SUCCESS;
	}


	private JwtResponse validateAccessToken(String accessToken) {
		try {
			boolean contentCheck = tokenContentCheck(accessToken);

			// token 내용이 유효한지 확인
			if (!contentCheck) {
				throw new JWTDecodeException(JwtResponse.ACCESS_TOKEN_MISMATCH.getJwtResponse());
			}

			// JWT 분리시키기
			DecodedJWT verify = require(Algorithm.HMAC512(jwtSecretKey)).build().verify(accessToken);


			// 만료시간이 지난 경우 새로운 accessToken 생성
			if (verify.getExpiresAt().before(new Date())) {

				throw new TokenExpiredException("만료된 토큰입니다.", Instant.now());
			}

		} catch (TokenExpiredException e) {

			System.out.println("만료된 토큰입니다." + e);
			throw new TokenExpiredException("만료된 토큰입니다.", Instant.now());

		} catch (Exception e) {

			System.out.println("유효하지 않은 토큰입니다" + e);
			throw new JWTDecodeException(JwtResponse.ACCESS_TOKEN_MISMATCH.getJwtResponse());

		}

		return JwtResponse.ACCESS_TOKEN_SUCCESS;
	}

	private boolean tokenContentCheck(String token) throws NoSuchAlgorithmException, InvalidKeyException {

		DecodedJWT verify = require(Algorithm.HMAC512(jwtSecretKey)).build().verify(token);
		// token이 가지고 있는 signature 추출
		String signature = verify.getSignature();

		// header와 payload를 합치기(JWT signature 생성 규칙)
		String data = verify.getHeader() + "." +verify.getPayload();

		// header와 payload를 통해 암호화 진행 -> bytes를 Base64url로 반환
		String result = hmacWithJava("HmacSHA512", data, jwtSecretKey);

		// token이 유효한지 확인
		return signature.equals(result);
	}

	private String hmacWithJava(String algorithm, String data, String key)
		throws NoSuchAlgorithmException, InvalidKeyException {
		SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(), algorithm);
		Mac mac = Mac.getInstance(algorithm);
		mac.init(secretKeySpec);
		String temp = Base64.getEncoder().encodeToString(mac.doFinal(data.getBytes()));
		temp = temp.replace("+", "-");
		temp = temp.replace("/", "_");
		temp = temp.replace("=", "");

		return temp;
	}

	private JwtPayLoadResponseDto checkToken(String token) {
		String[] data = token.split("\\.");
		String s = data[1];
		String decode = new String(Base64Utils.decode(s.getBytes()));
		try {
			JwtPayLoadResponseDto jwtPayLoadResponseDto = objectMapper.readValue(decode, JwtPayLoadResponseDto.class);
			return jwtPayLoadResponseDto;
		} catch (Exception e) {
			throw new JWTDecodeException("유효하지 않은 토큰입니다.");
		}
	}

	// Mono, Flux -> Spring WebFlux 단일값일 때 Mono.
	private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus) {
		ServerHttpResponse response = exchange.getResponse();
		response.setStatusCode(httpStatus);

		return response.setComplete();
	}

	private String resolveToken(ServerHttpRequest request) {
		String authorizationToken = request.getHeaders().get(HttpHeaders.AUTHORIZATION).get(0);
		if (StringUtils.hasText(authorizationToken) && authorizationToken.startsWith(BEARER_PREFIX)) {
			return authorizationToken.substring(7);
		}
		return null;
	}


}
