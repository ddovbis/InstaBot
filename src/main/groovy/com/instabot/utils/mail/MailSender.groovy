package com.instabot.utils.mail

import com.instabot.config.InstaBotConfig
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.ini4j.Wini
import org.springframework.context.annotation.DependsOn
import org.springframework.stereotype.Component

import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

@Component
@DependsOn("initializeInstaBotConfig")
class MailSender {
    public static final Logger LOG = LogManager.getLogger(MailSender.class)

    // TODO Enable @Autowired and fix NPE due failed @Autowired initialization
    // @Autowired
    InstaBotConfig instaBotConfig = new InstaBotConfig()

    boolean isSmtpEnabled
    Session session
    String host
    Integer port
    String username
    String password
    String recipient

    MailSender() {
        if (!isSmtpEnabled) {
            LOG.info("SMTP is not enabled; MailSender will not be initialized")
            return
        }
        LOG.info("Initialize MailSender")
        configureSmpt()
        Properties smtpProperties = getSmtpProperties()
        setSession(smtpProperties)
    }

    private void configureSmpt() {
        Wini iniFile = instaBotConfig.getIniFile()

        isSmtpEnabled = iniFile.get("smtp", "enable", Boolean.class) as boolean
        if (isSmtpEnabled) {
            host = iniFile.get("smtp", "host", String.class)
            port = iniFile.get("smtp", "port", Integer.class)
            username = iniFile.get("smtp", "username", String.class)
            password = iniFile.get("smtp", "password", String.class)
            recipient = iniFile.get("smtp", "recipient", String.class)
        }
    }

    private Properties getSmtpProperties() {
        Properties smtpProperties = new Properties()
        smtpProperties.put("mail.smtp.host", host)
        smtpProperties.put("mail.smtp.port", port)
        smtpProperties.put("mail.smtp.ssl.enable", true)
        smtpProperties.put("mail.smtp.auth", true)
        return smtpProperties
    }

    private void setSession(Properties smtpProperties) {
        session = Session.getInstance(smtpProperties, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password)
            }
        })
        session.setDebug(false)
    }

    void send(String subject, String body) {
        if (!isSmtpEnabled) {
            LOG.error("SMTP is not enabled; email will not be sent")
            return
        }

        try {
            LOG.info "Send email with subject '$subject' to $recipient"
            MimeMessage message = new MimeMessage(session)
            message.setFrom(new InternetAddress(username))
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient))
            message.setSubject(subject)
            message.setText(body)
            Transport.send(message)
            LOG.info "Email sent successfully"
        } catch (MessagingException me) {
            LOG.error("Could not send email due to MessagingException:", me)
        }
    }

}