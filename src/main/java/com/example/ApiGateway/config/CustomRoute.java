package com.example.ApiGateway.config;


import com.example.ApiGateway.filter.AuthorizationHeaderFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//filterconfig파일!
@Configuration
public class CustomRoute {

    @Autowired
    private AuthorizationHeaderFilter authorizationHeaderFilter;

    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder builder){
        return builder.routes()
                .route("UserService_join",r -> r.path("/UserService/join")
                        .uri("lb://UserService"))
                .route("UserService_login",r -> r.path("/UserService/login")
                        .uri("lb://UserService"))
                .route("UserService_other", r -> r.path("/UserService/**")
                        .filters(f -> f

                                .filter(authorizationHeaderFilter.apply(new AuthorizationHeaderFilter.Config())))
                        .uri("lb://UserService"))

                .route(r -> r.path("/ProductService/**")
                        .filters(f->f

                                .filter(authorizationHeaderFilter.apply(new AuthorizationHeaderFilter.Config())))
                        .uri("lb://ProductService"))

                .route(r -> r.path("/PaymentService/**")
                        .filters(f->f

                                .filter(authorizationHeaderFilter.apply(new AuthorizationHeaderFilter.Config())))
                        .uri("lb://PaymentService"))
                .build();
    }
}
