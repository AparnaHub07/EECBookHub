package com.eec.bookhub.service;
import com.eec.bookhub.model.Notification;
import com.eec.bookhub.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import java.util.UUID;
@Service
public class NotificationService {
  private final NotificationRepository repo;
  public NotificationService(NotificationRepository repo){this.repo=repo;}
  public void send(String userId,String type,String message){ Notification n=new Notification(); n.id="nt_"+UUID.randomUUID().toString().replace("-","").substring(0,12); n.userId=userId;n.type=type;n.message=message;repo.save(n); }
}
