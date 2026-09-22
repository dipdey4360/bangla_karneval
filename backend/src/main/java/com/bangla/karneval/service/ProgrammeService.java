package com.bangla.karneval.service;
import com.bangla.karneval.model.EventConfig;
import com.bangla.karneval.dto.request.EditionRequest;
import org.springframework.jdbc.core.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;
import java.util.*;

@Service
public class ProgrammeService {
 private final JdbcTemplate jdbc;
 public ProgrammeService(JdbcTemplate jdbc) { this.jdbc=jdbc; }
 private static final String SELECT="SELECT e.*,p.code AS programme_code FROM event_editions e JOIN programmes p ON p.id=e.programme_id";
 private final RowMapper<EventConfig> mapper=(r,n)->{
  var c=new EventConfig(); c.setEventEditionId(r.getLong("id")); c.setEventYear(r.getInt("event_year"));
  c.setLegacyEventYear((Integer)r.getObject("legacy_event_year")); c.setTitle(r.getString("title"));
  c.setProgrammeCode(r.getString("programme_code")); c.setTagline(r.getString("tagline"));
  c.setAboutText(r.getString("description")); c.setEventDate(r.getObject("event_date",java.time.LocalDate.class));
  c.setEventLocation(r.getString("event_location")); c.setPricePerPerson(r.getBigDecimal("price_per_person"));
  c.setRegistrationEnabled(r.getBoolean("registration_enabled")); c.setPerformerEnabled(r.getBoolean("performer_enabled"));
  c.setThemeKey(r.getString("theme_key")); c.setAccentColor(r.getString("accent_color")); c.setPosterPath(r.getString("poster_path"));
  c.setPaymentInstructions(r.getString("payment_instructions")); c.setVersion(r.getLong("version"));
  c.setContactPhone(r.getString("contact_phone")); c.setContactEmail(r.getString("contact_email"));
  c.setContactWhatsapp(r.getString("contact_whatsapp")); c.setContactFacebook(r.getString("contact_facebook")); c.setContactInstagram(r.getString("contact_instagram")); return c;
 };
 public List<Map<String,Object>> programmes() { return jdbc.queryForList("SELECT code,name FROM programmes ORDER BY id"); }
 public EventConfig get(long id) { return jdbc.query(SELECT+" WHERE e.id=?",mapper,id).stream().findFirst().orElseThrow(()->new ResponseStatusException(NOT_FOUND,"Event not found")); }
 public EventConfig current() { Long id=jdbc.queryForObject("SELECT active_event_edition_id FROM application_settings WHERE id=1",Long.class); if(id==null) throw new ResponseStatusException(SERVICE_UNAVAILABLE,"Active event is not configured"); return get(id); }
 public List<EventConfig> list() { return jdbc.query(SELECT+" ORDER BY e.event_year DESC,e.event_date DESC NULLS LAST,e.id DESC",mapper); }
 public Long legacyId(int year) { return jdbc.query("SELECT id FROM event_editions WHERE legacy_event_year=?",(r,n)->r.getLong(1),year).stream().findFirst().orElseThrow(()->new ResponseStatusException(NOT_FOUND,"Event year not found")); }
 public Long resolve(Long id,Integer year) { if(id!=null) return get(id).getEventEditionId(); return year!=null?legacyId(year):current().getEventEditionId(); }
 @Transactional public EventConfig save(Long id,EditionRequest r) {
  jdbc.queryForObject("SELECT id FROM application_settings WHERE id=1 FOR UPDATE",Integer.class);
  validateUrl(r.contactFacebook()); validateUrl(r.contactInstagram());
  var p=jdbc.query("SELECT id FROM programmes WHERE code=?",(s,n)->s.getLong(1),r.programmeCode());
  if(p.isEmpty()) throw new ResponseStatusException(BAD_REQUEST,"Choose a valid programme");
  if(id==null) {
   id=jdbc.queryForObject("INSERT INTO event_editions(programme_id,slug,title,event_year,theme_key) VALUES(?,?,?,?,?) RETURNING id",Long.class,p.get(0),r.programmeCode()+"-"+r.eventYear()+"-"+UUID.randomUUID().toString().substring(0,8),r.title().trim(),r.eventYear(),r.themeKey());
  } else {
   var old=get(id);
   if(!Objects.equals(old.getVersion(),r.version())) throw new ResponseStatusException(CONFLICT,"This event was changed. Reload its settings before saving.");
   if(!old.getEventYear().equals(r.eventYear()) || !old.getProgrammeCode().equals(r.programmeCode())) throw new ResponseStatusException(BAD_REQUEST,"Programme and year cannot be changed after creation. Create another event instead.");
  }
  int changed=jdbc.update("UPDATE event_editions SET title=?,tagline=?,description=?,event_date=?,event_location=?,price_per_person=?,registration_enabled=?,performer_enabled=?,theme_key=?,accent_color=?,payment_instructions=?,contact_phone=?,contact_email=?,contact_whatsapp=?,contact_facebook=?,contact_instagram=?,version=version+1 WHERE id=? AND version=?",
   r.title().trim(),r.tagline(),r.aboutText(),r.eventDate(),r.eventLocation(),r.pricePerPerson(),r.registrationEnabled(),r.performerEnabled(),r.themeKey(),r.accentColor(),r.paymentInstructions(),r.contactPhone(),r.contactEmail(),r.contactWhatsapp(),r.contactFacebook(),r.contactInstagram(),id,r.version()==null?0:r.version());
  if(changed!=1) throw new ResponseStatusException(CONFLICT,"Event changed. Reload before saving.");
  return get(id);
 }
 @Transactional public EventConfig activate(long id) {
  jdbc.queryForObject("SELECT id FROM application_settings WHERE id=1 FOR UPDATE",Integer.class);
  var event=get(id);
  if(event.getEventDate()==null || event.getEventLocation()==null || event.getEventLocation().isBlank() || event.getPricePerPerson()==null)
   throw new ResponseStatusException(BAD_REQUEST,"Save the event date, venue and donation amount before activating it.");
  jdbc.update("UPDATE application_settings SET active_event_edition_id=?,active_event_year=? WHERE id=1",id,event.getEventYear()); return event;
 }
 @Transactional public boolean toggle(boolean performer) {
  var c=current(); String field=performer?"performer_enabled":"registration_enabled";
  jdbc.update("UPDATE event_editions SET "+field+"=NOT "+field+",version=version+1 WHERE id=?",c.getEventEditionId());
  return performer?get(c.getEventEditionId()).getPerformerEnabled():get(c.getEventEditionId()).getRegistrationEnabled();
 }
 public Map<String,Object> organisation() { return jdbc.queryForMap("SELECT name,story,contact_email AS \"contactEmail\",contact_phone AS \"contactPhone\",contact_facebook AS \"contactFacebook\",contact_instagram AS \"contactInstagram\" FROM organisation_profile WHERE id=1"); }
 public void organisation(String story,String phone,String email,String facebook,String instagram) {
  validateUrl(facebook); validateUrl(instagram);
  jdbc.update("UPDATE organisation_profile SET story=?,contact_phone=?,contact_email=?,contact_facebook=?,contact_instagram=? WHERE id=1",story,phone,email,facebook,instagram);
 }
 public void poster(long id,String path) { get(id); jdbc.update("UPDATE event_editions SET poster_path=?,version=version+1 WHERE id=?",path,id); }
 private void validateUrl(String value) { if(value!=null&&!value.isBlank()&&!value.matches("https?://[^\\s]+")) throw new ResponseStatusException(BAD_REQUEST,"Social links must start with https:// or http://"); }
}
