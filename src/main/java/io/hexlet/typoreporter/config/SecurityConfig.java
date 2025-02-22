package io.hexlet.typoreporter.config;

import io.hexlet.typoreporter.security.filter.WorkspaceAuthTokenFilter;
import io.hexlet.typoreporter.security.provider.WorkspaceTokenAuthenticationProvider;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.zalando.problem.spring.web.advice.AdviceTrait;
import org.zalando.problem.spring.web.advice.security.SecurityProblemSupport;

import static io.hexlet.typoreporter.web.Routers.Typo.TYPOS;
import static io.hexlet.typoreporter.web.Routers.Workspace.API_WORKSPACES;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

@Configuration
@Setter(onMethod_ = @Autowired)
public class SecurityConfig {

    private SecurityProblemSupport problemSupport;

    private WorkspaceTokenAuthenticationProvider workspaceTokenAuthenticationProvider;

    @Bean
    public PasswordEncoder noOpPasswordEncoder() {
        return NoOpPasswordEncoder.getInstance();
    }

    @Bean
    @ConditionalOnMissingBean(AdviceTrait.class)
    public AdviceTrait securityExceptionHandling() {
        return new SecurityExceptionHandler();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, AuthenticationManager authManager) throws Exception {
        http.httpBasic();

        http.exceptionHandling()
            .authenticationEntryPoint(problemSupport)
            .accessDeniedHandler(problemSupport);

        http.authenticationProvider(workspaceTokenAuthenticationProvider);

        http.authorizeRequests()
            .antMatchers(GET, "/", "/webjars/**", "/static/**").permitAll()
            .mvcMatchers(POST, API_WORKSPACES + "/*" + TYPOS).authenticated()
            .anyRequest().permitAll() // TODO remove when login added
            .and()
            .csrf()
            .ignoringRequestMatchers(new AntPathRequestMatcher(API_WORKSPACES + "/*" + TYPOS, POST.name()));

        http.addFilterBefore(workspaceAuthTokenFilter(authManager), BasicAuthenticationFilter.class);

        return http.build();
    }

    private WorkspaceAuthTokenFilter workspaceAuthTokenFilter(AuthenticationManager authManager) {
        return new WorkspaceAuthTokenFilter(authManager);
    }
}
