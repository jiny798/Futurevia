package jiny.futurevia.service.modules.event.event;

import jiny.futurevia.service.infra.config.AppProperties;
import jiny.futurevia.service.infra.mail.EmailMessage;
import jiny.futurevia.service.infra.mail.EmailService;
import jiny.futurevia.service.modules.account.domain.entity.Account;
import jiny.futurevia.service.modules.event.domain.entity.Enrollment;
import jiny.futurevia.service.modules.event.domain.entity.Event;
import jiny.futurevia.service.modules.notification.domain.entity.Notification;
import jiny.futurevia.service.modules.notification.domain.entity.NotificationType;
import jiny.futurevia.service.modules.notification.infra.repository.NotificationRepository;
import jiny.futurevia.service.modules.study.domain.entity.Study;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDateTime;

@Slf4j
@Async
@Component
@Transactional
@RequiredArgsConstructor
public class EnrollmentEventListener {

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private final AppProperties appProperties;
    private final TemplateEngine templateEngine;

    @EventListener
    public void handleEnrollmentEvent(EnrollmentEvent enrollmentEvent) {
        Enrollment enrollment = enrollmentEvent.getEnrollment();
        Account account = enrollment.getAccount();
        Event event = enrollment.getEvent();
        Study study = event.getStudy();
        if (account.getNotificationSetting().isStudyRegistrationResultByEmail()) {
            sendEmail(enrollmentEvent, account, event, study);
        }
        if (account.getNotificationSetting().isStudyRegistrationResultByWeb()) {
            createNotification(enrollmentEvent, account, event, study);
        }
    }

    private void sendEmail(EnrollmentEvent enrollmentEvent, Account account, Event event, Study study) {
        Context context = new Context();
        context.setVariable("nickname", account.getNickname());
        context.setVariable("link", "/study/" + study.getEncodedPath() + "/events/" + event.getId());
        context.setVariable("linkName", study.getTitle());
        context.setVariable("message", enrollmentEvent.getMessage());
        context.setVariable("host", appProperties.getHost());
        String message = templateEngine.process("mail/simple-link", context);
        EmailMessage emailMessage = EmailMessage.builder()
                .subject(event.getTitle() + " 모임 참가 신청 결과입니다." )
                .to(account.getEmail())
                .message(message)
                .build();
        emailService.sendEmail(emailMessage);
    }

    private void createNotification(EnrollmentEvent enrollmentEvent, Account account, Event event, Study study) {
        notificationRepository.save(Notification.from(study.getTitle() + " / " + event.getTitle(),
                "/study/" + study.getEncodedPath() + "/events/" + event.getId(), false,
                LocalDateTime.now(), enrollmentEvent.getMessage(), account, NotificationType.EVENT_ENROLLMENT));
    }
}