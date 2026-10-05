package com.soodfolio.api.security;

import com.soodfolio.api.entity.Portfolio;
import com.soodfolio.api.entity.User;
import com.soodfolio.api.repository.PortfolioRepository;
import com.soodfolio.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PortfolioRepository portfolioRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        // 1. Try finding by email
        User user = userRepository.findByEmail(usernameOrEmail)
                .orElseGet(() -> {
                    // 2. If not found by email, try finding user via portfolio username
                    Portfolio portfolio = portfolioRepository.findByUsername(usernameOrEmail)
                            .orElseThrow(() -> new UsernameNotFoundException("User not found with email or username: " + usernameOrEmail));
                    return portfolio.getUser();
                });

        return UserPrincipal.create(user);
    }

    @Transactional(readOnly = true)
    public UserDetails loadUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + id));
        return UserPrincipal.create(user);
    }
}
