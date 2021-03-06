package com.jiyeon.project.config;

import com.jiyeon.project.jwt.AuthoritiesLoggingAfterFilter;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.header.writers.DelegatingRequestMatcherHeaderWriter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.security.web.header.writers.frameoptions.XFrameOptionsHeaderWriter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.Collections;


@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(securedEnabled = true)
@AllArgsConstructor
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    private final Oauth2UserService oauth2UserService;

    @Override
    protected void configure(HttpSecurity http) throws Exception {

        //filter ?????? --> spring filter
        http.addFilterBefore(new AuthoritiesLoggingAfterFilter(), BasicAuthenticationFilter.class);
        http.addFilterAfter(new AuthoritiesLoggingAfterFilter(), BasicAuthenticationFilter.class);


        // to make sure not to generate session ????????????
        http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .cors().configurationSource(corsConfigurationSource())
                .and()
        .formLogin().disable()
        .httpBasic().disable()
        .csrf().disable()
                .authorizeRequests()
                .antMatchers("/auth/**").permitAll()
                .antMatchers("/**").permitAll()
                .antMatchers("/non-auth/**").permitAll()
                .antMatchers("/admin/**").permitAll();

        // ?????? ????????? ?????? ?????? ?????????.
        // jwt?????? ?????? ?????? ????????? ????????? ???????????? ????????? ???????????? ????????? ????????? ??????.
        // ????????? ????????? ?????? ???????????? ?????????????????? ????????? ????????????.

//                .formLogin()
//                .loginPage("/login")
//                .loginProcessingUrl("/auth/login") // check the path out !
//                .defaultSuccessUrl("/")
//                .and()
//                .oauth2Login()
//                .loginPage("/login") // when google login completed, needed to be afterward works ->with token && profiles
//                .userInfoEndpoint()
//                .userService(oauth2UserService);

        /**
         * authenticated();
         * permitAll();
         * anyRequest();
         * denyAll();
         */


        /**
         *  ????????? ????????? ????????? ->> ????????? ???????????? ????????? ????????????????????? ????????? ??? ??? ??????.
         *  ????????? ???????????? ????????? ?????? ??????????????? ?????? ?????? ??????????????? ????????????
         *  ????????? ?????????????????? ????????????????????? ???????????? ??? ??????.
         */
        http.sessionManagement()
                .maximumSessions(1)
                .expiredUrl("/login/expired")
                .maxSessionsPreventsLogin(true);


        /**
         * ?????????????????? ???????????? ?????? http ????????????s
         */

        http.headers()
                .cacheControl()
                .and()
                .contentTypeOptions()
                .and()
                .httpStrictTransportSecurity()
                .and()
                .frameOptions()
                .and()
                .xssProtection();


        /**
         * ????????? ?????? ???????????? ?????? 1)
         */

        http.headers()
                .addHeaderWriter(
                        new StaticHeadersWriter(
                        "X-Content-Security-Policy",
                        "default-src 'self'"))
                .addHeaderWriter(
                        new StaticHeadersWriter(
                                "X-WebKit-CSP",
                                "default-src 'self'"));

        /**
         * ????????? ?????? ???????????? ?????? 2)
         */

        http.headers()
                .addHeaderWriter(
                        new XFrameOptionsHeaderWriter(
                                XFrameOptionsHeaderWriter.XFrameOptionsMode.SAMEORIGIN
                        )
                );

        /**
         * ????????? ?????? ???????????? ?????? 3)
         */
        DelegatingRequestMatcherHeaderWriter headerWriter =
                new DelegatingRequestMatcherHeaderWriter(
                        new AntPathRequestMatcher("/login"),
                        new XFrameOptionsHeaderWriter());

        http.headers()
                .addHeaderWriter(headerWriter);

    }


    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder(){
        return new BCryptPasswordEncoder();
    }


//    @Override
//    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
//
//        //configure default user
//        //like i did at application.properties
//
//        InMemoryUserDetailsManager userDetailsService = new InMemoryUserDetailsManager();
//
//        UserDetails admin = User.withUsername(userDetailsService.).password("9999").authorities("admin").build();
//        UserDetails user = User.withUsername("user").password("12345").authorities("read").build();
//
//        userDetailsService.createUser(admin);
//        userDetailsService.createUser(user);
//
//        auth.userDetailsService(userDetailsService);
//    }

    @Bean
    public UserDetailsService userDetailsService(DataSource dataSource) {
        return new JdbcUserDetailsManager(dataSource);
    }

    @Bean
    public PasswordEncoder passwordEncoder(){
        return NoOpPasswordEncoder.getInstance();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Collections.singletonList("http://localhost:3300"));
        config.setAllowedMethods(Collections.singletonList("*"));
        config.setAllowCredentials(true);
        config.setAllowedHeaders(Collections.singletonList("*"));
        config.setMaxAge(3600L);
        config.setExposedHeaders(Arrays.asList("Authorization")); //if there is other application

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }

}
