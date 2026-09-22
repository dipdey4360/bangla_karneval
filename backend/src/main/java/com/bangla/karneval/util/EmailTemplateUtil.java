package com.bangla.karneval.util;
import com.bangla.karneval.model.*;
import org.springframework.stereotype.Component;
@Component
public class EmailTemplateUtil {
 public String eventTitle(Registration r) { return r.getEventTitle()==null?"Bangla Karneval"+(r.getEventYear()==null?"":" "+r.getEventYear()):r.getEventTitle(); }
 public String buildRegistrationConfirmationEmail(Registration r) { return registration(r,"Registration received","Your registration has been received. Please follow the donation instructions below."); }
 public String buildPaymentConfirmationEmail(Registration r) { return registration(r,"Donation confirmed","Your donation has been confirmed. We look forward to seeing you."); }
 public String buildPaymentReminderEmail(Registration r) { return registration(r,"Donation reminder","Your donation is still pending. Please complete your donation to confirm your place."); }
 public String buildRegistrationStatusEmail(Registration r,String note) { return registration(r,"Registration update","Status: "+r.getPaymentStatus()+"\n"+(note==null?"":note)); }
 private String registration(Registration r,String heading,String message) {
  return base(heading,"<p>Hello "+esc(r.getPrimaryName())+",</p><h2>"+esc(eventTitle(r))+"</h2><p>"+lines(message)+"</p>"
   +details(r.getEventDateSnapshot(),r.getEventLocationSnapshot())
   +"<p><strong>Reference:</strong> "+esc(r.getReferenceCode())+"<br><strong>Participants:</strong> "+r.getParticipantCount()
   +"<br><strong>Donation:</strong> EUR "+r.getCalculatedAmount()+"<br><strong>Donation method:</strong> "+esc(String.valueOf(r.getPaymentMethod()))+"</p>"
   +"<h3>Donation instructions</h3><p>"+lines(r.getPaymentInstructionsSnapshot()==null||r.getPaymentInstructionsSnapshot().isBlank()?"Contact the organisers for donation details. Include your registration reference with your donation.":r.getPaymentInstructionsSnapshot())+"</p>");
 }
 public String buildPerformerStatusEmail(String name,String status,String note,Integer year) { return performer(name,status,note,"Bangla Karneval "+year,null,null); }
 public String buildPerformerStatusEmail(PerformerRegistration p,String note) { return performer(p.getName(),p.getApprovalStatus(),note,p.getEventTitle()==null?"Bangla Karneval "+p.getEventYear():p.getEventTitle(),p.getEventDateSnapshot(),p.getEventLocationSnapshot()); }
 private String performer(String name,String status,String note,String title,java.time.LocalDate date,String location) {
  return base("Performer application update","<p>Hello "+esc(name)+",</p><h2>"+esc(title)+"</h2>"+details(date,location)+"<p><strong>Decision:</strong> "+esc(status)+"</p><p>"+lines(note)+"</p>");
 }
 public String buildContactReplyEmail(String name,String original,String reply) { return base("Reply from Bangla Karneval e.V.","<p>Hello "+esc(name)+",</p><h3>Your message</h3><p>"+lines(original)+"</p><h3>Our reply</h3><p>"+lines(reply)+"</p>"); }
 private String details(java.time.LocalDate date,String location) { return "<p>"+(date==null?"":"<strong>Date:</strong> "+date+"<br>")+(location==null?"":"<strong>Venue:</strong> "+esc(location))+"</p>"; }
 private String base(String heading,String body) { return "<!doctype html><html><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width,initial-scale=1'></head><body style='margin:0;background:#f9f5ef;font:16px Arial,sans-serif;color:#292929'><div style='max-width:600px;margin:24px auto;background:white;padding:24px'><header style='background:#7b241c;color:white;padding:20px'><strong>Bangla Karneval e.V.</strong><h1 style='font-size:22px'>"+esc(heading)+"</h1></header>"+body+"<hr><p>Bangla Karneval e.V. · Wesseling, Germany</p></div></body></html>"; }
 private String lines(String s) { return esc(s).replace("\n","<br>"); }
 private String esc(String s) { return s==null?"":s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;"); }
}
