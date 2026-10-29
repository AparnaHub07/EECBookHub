package com.eec.bookhub.controller;

import com.eec.bookhub.model.*;
import com.eec.bookhub.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/books")
public class BookController {
    private final BookService service;
    private final CurrentUserService current;
    public BookController(BookService service,CurrentUserService current){this.service=service;this.current=current;}

    @GetMapping public List<Book> all(){return service.all();}
    @GetMapping("/{id}") public Book one(@PathVariable String id){return service.get(id);}
    @PostMapping public Book create(@RequestBody Book b,HttpSession s){return service.create(b,current.require(s));}
    @PatchMapping("/{id}/status") public Book status(@PathVariable String id,@RequestBody Map<String,String> body,HttpSession s){return service.updateStatus(id,body.get("status"),current.require(s));}
    @DeleteMapping("/{id}") public Map<String,Object> delete(@PathVariable String id,HttpSession s){service.delete(id,current.require(s));return Map.of("ok",true);}
    @GetMapping("/{id}/contact") public Map<String,Object> contact(@PathVariable String id,HttpSession s){return service.contact(id,current.require(s));}
    @GetMapping("/borrowing/{id}/contact") public Map<String,Object> borrowingContact(@PathVariable String id,@RequestParam(defaultValue="provider") String party,HttpSession s){return service.borrowingContact(id,current.require(s),party);}
}
