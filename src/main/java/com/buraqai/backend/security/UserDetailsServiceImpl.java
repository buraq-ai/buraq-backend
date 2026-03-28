package com.buraqai.backend.security;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Temporary implementation - we'll replace this later with real database lookup
        if (username.equals("admin")) {
            return User.builder()
                    .username("admin")
                    .password("{noop}admin123")   // {noop} means no password encoder for now
                    .roles("USER")
                    .build();
        }
        throw new UsernameNotFoundException("User not found: " + username);
    }
}