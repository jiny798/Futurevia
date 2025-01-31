package jiny.futurevia.service.event.endpoint;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.validation.Valid;
import jiny.futurevia.service.account.domain.entity.Account;
import jiny.futurevia.service.account.support.CurrentUser;
import jiny.futurevia.service.event.application.EventService;
import jiny.futurevia.service.event.domain.entity.Event;
import jiny.futurevia.service.event.form.EventForm;
import jiny.futurevia.service.event.infra.repository.EventRepository;
import jiny.futurevia.service.event.vaildator.EventValidator;
import jiny.futurevia.service.study.application.StudyService;
import jiny.futurevia.service.study.domain.entity.Study;
import jiny.futurevia.service.study.infra.repository.StudyRepository;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/study/{path}")
@RequiredArgsConstructor
public class EventController {

    private final StudyService studyService;
    private final EventService eventService;
    private final EventRepository eventRepository;
    private final StudyRepository studyRepository;
    private final EventValidator eventValidator;

    @InitBinder("eventForm")
    public void initBinder(WebDataBinder webDataBinder) {
        webDataBinder.addValidators(eventValidator);
    }

    @GetMapping("/new-event")
    public String newEventForm(@CurrentUser Account account, @PathVariable String path, Model model) {
        Study study = studyService.getStudyToUpdateStatus(account, path);
        model.addAttribute(study);
        model.addAttribute(account);
        model.addAttribute(new EventForm());
        return "event/form";
    }

    @PostMapping("/new-event")
    public String createNewEvent(@CurrentUser Account account, @PathVariable String path, @Valid @RequestBody EventForm eventForm, Errors errors, Model model) {
        Study study = studyService.getStudyToUpdateStatus(account, path);
        if (errors.hasErrors()) {
            model.addAttribute(account);
            model.addAttribute(study);
            return "event/form";
        }
        Event event = eventService.createEvent(study, eventForm, account);
        return "redirect:/study/" + study.getEncodedPath() + "/events/" + event.getId();
    }

    @GetMapping("/events/{id}")
    public String getEvent(@CurrentUser Account account, @PathVariable String path, @PathVariable Long id, Model model) {
        model.addAttribute(account);
        model.addAttribute(eventRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("해당 모임은 존재하지 않습니다.")));
        model.addAttribute(studyRepository.findStudyWithManagersByPath(path));
        return "event/view";
    }

    @GetMapping("/events")
    public String viewStudyEvents(@CurrentUser Account account, @PathVariable String path, Model model) {
        Study study = studyService.getStudy(path);
        model.addAttribute(account);
        model.addAttribute(study);
        List<Event> events = eventRepository.findByStudyOrderByStartDateTime(study);
        List<Event> newEvents = new ArrayList<>();
        List<Event> oldEvents = new ArrayList<>();
        for (Event event : events) {
            if (event.getEndDateTime().isBefore(LocalDateTime.now())) {
                oldEvents.add(event);
            } else {
                newEvents.add(event);
            }
        }
        model.addAttribute("newEvents", newEvents);
        model.addAttribute("oldEvents", oldEvents);
        return "study/events";
    }

    @GetMapping("/events/{id}/edit")
    public String updateEventForm(@CurrentUser Account account, @PathVariable String path, @PathVariable Long id, Model model) {
        Study study = studyService.getStudyToUpdate(account, path);
        Event event = eventRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("모임이 존재하지 않습니다."));
        model.addAttribute(study);
        model.addAttribute(account);
        model.addAttribute(event);
        model.addAttribute(EventForm.from(event));
        return "event/update-form";
    }

    @PostMapping("/events/{id}/edit")
    public String updateEventSubmit(@CurrentUser Account account, @PathVariable String path, @PathVariable Long id, @Valid @RequestBody EventForm eventForm, Errors errors, Model model) {
        Study study = studyService.getStudyToUpdate(account, path);
        Event event = eventRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("모임이 존재하지 않습니다."));

        eventForm.setEventType(event.getEventType());
        eventValidator.validateUpdateForm(eventForm, event, errors);
        if (errors.hasErrors()) {
            model.addAttribute(account);
            model.addAttribute(study);
            model.addAttribute(event);
            return "event/update-form";
        }
        eventService.updateEvent(event, eventForm);
        return "redirect:/study/" + study.getEncodedPath() +  "/events/" + event.getId();
    }

    @DeleteMapping("/events/{id}")
    public String deleteEvent(@CurrentUser Account account, @PathVariable String path, @PathVariable Long id) {
        Study study = studyService.getStudyToUpdateStatus(account, path);
        eventService.deleteEvent(eventRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("모임이 존재하지 않습니다.")));
        return "redirect:/study/" + study.getEncodedPath() + "/events";
    }

    @PostMapping("/events/{id}/enroll")
    public String enroll(@CurrentUser Account account, @PathVariable String path, @PathVariable Long id) {
        Study study = studyService.getStudyToEnroll(path);
        eventService.enroll(eventRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("모임이 존재하지 않습니다.")), account);
        return "redirect:/study/" + study.getEncodedPath() + "/events/" + id;
    }

    @PostMapping("/events/{id}/leave")
    public String leave(@CurrentUser Account account, @PathVariable String path, @PathVariable Long id) {
        Study study = studyService.getStudyToEnroll(path);
        eventService.leave(eventRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("모임이 존재하지 않습니다.")), account);
        return "redirect:/study/" + study.getEncodedPath() + "/events/" + id;
    }
}