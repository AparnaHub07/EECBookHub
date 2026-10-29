package com.eec.bookhub.service;
import com.eec.bookhub.dto.AuthDtos.*;
import com.eec.bookhub.model.*;
import com.eec.bookhub.repository.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
@Service
public class AuthService {
  private final UserRepository users; private final PasswordResetOtpRepository otps; private final PasswordEncoder encoder; private final MailService mail;
  private final SecureRandom random=new SecureRandom();
  @Value("${app.allowed-email-domain:@eec.srmrmp.edu.in}") private String domain;
  @Value("${app.otp.expiry-minutes:10}") private int expiryMinutes;
  @Value("${app.otp.max-attempts:5}") private int maxAttempts;
  public AuthService(UserRepository users,PasswordResetOtpRepository otps,PasswordEncoder encoder,MailService mail){this.users=users;this.otps=otps;this.encoder=encoder;this.mail=mail;}
  private void validEmail(String e){ if(e==null || !e.toLowerCase().trim().endsWith(domain.toLowerCase())) throw new IllegalArgumentException("Use your official EEC college email."); }
  private Map<String,Object> userView(User u){
    Map<String,Object> m=new LinkedHashMap<>();
    m.put("id",u.id);m.put("name",u.name);m.put("email",u.email);m.put("phone",u.phone);
    m.put("upiId",u.upiId==null?"":u.upiId);m.put("dept",u.dept);m.put("year",u.year);m.put("initials",initials(u.name));
    return m;
  }
  private String initials(String n){StringBuilder s=new StringBuilder(); for(String p:n.trim().split("\s+")){if(!p.isBlank())s.append(Character.toUpperCase(p.charAt(0))); if(s.length()==2)break;} return s.toString();}
  @Transactional public Map<String,Object> register(RegisterRequest r,HttpSession s){
    String email=r.email().trim().toLowerCase(); validEmail(email); if(users.findByEmailIgnoreCase(email).isPresent()) throw new IllegalArgumentException("An account already exists for this email.");
    User u=new User();u.id="u_"+UUID.randomUUID().toString().replace("-","").substring(0,12);u.name=r.name().trim();u.email=email;u.phone=r.phone();u.upiId="";u.dept=r.dept();u.year=r.year();u.passwordHash=encoder.encode(r.password());users.save(u);s.setAttribute("userId",u.id);return Map.of("ok",true,"user",userView(u));
  }
  public Map<String,Object> login(LoginRequest r,HttpSession s){ User u=users.findByEmailIgnoreCase(r.email().trim()).orElseThrow(()->new IllegalArgumentException("Invalid email or password.")); if(!encoder.matches(r.password(),u.passwordHash)) throw new IllegalArgumentException("Invalid email or password.");s.setAttribute("userId",u.id);return Map.of("ok",true,"user",userView(u)); }
  @Transactional public Map<String,Object> requestOtp(ForgotRequest r){
    String email=r.email().trim().toLowerCase(); validEmail(email); Optional<User> maybe=users.findByEmailIgnoreCase(email);
    if(maybe.isEmpty()) return Map.of("ok",true,"message","If the account exists, an OTP has been sent.");
    User u=maybe.get(); otps.findTopByUserIdAndUsedFalseOrderByCreatedAtDesc(u.id).ifPresent(o->{o.used=true;otps.save(o);});
    String otp=String.format("%06d",random.nextInt(1_000_000)); PasswordResetOtp o=new PasswordResetOtp();o.userId=u.id;o.otpHash=encoder.encode(otp);o.expiresAt=Instant.now().plus(expiryMinutes,ChronoUnit.MINUTES);otps.save(o);
    mail.sendPasswordOtp(u.email,otp,expiryMinutes); return Map.of("ok",true,"message","OTP sent to your registered college email.");
  }
  @Transactional public Map<String,Object> reset(ResetRequest r){
    User u=users.findByEmailIgnoreCase(r.email().trim()).orElseThrow(()->new IllegalArgumentException("Invalid or expired OTP.")); PasswordResetOtp o=otps.findTopByUserIdAndUsedFalseOrderByCreatedAtDesc(u.id).orElseThrow(()->new IllegalArgumentException("Invalid or expired OTP."));
    if(o.expiresAt.isBefore(Instant.now())){o.used=true;otps.save(o);throw new IllegalArgumentException("OTP has expired. Request a new one.");}
    if(o.attempts>=maxAttempts){o.used=true;otps.save(o);throw new IllegalArgumentException("Too many attempts. Request a new OTP.");}
    if(!encoder.matches(r.otp(),o.otpHash)){o.attempts++;otps.save(o);throw new IllegalArgumentException("Invalid OTP.");}
    o.used=true;u.passwordHash=encoder.encode(r.newPassword());otps.save(o);users.save(u);return Map.of("ok",true,"message","Password reset successfully. You can log in now.");
  }
  public Map<String,Object> me(HttpSession s){Object id=s.getAttribute("userId");if(id==null)return Map.of("ok",false); return users.findById(id.toString()).<Map<String,Object>>map(u->Map.of("ok",true,"user",userView(u))).orElse(Map.of("ok",false));}
}
