package com.fuegolento.fuegolento_franquicia.security;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.fuegolento.fuegolento_franquicia.model.User;
import com.fuegolento.fuegolento_franquicia.repository.UserRepository;

@Service
public class RepositoryUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public RepositoryUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        // Block login if the user is banned
        if (user.isBanned()) {
            throw new DisabledException("User is banned");
        }

        List<GrantedAuthority> authorities = new ArrayList<>();
        if (user.getRoles() != null) {
            for (String role : user.getRoles()) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
            }
        }

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getEncodedPassword(),
                authorities
        );
    }
}
