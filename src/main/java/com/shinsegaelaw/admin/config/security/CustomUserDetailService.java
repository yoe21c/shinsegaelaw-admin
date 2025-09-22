package com.shinsegaelaw.admin.config.security;

import com.shinsegaelaw.admin.exception.TbmAdminRuntimeException;
import com.shinsegaelaw.admin.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailService implements UserDetailsService {

    private final MemberRepository memberRepository;
    private final Environment environment;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        return memberRepository.findByEmail(username)
            .orElseThrow(() -> new TbmAdminRuntimeException("[Admin] Not Exist User : " + username));
    }

}