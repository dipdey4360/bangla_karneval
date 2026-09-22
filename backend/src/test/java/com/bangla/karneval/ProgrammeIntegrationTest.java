package com.bangla.karneval;
import com.bangla.karneval.model.*;
import com.bangla.karneval.repository.*;
import com.bangla.karneval.service.*;
import com.bangla.karneval.scheduler.PaymentReminderScheduler;
import com.bangla.karneval.util.EmailTemplateUtil;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import java.util.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@EnabledIfEnvironmentVariable(named="BK_MEMBERSHIP_INTEGRATION",matches="true")
@SpringBootTest(properties={"spring.datasource.url=jdbc:postgresql://127.0.0.1:15432/membership_test",
 "spring.datasource.username=postgres","spring.datasource.password=membership_test_only","spring.jpa.hibernate.ddl-auto=create-drop",
 "spring.mail.username=test@example.invalid","spring.mail.password=test-only",
 "jwt.secret=isolated-membership-test-key-at-least-64-characters-never-production","logging.level.root=WARN"})
@AutoConfigureMockMvc
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class ProgrammeIntegrationTest {
 static java.nio.file.Path posterDirectory;
 @org.springframework.test.context.DynamicPropertySource
 static void isolated(org.springframework.test.context.DynamicPropertyRegistry r) throws Exception {
  String url=IsolatedPostgres.newSchemaUrl();r.add("spring.datasource.url",()->url);
  posterDirectory=java.nio.file.Files.createTempDirectory("bk-programme-test-");r.add("app.poster-upload-dir",()->posterDirectory.toString());
 }
 @AfterAll static void cleanPosters() throws Exception {
  if(posterDirectory!=null) {try(var files=java.nio.file.Files.list(posterDirectory)){for(var file:files.toList())java.nio.file.Files.delete(file);}java.nio.file.Files.delete(posterDirectory);}
 }
 @Autowired MockMvc mvc;
 @Autowired ObjectMapper json;
 @Autowired ProgrammeService programmes;
 @Autowired EventConfigRepository configs;
 @Autowired RegistrationRepository registrations;
 @Autowired PerformerRegistrationRepository performers;
 @Autowired EmailTemplateUtil templates;
 @MockBean JavaMailSender mail;
 @MockBean EmailService emails;
 @MockBean PaymentReminderScheduler scheduler;
 Map<String,Object> request(String code,String title) {
  Map<String,Object> r=new HashMap<>();r.put("programmeCode",code);r.put("eventYear",2026);r.put("title",title);
  r.put("tagline","Our community celebration");r.put("aboutText","An event description");r.put("eventDate","2026-10-15");r.put("eventLocation","Test community hall");
  r.put("pricePerPerson",20);r.put("registrationEnabled",true);r.put("performerEnabled",true);r.put("themeKey",code);r.put("accentColor","#a92330");r.put("paymentInstructions","Test-only transfer instructions");return r;
 }
 JsonNode create(Map<String,Object> r) throws Exception {return json.readTree(mvc.perform(post("/api/admin/editions").with(user("admin").roles("ADMIN")).contentType("application/json").content(json.writeValueAsString(r))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());}
 String registration(long id,long version) {return """
 {"eventEditionId":%d,"eventVersion":%d,"eventYear":2026,"primaryName":"Test attendee","email":"test@example.invalid","primaryDateOfBirth":"1990-01-01","paymentMethod":"BANK_TRANSFER","additionalParticipants":[],"consent":true}
 """.formatted(id,version);}
 @Test void adminManagesThemesAndSameYearRecordsStaySeparate() throws Exception {
  var old=new EventConfig();old.setEventYear(2026);old.setEventDate(LocalDate.of(2026,5,15));old.setEventLocation("Original hall");old.setAboutText("Original event description");configs.save(old);
  long bangla=programmes.legacyId(2026);
  programmes.activate(bangla);
  mvc.perform(post("/api/admin/editions").contentType("application/json").content(json.writeValueAsString(request("puja","Puja 2026")))).andExpect(status().isForbidden());
  mvc.perform(get("/api/admin/editions").with(user("visitor").roles("USER"))).andExpect(status().isForbidden());
  var body=request("puja","Puja 2026");var puja=create(body);long id=puja.get("eventEditionId").asLong(),version=puja.get("version").asLong();
  assertNotEquals(create(request("bbq","Spring BBQ")).get("eventEditionId"),create(request("bbq","Summer BBQ")).get("eventEditionId"));
  mvc.perform(get("/api/config/current")).andExpect(jsonPath("$.eventEditionId").value(bangla));
  mvc.perform(get("/api/admin/editions/"+id).with(user("admin").roles("ADMIN"))).andExpect(jsonPath("$.themeKey").value("puja"));
  mvc.perform(post("/api/admin/events").with(user("admin").roles("ADMIN")).contentType("application/json")
   .content("{\"eventEditionId\":"+id+",\"eventYear\":2026,\"category\":\"FOOD\",\"title\":\"Puja food\"}")).andExpect(status().isOk());
  mvc.perform(multipart("/api/admin/gallery").param("eventEditionId",Long.toString(id)).param("url","/assets/images/test-puja.png").param("caption","Puja photo").with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
  mvc.perform(get("/api/editions/"+id+"/activities")).andExpect(jsonPath("$[0].title").value("Puja food"));
  mvc.perform(get("/api/editions/"+bangla+"/activities")).andExpect(jsonPath("$.length()").value(0));
  mvc.perform(get("/api/editions/"+id+"/gallery")).andExpect(jsonPath("$[0].caption").value("Puja photo"));
  mvc.perform(get("/api/gallery/2026")).andExpect(jsonPath("$.length()").value(0));
  mvc.perform(put("/api/admin/editions/"+id+"/activate").with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
  mvc.perform(get("/api/config/current")).andExpect(jsonPath("$.title").value("Puja 2026")).andExpect(jsonPath("$.themeKey").value("puja"));
  mvc.perform(post("/api/register/general").contentType("application/json").content(registration(bangla,0))).andExpect(status().isConflict());
  mvc.perform(post("/api/register/general").contentType("application/json").content(registration(id,version).replace("\"eventEditionId\":"+id+",", ""))).andExpect(status().isConflict());
  mvc.perform(post("/api/register/general").contentType("application/json").content(registration(id,version))).andExpect(status().isOk());
  mvc.perform(post("/api/register/performer").contentType("application/json").content("{\"eventEditionId\":"+id+",\"eventVersion\":"+version+",\"eventYear\":2026,\"name\":\"Artist\",\"email\":\"artist@example.invalid\",\"performanceType\":\"DANCE\",\"groupMembers\":[],\"consent\":true}")).andExpect(status().isOk());
  mvc.perform(get("/api/admin/registrations?eventEditionId="+id).with(user("admin").roles("ADMIN"))).andExpect(jsonPath("$.length()").value(1));
  mvc.perform(get("/api/admin/registrations?eventEditionId="+bangla).with(user("admin").roles("ADMIN"))).andExpect(jsonPath("$.length()").value(0));
  mvc.perform(get("/api/admin/performers?eventEditionId="+id).with(user("admin").roles("ADMIN"))).andExpect(jsonPath("$.length()").value(1));
  mvc.perform(get("/api/admin/dashboard/stats?eventEditionId="+bangla).with(user("admin").roles("ADMIN"))).andExpect(jsonPath("$.totalRegistrations").value(0));
  var export=mvc.perform(get("/api/admin/registrations/export?eventEditionId="+bangla).with(user("admin").roles("ADMIN"))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();assertFalse(export.contains("Test attendee"));
  body.put("version",version);body.put("title","Renamed Puja");body.put("pricePerPerson",30);
  mvc.perform(put("/api/admin/editions/"+id).with(user("admin").roles("ADMIN")).contentType("application/json").content(json.writeValueAsString(body))).andExpect(status().isOk());
  mvc.perform(put("/api/admin/editions/"+id).with(user("admin").roles("ADMIN")).contentType("application/json").content(json.writeValueAsString(body))).andExpect(status().isConflict());
  mvc.perform(post("/api/register/general").contentType("application/json").content(registration(id,version))).andExpect(status().isConflict());
  var saved=registrations.findByEventEditionId(id).get(0);assertEquals(0,saved.getCalculatedAmount().compareTo(new BigDecimal("20")));assertEquals("Puja 2026",saved.getEventTitle());
  String html=templates.buildRegistrationConfirmationEmail(saved);assertTrue(html.contains("Puja 2026"));assertTrue(html.contains("2026-10-15"));assertTrue(html.contains("Test-only transfer instructions"));assertFalse(html.contains("Renamed Puja"));
  mvc.perform(put("/api/admin/editions/"+bangla+"/activate").with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
  mvc.perform(get("/api/config/current")).andExpect(jsonPath("$.posterPath").value("/assets/images/home_background.jpg"));
  assertEquals(1,registrations.findByEventEditionId(id).size());assertEquals(1,performers.findByEventEditionId(id).size());
 }
 @Test void validatesPosterAndOrganisationAndClosedEvents() throws Exception {
  var body=request("eid","Eid test");body.put("registrationEnabled",false);body.put("performerEnabled",false);var event=create(body);long id=event.get("eventEditionId").asLong();
  mvc.perform(put("/api/admin/editions/"+id+"/activate").with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
  mvc.perform(post("/api/register/general").contentType("application/json").content(registration(id,event.get("version").asLong()))).andExpect(status().isConflict());
  mvc.perform(multipart("/api/admin/editions/"+id+"/poster").file(new MockMultipartFile("image","bad.png","image/png","not an image".getBytes())).with(user("admin").roles("ADMIN"))).andExpect(status().isBadRequest());
  var bytes=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(2,2,java.awt.image.BufferedImage.TYPE_INT_RGB),"png",bytes);
  mvc.perform(multipart("/api/admin/editions/"+id+"/poster").file(new MockMultipartFile("image","poster.png","image/png",bytes.toByteArray())).with(user("admin").roles("ADMIN"))).andExpect(status().isOk()).andExpect(jsonPath("$.posterPath").value(org.hamcrest.Matchers.startsWith("/uploads/posters/")));
  mvc.perform(put("/api/admin/organisation").with(user("admin").roles("ADMIN")).contentType("application/json").content("{\"story\":\"Shared organisation story\",\"contactEmail\":\"org@example.invalid\"}")).andExpect(status().isOk());
  mvc.perform(get("/api/organisation")).andExpect(jsonPath("$.story").value("Shared organisation story"));
  body.put("contactFacebook","javascript:alert(1)");mvc.perform(post("/api/admin/editions").with(user("admin").roles("ADMIN")).contentType("application/json").content(json.writeValueAsString(body))).andExpect(status().isBadRequest());
 }
}
