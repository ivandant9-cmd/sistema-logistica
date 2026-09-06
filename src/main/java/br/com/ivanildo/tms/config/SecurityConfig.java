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

@EnableWebSecurity
@Configuration
public class SecurityConfig extends VaadinWebSecurity {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // 1. Libera os recursos estáticos e rotas públicas usando padrão moderno em string
        http.authorizeHttpRequests(auth -> 
            auth.requestMatchers(
                "/images/**",
                "/icons/**",
                "/VAADIN/**",
                "/line-awesome/**",
                "/checkin/**"
            ).permitAll()
        );

        // 2. Aplica as configurações padrão de segurança do Vaadin
        super.configure(http);

        // 3. Define a View de Login
        setLoginView(http, LoginView.class);
    }

    @Bean
    public UserDetailsService customUserDetailsService() {
        UserDetails admin = User.builder()
                .username("admin")
                .password("{noop}admin123")
                .roles("ADMIN", "USER")
                .build();

        UserDetails pcl = User.builder()
                .username("pcl")
                .password("{noop}pcl123")
                .roles("PCL")
                .build();

        return new InMemoryUserDetailsManager(admin, pcl);
    }
}