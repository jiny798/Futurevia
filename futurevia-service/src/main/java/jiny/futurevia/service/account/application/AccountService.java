package jiny.futurevia.service.account.application;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jiny.futurevia.service.account.domain.dto.AccountContext;
import jiny.futurevia.service.account.domain.entity.Account;
import jiny.futurevia.service.account.domain.entity.Role;
import jiny.futurevia.service.account.endpoint.controller.SignUpForm;
import jiny.futurevia.service.account.repository.AccountRepository;
import jiny.futurevia.service.admin.repository.RoleRepository;
import lombok.RequiredArgsConstructor;

import org.modelmapper.ModelMapper;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class AccountService {

	private final AccountRepository accountRepository;
	private final RoleRepository roleRepository;
	private final JavaMailSender mailSender;
	private final PasswordEncoder passwordEncoder;

	/*** Authenticate ***/
	private final AuthenticationManager authenticationManager;
	private final HttpSessionSecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

	@Transactional
	public Account signUp(SignUpForm signUpForm) {
		Account newAccount = saveNewAccount(signUpForm);
		newAccount.generateToken();
		sendVerificationEmail(newAccount);
		return newAccount;
	}

	private Account saveNewAccount(SignUpForm signUpForm) {
		Role role = roleRepository.findByRoleName("ROLE_USER");
		Set<Role> roles = new HashSet<>();
		roles.add(role);

		Account account = Account.builder()
			.email(signUpForm.getEmail())
			.nickname(signUpForm.getNickname())
			.password(passwordEncoder.encode(signUpForm.getPassword()))
			.notificationSetting(Account.NotificationSetting.builder()
				.studyCreatedByWeb(true)
				.studyUpdatedByWeb(true)
				.studyRegistrationResultByWeb(true)
				.build())
			.userRoles(roles)
			.build();
		return accountRepository.save(account);
	}

	public void sendVerificationEmail(Account newAccount) {
		SimpleMailMessage mailMessage = new SimpleMailMessage();
		mailMessage.setTo(newAccount.getEmail());
		mailMessage.setSubject("회원 가입 인증");
		mailMessage.setText(String.format("/check-email-token?token=%s&email=%s", newAccount.getEmailToken(),
			newAccount.getEmail()));
		mailSender.send(mailMessage);
	}

	public Account findAccountByEmail(String email) {
		return accountRepository.findByEmail(email);
	}

	public void login(String email, String password, HttpServletRequest request, HttpServletResponse response) {
		//        UsernamePasswordAuthenticationToken unAuthenticationToken = new UsernamePasswordAuthenticationToken(
		//                account.getNickname(),
		//                account.getPassword(),
		//                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")));
		// Security 에서 제공하는 DaoAuthenticationProvider 는 email, password 평문으로 전달해야함
		UsernamePasswordAuthenticationToken unAuthenticationToken = UsernamePasswordAuthenticationToken.unauthenticated(
			email,
			password);

		Authentication authentication = authenticationManager.authenticate(unAuthenticationToken);
		SecurityContext securityContext = SecurityContextHolder.getContextHolderStrategy().createEmptyContext();
		securityContext.setAuthentication(authentication);
		// Save in ThreadLocal
		SecurityContextHolder.getContextHolderStrategy().setContext(securityContext);

		securityContextRepository.saveContext(securityContext, request, response);

	}
}