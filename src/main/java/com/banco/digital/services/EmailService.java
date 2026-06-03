package com.banco.digital.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        String resetUrl = "http://localhost:5173/reset-password/" + resetToken;
        
        System.out.println("================================");
        System.out.println("SIMULACIÓN DE CORREO ENVIADO");
        System.out.println("Para: " + toEmail);
        System.out.println("Asunto: Recuperación de contraseña");
        System.out.println("Enlace: " + resetUrl);
        System.out.println("================================");
        
        // mailSender.send(message); // Comentado para evitar errores locales
    }

    public void sendMfaEmail(String toEmail, String code) {
        System.out.println("================================");
        System.out.println("SIMULACIÓN DE MFA ENVIADO");
        System.out.println("Para: " + toEmail);
        System.out.println("Código OTP: " + code);
        System.out.println("Válido por 5 minutos");
        System.out.println("================================");
    }
}
