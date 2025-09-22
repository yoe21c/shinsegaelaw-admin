package com.shinsegaelaw.admin.config;

import com.shinsegaelaw.admin.config.security.Authed;
import com.shinsegaelaw.admin.interceptor.LoggingInterceptor;
import com.shinsegaelaw.admin.utils.Utils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

	private final LoggingInterceptor loggingInterceptor;

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry
			.addResourceHandler("/resources/**")
			.addResourceLocations("/resources/");
	}

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/**").allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS").allowedOrigins("*");
	}

	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
		resolvers.add(loginUserIdResolver());
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(loggingInterceptor)
			.addPathPatterns("/**")
			.excludePathPatterns("/vendor/**", "/css/*", "/img/*");;
	}

	public HandlerMethodArgumentResolver loginUserIdResolver() {

		return new HandlerMethodArgumentResolver() {

			@Override
			public boolean supportsParameter(MethodParameter parameter) {
				return parameter.hasParameterAnnotation(Authed.class);
			}

			@Override
			public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
										  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
				HttpServletRequest request = (HttpServletRequest) webRequest.getNativeRequest();
				final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
				log.debug("authentication ::: > {}", Utils.toJson(authentication));
				return authentication.getPrincipal();
			}
		};
	}
}