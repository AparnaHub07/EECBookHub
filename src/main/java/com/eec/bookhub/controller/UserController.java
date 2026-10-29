package com.eec.bookhub.controller;

import com.eec.bookhub.model.User;
import com.eec.bookhub.repository.UserRepository;
import com.eec.bookhub.service.CurrentUserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserRepository users;
    private final CurrentUserService current;
    public UserController(UserRepository users,CurrentUserService current){this.users=users;this.current=current;}

    @GetMapping
    public List<Map<String,Object>> all(){
        return users.findAll().stream().map(u->Map.<String,Object>of("id",u.id,"name",u.name,"dept",u.dept,"year",u.year)).toList();
    }

    @PatchMapping("/me")
    public Map<String,Object> update(@RequestBody Map<String,String> body,HttpSession s){
        User u=current.require(s);
        if(body.containsKey("phone")){
            String phone=body.getOrDefault("phone","").trim();
            if(!phone.matches("^[0-9]{10}$"))throw new IllegalArgumentException("Enter a valid 10-digit phone number.");
            u.phone=phone;
        }
        if(body.containsKey("upiId")){
            String upi=body.getOrDefault("upiId","").trim();
            if(!upi.isBlank() && !upi.matches("^[A-Za-z0-9._-]{2,}@[A-Za-z0-9.-]{2,}$"))
                throw new IllegalArgumentException("Enter a valid UPI ID, for example name@bank.");
            u.upiId=upi;
        }
        users.save(u);
        return Map.of("ok",true,"user",view(u));
    }

    private Map<String,Object> view(User u){
        Map<String,Object> m=new LinkedHashMap<>();
        m.put("id",u.id);m.put("name",u.name);m.put("email",u.email);m.put("phone",u.phone);
        m.put("upiId",u.upiId==null?"":u.upiId);m.put("dept",u.dept);m.put("year",u.year);
        String initials=u.name==null||u.name.isBlank()?"U":u.name.substring(0,1).toUpperCase();
        m.put("initials",initials);return m;
    }
}
