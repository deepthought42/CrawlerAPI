package com.qanairy.services;

import org.springframework.stereotype.Service;

@Service
public interface EmailService {
 
    public void sendSimpleMessage(String to, String subject, String text);
    
    public void sendHtmlMessage(String to, String subject, String text);
}
