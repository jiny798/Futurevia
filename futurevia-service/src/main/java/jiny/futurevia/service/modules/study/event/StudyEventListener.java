package jiny.futurevia.service.modules.study.event;

import jiny.futurevia.service.infra.config.AppProperties;
import jiny.futurevia.service.infra.mail.EmailMessage;
import jiny.futurevia.service.infra.mail.EmailService;
import jiny.futurevia.service.modules.account.domain.entity.Account;
import jiny.futurevia.service.modules.account.infra.predicates.AccountPredicates;
import jiny.futurevia.service.modules.account.infra.repository.AccountRepository;
import jiny.futurevia.service.modules.notification.domain.entity.Notification;
import jiny.futurevia.service.modules.notification.domain.entity.NotificationType;
import jiny.futurevia.service.modules.notification.infra.repository.NotificationRepository;
import jiny.futurevia.service.modules.study.domain.entity.Study;
import jiny.futurevia.service.modules.study.domain.entity.StudyCreatedEvent;
import jiny.futurevia.service.modules.study.infra.repository.StudyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Async
@Transactional
@Component
@RequiredArgsConstructor
public class StudyEventListener {

    private final StudyRepository studyRepository;
    private final AccountRepository accountRepository;
    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private final TemplateEngine templateEngine;
    private final AppProperties appProperties;

    @EventListener
    public void handleStudyCreatedEvent(StudyCreatedEvent studyCreatedEvent) {
        Study study = studyRepository.findStudyWithTagsAndZonesById(studyCreatedEvent.getStudy().getId());
        Iterable<Account> accounts = accountRepository.findAll(AccountPredicates.findByTagsAndZones(study.getTags(), study.getZones()));
        for (Account account : accounts) {
            Account.NotificationSetting notificationSetting = account.getNotificationSetting();
            if (notificationSetting.isStudyCreatedByEmail()) {
                sendEmail(study, account, "새로운 스터디가 오픈하였습니다.", study.getTitle() + " 스터디가 오픈하였습니다.");
            }
            if (notificationSetting.isStudyCreatedByWeb()) {
                saveNotification(study, account, NotificationType.STUDY_CREATED, study.getShortDescription());
            }
        }
    }

    @EventListener
    public void handleStudyUpdateEvent(StudyUpdateEvent studyUpdateEvent) {
        Study study = studyRepository.findStudyWithManagersAndMembersById(studyUpdateEvent.getStudy().getId());
        Set<Account> accounts = new HashSet<>();
        accounts.addAll(study.getManagers());
        accounts.addAll(study.getMembers());
        accounts.forEach(account -> {
            if (account.getNotificationSetting().isStudyUpdatedByEmail()) {
                sendEmail(study, account, studyUpdateEvent.getMessage(),   study.getTitle() + " 스터디에 새소식이 있습니다.");
            }
            if (account.getNotificationSetting().isStudyUpdatedByWeb()) {
                saveNotification(study, account, NotificationType.STUDY_UPDATED, studyUpdateEvent.getMessage());
            }
        });
    }

    private void sendEmail(Study study, Account account, String contextMessage, String emailSubject) {
        Context context = new Context();
        context.setVariable("link", "/study/" + study.getEncodedPath());
        context.setVariable("nickname", account.getNickname());
        context.setVariable("linkName", study.getTitle());
        context.setVariable("message", contextMessage);
        context.setVariable("host", appProperties.getHost());
        String message = templateEngine.process("mail/simple-link", context);
        emailService.sendEmail(EmailMessage.builder()
                .to(account.getEmail())
                .subject(emailSubject)
                .message(message)
                .build());
    }

    private void saveNotification(Study study, Account account, NotificationType notificationType, String message) {
        notificationRepository.save(Notification.from(study.getTitle(), "/study/" + study.getEncodedPath(),
                false, LocalDateTime.now(), message, account, notificationType));
    }


}