package com.bangla.karneval.controller;
import com.bangla.karneval.service.*;
import com.bangla.karneval.model.*;
import com.bangla.karneval.repository.*;
import com.bangla.karneval.dto.request.EditionRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;

@RestController
public class ProgrammeController {
 private final ProgrammeService programmes;
 private final EventRepository events;
 private final GalleryItemRepository gallery;
 private final ProgrammePosterService posters;
 public ProgrammeController(ProgrammeService programmes,EventRepository events,GalleryItemRepository gallery,ProgrammePosterService posters) {
  this.programmes=programmes; this.events=events; this.gallery=gallery; this.posters=posters;
 }
 @GetMapping("/api/admin/programmes") public Object programmes() { return programmes.programmes(); }
 @GetMapping("/api/admin/editions") public Object adminEditions() { return programmes.list(); }
 @GetMapping("/api/admin/editions/{id}") public EventConfig edition(@PathVariable long id) { return programmes.get(id); }
 @GetMapping("/api/editions") public Object publicEditions() {
  Long active=programmes.current().getEventEditionId();
  return programmes.list().stream().filter(c->c.getEventEditionId().equals(active) || !gallery.findByEventEditionIdOrderByDisplayOrderAsc(c.getEventEditionId()).isEmpty())
   .map(c->Map.of("eventEditionId",c.getEventEditionId(),"title",c.getTitle(),"eventYear",c.getEventYear())).toList();
 }
 @GetMapping("/api/editions/{id}/activities") public List<Event> activities(@PathVariable long id) { programmes.get(id); return events.findByEventEditionId(id); }
 @GetMapping("/api/editions/{id}/gallery") public List<GalleryItem> gallery(@PathVariable long id,@RequestParam(defaultValue="false") boolean highlight) {
  programmes.get(id); return gallery.findByEventEditionIdOrderByDisplayOrderAsc(id).stream().filter(i->!highlight || Boolean.TRUE.equals(i.getIsHighlight())).toList();
 }
 @PostMapping("/api/admin/editions") public EventConfig create(@Valid @RequestBody EditionRequest request) { return programmes.save(null,request); }
 @PutMapping("/api/admin/editions/{id}") public EventConfig save(@PathVariable long id,@Valid @RequestBody EditionRequest request) { return programmes.save(id,request); }
 @PutMapping("/api/admin/editions/{id}/activate") public EventConfig activate(@PathVariable long id) { return programmes.activate(id); }
 @PostMapping("/api/admin/editions/{id}/poster") public EventConfig poster(@PathVariable long id,@RequestParam MultipartFile image) throws java.io.IOException {
  programmes.get(id); programmes.poster(id,posters.saveImage(image)); return programmes.get(id);
 }
 @GetMapping("/api/organisation") public Object organisation() { return programmes.organisation(); }
 public record OrganisationRequest(@NotBlank @Size(max=15000) String story,@Size(max=30) String contactPhone,
  @Email @Size(max=150) String contactEmail,@Size(max=255) String contactFacebook,@Size(max=255) String contactInstagram) {}
 @PutMapping("/api/admin/organisation") public Object organisation(@Valid @RequestBody OrganisationRequest r) {
  programmes.organisation(r.story(),r.contactPhone(),r.contactEmail(),r.contactFacebook(),r.contactInstagram()); return programmes.organisation();
 }
}
