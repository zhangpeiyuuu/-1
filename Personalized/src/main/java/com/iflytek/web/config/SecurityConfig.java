/*
 * Copyright 2002-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.iflytek.web.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iflytek.web.pojo.User;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {
	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http
				.authorizeRequests((authorize) -> authorize
						.antMatchers("user/checkLogin","/css/**", "/home","/home/**","/","/js/**","/images/**","/fonts/**").permitAll()
						.anyRequest().authenticated()
				)
				.formLogin((formLogin) -> formLogin
								.loginPage("/login")
								.loginProcessingUrl("/doLogin")
								.failureUrl("/login")
								.permitAll()
//                        .successForwardUrl("/index")
								.successHandler(new AuthenticationSuccessHandler() {
									@Override
									public void onAuthenticationSuccess(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Authentication authentication) throws IOException, ServletException {
										// 放到session里面去
										//获取头部信息
										SecurityContext context = SecurityContextHolder.createEmptyContext();
										context.setAuthentication(authentication);
										SecurityContextHolder.setContext(context);

										User obj = (User)authentication.getPrincipal();
										String xRequestedWith=httpServletRequest.getHeader("X-Requested-With");
										if(xRequestedWith!= null && xRequestedWith.indexOf("XMLHttpRequest")!=-1){
											// 是ajax的处理方式
											httpServletRequest.getSession().setAttribute("userId", obj.getId());
											httpServletRequest.getSession().setAttribute("userName", obj.getUsername());

											// 向页面上输入true或者false
											httpServletResponse.setContentType("application/json;charset=utf-8");
											ServletOutputStream out = httpServletResponse.getOutputStream();
											ObjectMapper objectMapper = new ObjectMapper();
											objectMapper.writeValue(out, true);
											out.flush();
											out.close();
										}else{

											httpServletRequest.getSession().setAttribute("user", obj);
											httpServletRequest.getSession().setAttribute("userName", obj.getUsername());
											httpServletResponse.sendRedirect("/home/");
										}
									}
								})
								.failureHandler(new AuthenticationFailureHandler() {
									@Override
									public void onAuthenticationFailure(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, AuthenticationException e) throws IOException, ServletException {
										// 登录失败要返回错误消息到页面上
										httpServletRequest.setAttribute("error", e.getMessage());

										String xRequestedWith=httpServletRequest.getHeader("X-Requested-With");
										if(xRequestedWith!= null && xRequestedWith.indexOf("XMLHttpRequest")!=-1){
											// 向页面上输入true或者false
											httpServletResponse.setContentType("application/json;charset=utf-8");
											ServletOutputStream out = httpServletResponse.getOutputStream();
											ObjectMapper objectMapper = new ObjectMapper();
											objectMapper.writeValue(out, false);
											out.flush();
											out.close();
										}else{
											httpServletResponse.sendRedirect("/login");
										}
									}
								})
								//.defaultSuccessUrl("/home/",true) // 登录成功重定向
				)
//				.headers(headers -> headers
//						.frameOptions(frameOptions -> frameOptions.sameOrigin())   // 处理iframe
//				)
				.logout((logout) ->
						logout
								.permitAll()
				)
				.csrf(csrf -> csrf.disable());
	}

	@Bean
	public PasswordEncoder getPasswordEncoder(){
		return new BCryptPasswordEncoder(){
			@Override
			public boolean matches(CharSequence rawPassword, String encodedPassword) {
				return super.matches(rawPassword, encodedPassword);
			}
		};
	}
}
