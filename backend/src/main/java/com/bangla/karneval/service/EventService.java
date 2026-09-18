package com.bangla.karneval.service;

import com.bangla.karneval.dto.request.EventRequest;
import com.bangla.karneval.model.Event;
import com.bangla.karneval.repository.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EventService {

    @Autowired private EventRepository eventRepository;
    @Autowired private ProgrammeService programmes;

    public List<Event> getByYear(Integer year){
        return eventRepository.findByEventEditionId(programmes.legacyId(year));
    }

    public List<Event> getHighlights(Integer year){
        return eventRepository.findByEventYearAndIsHighlight(year, true);
    }
    public List<Event> getByEdition(Long id) { return eventRepository.findByEventEditionId(id); }
    public List<Event> getAll() {                                    // ← ADD THIS
        return eventRepository.findAll();
    }


    @Transactional
    public Event create(EventRequest request) {
        Event event = new Event();
        var edition=programmes.get(programmes.resolve(request.getEventEditionId(),request.getEventYear()));
        event.setEventYear(edition.getEventYear());
        event.setEventEditionId(edition.getEventEditionId());
        event.setCategory(request.getCategory());
        event.setTitle(request.getTitle());
        event.setDescription(request.getDescription());
        event.setIsHighlight(request.getIsHighlight());
        return eventRepository.save(event);
    }

    @Transactional
    public Event update(Long id, EventRequest request) {
        Event event = eventRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Event not found"));
        event.setCategory(request.getCategory());
        event.setTitle(request.getTitle());
        event.setDescription(request.getDescription());
        event.setIsHighlight(request.getIsHighlight());
        return eventRepository.save(event);
    }

    @Transactional
    public void delete(Long id) { eventRepository.deleteById(id); }
}
