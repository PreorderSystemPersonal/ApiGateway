package com.example.ApiGateway.filter;


import com.example.ApiGateway.Redis.RedisUtil;
import com.example.ApiGateway.jwt.JWTUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Objects;


//권한필터 적용(token)
@Component
@Slf4j
public class AuthorizationHeaderFilter extends AbstractGatewayFilterFactory<AuthorizationHeaderFilter.Config> {
    Environment env;
    private final JWTUtil jwtUtil;
    private final RedisUtil redisUtil;
    public AuthorizationHeaderFilter(Environment env, JWTUtil jwtUtil, RedisUtil redisUtil){
        super(Config.class);
        this.env = env;
        this.jwtUtil = jwtUtil;
        this.redisUtil = redisUtil;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return ((exchange,chain)->{
            ServerHttpRequest request = exchange.getRequest();


            //헤더에 토큰 정보가 포함되어 있는가?
            if(!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)){
                return onError(exchange,"헤더에 토큰 정보가 포함되어 있지 않음 ", HttpStatus.UNAUTHORIZED);
            }
            String authorizationHeader =
                    Objects.requireNonNull(request.getHeaders().get(HttpHeaders.AUTHORIZATION)).get(0);
            String jwt = authorizationHeader.replace("Bearer ","");
            System.out.println(jwt);

            //실제로 맞는 토큰인지 검증
            //토큰 소멸 시간 검증
            if(jwtUtil.isExpired(jwt)){
                System.out.println("token expired");

                //조건이 해당되면 메소드 종료
                return onError(exchange,"토큰이 소멸되어서 접근 불가 ", HttpStatus.UNAUTHORIZED);
            }


            //로그아웃한 토큰인지 확인
            //토큰에서 username과 role 획득
            String username = jwtUtil.getEmail(jwt);
            String role = jwtUtil.getRole(jwt);
            System.out.println(role);

            //Redis에서 username을 키로 가지고 있는지 확인
            if(!redisUtil.hasKey(username)){
                // Redis에 해당 username을 키로 하는 데이터가 존재하지 않음
                System.out.println("redis에서 username을 찾을 수 없음 " + username);

                return onError(exchange,"redis에서 username을 찾을 수 없음 ", HttpStatus.UNAUTHORIZED);
            }
            String tokenFromRedis = redisUtil.getData(username);
            if (!jwt.equals(tokenFromRedis)) {
                // Redis에 해당 username을 키로 하는 데이터와 토큰이 일치하지 않음
                System.out.println("username은 있지만 토큰이 없음: 로그아웃된 회원" + username);

                return onLogout(exchange,"로그아웃된 회원입니다.", HttpStatus.OK);
            }


            request = request.mutate()
                    .header("X-USERNAME", username)
                    .build();

            System.out.println(request.getHeaders());

            return chain.filter(exchange.mutate()
                    .request(request)
                    .build());
        });
    }


    //Web Flux 개념에서 나옴
    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);

        System.out.println(err);
        return response.setComplete();
    }
    //로그인 회원
    private Mono<Void> onSuccess(ServerWebExchange exchange, String err, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);

        System.out.println(err);
        return response.setComplete();
    }
    //로그아웃된 회원
    private Mono<Void> onLogout(ServerWebExchange exchange, String status, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);

        System.out.println(status);
        return response.setComplete();
    }

    public static class Config {
    }
}
