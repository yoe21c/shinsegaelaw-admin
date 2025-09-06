package com.tbm.admin.config.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import java.io.IOException;

@Slf4j
@Configuration
public class LoginFailHandler implements AuthenticationFailureHandler {
    @Override
    public void onAuthenticationFailure(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, AuthenticationException e) throws IOException, ServletException {

        if(e instanceof BadCredentialsException || e instanceof AuthenticationServiceException) {
            String errMsg = "Invalid ID OR Password";
            e = new BadCredentialsException(errMsg);
            httpServletRequest.getSession().setAttribute("errMsg", errMsg);
        }

        httpServletResponse.sendRedirect("/login?error=true");
    }
}