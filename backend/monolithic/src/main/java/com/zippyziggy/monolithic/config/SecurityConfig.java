package com.zippyziggy.monolithic.config;

import com.zippyziggy.monolithic.member.filter.CustomAuthenticationEntryPoint;
import com.zippyziggy.monolithic.member.filter.JwtAuthenticationFilter;
import com.zippyziggy.monolithic.member.service.JwtProviderService;
import com.zippyziggy.monolithic.member.service.JwtValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.filter.CorsFilter;

@Configuration // Ioc 할 수 있게 만들기 위해
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    private final CorsFilter corsFilter;
    private final JwtProviderService jwtProviderService;
    private final JwtValidationService jwtValidationService;

    // authenticationManager를 Bean 등록합니다.
    @Override
    @Bean
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {


        http.csrf().disable();
        http.formLogin().disable();
        http
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS) // 세션을 사용하지 않겠다.
                .and()
                .httpBasic().disable() // Bearer 방식 사용 -> header에 authentication에 토큰을 넣어 전달하는 방식
                .authorizeRequests() // 요청에 대한 사용권한 체크
                .requestMatchers(CorsUtils::isPreFlightRequest).permitAll()
                .antMatchers("/members/logout").authenticated()
                .antMatchers("/members").authenticated()
                .antMatchers("/members/profile").authenticated()
                .antMatchers("/prompts").authenticated()
                .antMatchers("/members/**").authenticated()
                .antMatchers("/reports/**").authenticated()
                .antMatchers("/prompts/{promptUuid}/**").authenticated()
                .antMatchers("/talks").authenticated()
                .antMatchers("/talks/{talkId}/**").authenticated()
//                .antMatchers("/members/test/userUtil").hasRole("ADMIN") // ADMIN 권한이 있을 때에만 접근 가능
                .anyRequest().permitAll()
                .and()
                .addFilter(corsFilter)
                .addFilterBefore(new JwtAuthenticationFilter(jwtValidationService, jwtProviderService),
                        UsernamePasswordAuthenticationFilter.class) // JwtAuthenticationFilter를 UsernamePasswordAuthenticationFilter 전에 넣는다
                .exceptionHandling()
                .authenticationEntryPoint(new CustomAuthenticationEntryPoint());
    }

}
