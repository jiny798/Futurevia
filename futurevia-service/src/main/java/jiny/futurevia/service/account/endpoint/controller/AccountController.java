package jiny.futurevia.service.account.endpoint.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jiny.futurevia.service.account.application.AccountService;
import jiny.futurevia.service.account.application.LoginService;
import jiny.futurevia.service.account.domain.entity.Account;
import jiny.futurevia.service.account.endpoint.controller.dto.SignUpForm;
import jiny.futurevia.service.account.endpoint.controller.validator.SignUpFormValidator;
import jiny.futurevia.service.account.repository.AccountRepository;
import jiny.futurevia.service.account.support.CurrentUser;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Slf4j
@Controller
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final LoginService loginService;
    private final SignUpFormValidator signUpFormValidator;
    private final JavaMailSender javaMailSender;
    private final AccountRepository accountRepository;

    @InitBinder("signUpForm")
    public void initBinder(WebDataBinder webDataBinder) {
        webDataBinder.addValidators(signUpFormValidator);
    }

    @GetMapping("/sign-up")
    public String signUpForm(Model model) {
        model.addAttribute(new SignUpForm());
        return "account/sign-up";
    }

    @PostMapping("/sign-up")
    public String signUpSubmit(@Valid @ModelAttribute SignUpForm signUpForm, Errors errors, HttpServletRequest request,
                               HttpServletResponse response) {
        if (errors.hasErrors()) {
            return "account/sign-up";
        }
        Account account = accountService.signUp(signUpForm);
        loginService.login(account, request, response);

        return "redirect:/";
    }

    @GetMapping("/check-email-token")
    public String verifyEmail(String token, String email, Model model, HttpServletRequest request, HttpServletResponse response) { // HttpServletRequest request,	HttpServletResponse response

        log.info("[check-email-token] verifyEmail");
        Account account = accountService.findAccountByEmail(email);
        if (account == null) {
            model.addAttribute("error", "wrong.email");
            return "account/email-verification";
        }
        if (!token.equals(account.getEmailToken())) {
            model.addAttribute("error", "wrong.token");
            return "account/email-verification";
        }
        accountService.verify(account);
		loginService.login(account, request, response);

        model.addAttribute("numberOfUsers", accountRepository.count());
        model.addAttribute("nickname", account.getNickname());
        return "account/email-verification";
    }

    @GetMapping("/check-email")
    public String checkMail(@CurrentUser Account account, Model model) {
        model.addAttribute("email", account.getEmail());
        return "account/check-email";
    }


    @GetMapping("/resend-email")
    public String resendEmail(@CurrentUser Account account, Model model) {
        if (!account.enableToSendEmail()) {
            model.addAttribute("error", "인증 이메일은 5분에 한 번만 전송할 수 있습니다.");
            model.addAttribute("email", account.getEmail());
            return "account/check-email";
        }
        accountService.sendVerificationEmail(account);
        return "redirect:/";
    }

    @GetMapping("/profile/{nickname}")
    public String viewProfile(@PathVariable String nickname, Model model, @CurrentUser Account account) {
        log.info("viewProfile ");
        Account byNickname = accountRepository.findByNickname(nickname);
        if (byNickname == null) {
            throw new IllegalArgumentException(nickname + "에 해당하는 사용자가 없습니다.");
        }
        if(account != null){
            log.info("viewProfile account != null");
            model.addAttribute(account);
        }
        model.addAttribute(byNickname);
        model.addAttribute("isOwner", byNickname.equals(account));
        return "account/profile";
    }


}