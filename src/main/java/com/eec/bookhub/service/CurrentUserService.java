package com.eec.bookhub.service;
import com.eec.bookhub.model.User;
import com.eec.bookhub.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
@Service
public class CurrentUserService {
  private final UserRepository users;
  public CurrentUserService(UserRepository users){this.users=users;}
  public User require(HttpSession session){
    Object id=session.getAttribute("userId");
    if(id==null) throw new SecurityException("Please log in.");
    return users.findById(id.toString()).orElseThrow(()->new SecurityException("Session expired."));
  }
}
