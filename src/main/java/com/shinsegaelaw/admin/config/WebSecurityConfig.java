package com.shinsegaelaw.admin.config;

import com.shinsegaelaw.admin.config.security.LoginFailHandler;
import com.shinsegaelaw.admin.config.security.LoginSuccessHandler;
import com.shinsegaelaw.admin.config.security.MyLogoutSuccessHandler;
import jakarta.servlet.DispatcherType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class WebSecurityConfig {

    private final LoginSuccessHandler loginSuccessHandler;
    private final MyLogoutSuccessHandler logoutSuccessHandler;
    private final LoginFailHandler loginFailHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(request -> request
                .dispatcherTypeMatchers(DispatcherType.FORWARD).permitAll()
                .requestMatchers("/login", "/favicon.ico", "/error").permitAll()
                .requestMatchers("/public-api/**", "/actuator/**", "/api/counsel/**").permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(formLogin -> formLogin
                .loginPage("/login")
                .usernameParameter("email")
                .passwordParameter("password")
                .successHandler(loginSuccessHandler)
                .failureHandler(loginFailHandler)
            )
            .logout(logout -> logout
                .logoutSuccessHandler(logoutSuccessHandler)
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login")
                .invalidateHttpSession(true)
            )
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configure(http));

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().requestMatchers(
            "/assets/**"
        );
    }

}