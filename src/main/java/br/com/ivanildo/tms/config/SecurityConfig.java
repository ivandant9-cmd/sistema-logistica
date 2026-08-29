package br.com.ivanildo.tms.config;

import br.com.ivanildo.tms.views.LoginView;
import com.vaadin.flow.spring.security.VaadinWebSecurity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@EnableWebSecurity
@Configuration
public class SecurityConfig extends VaadinWebSecurity {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // 1. Libera os recursos estáticos e rotas públicas ANTES de chamar o super.configure
        http.authorizeHttpRequests(auth -> 
            auth.requestMatchers(
                new AntPathRequestMatcher("/images/**"),
                new AntPathRequestMatcher("/icons/**"),
                new AntPathRequestMatcher("/VAADIN/**"),
                new AntPathRequestMatcher("/line-awesome/**"),
                new AntPathRequestMatcher("/checkin/**")
            ).permitAll()
        );

        // 2. Aplica as configurações padrão de segurança do Vaadin (precisa vir depois das regras específicas)
        super.configure(http);

        // 3. Define a View de Login
        setLoginView(http, LoginView.class);
    }

    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails admin = User.builder()
                .username("admin")
                .password("{noop}admin123")
                .roles("ADMIN", "USER")
                .build();

        return new InMemoryUserDetailsManager(admin);
    }

 
}