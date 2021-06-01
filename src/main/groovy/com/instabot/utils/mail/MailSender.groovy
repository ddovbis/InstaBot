package com.instabot.utils.mail

import com.instabot.config.InstaBotConfig
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.ini4j.Wini
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.DependsOn
import org.springframework.stereotype.Component

import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart

@Component
class MailSender {
    public static final Logger LOG = LogManager.getLogger(MailSender.class)

    @Autowired
    private InstaBotConfig initializeInstaBotConfig

    private boolean isSmtpEnabled
    private Session session
    private String host
    private Integer port
    private String username
    private String password
    private String recipient

    @Bean("initializeMailSender")
    @DependsOn("initializeInstaBotConfig")
    protected initialize() {
        LOG.info("Initialize MailSender")

        configureSmpt()
        if (!isSmtpEnabled) {
            LOG.info("SMTP is not enabled; MailSender will not be initialized")
            return
        }
        Properties smtpProperties = getSmtpProperties()
        setSession(smtpProperties)
    }

    private void configureSmpt() {
        LOG.info("Configure smtp")

        if (initializeInstaBotConfig == null || initializeInstaBotConfig.getIniFile() == null) {
            return
        }
        Wini iniFile = initializeInstaBotConfig.getIniFile()

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

    void setSession(Properties smtpProperties) {
        session = Session.getInstance(smtpProperties, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password)
            }
        })
        session.setDebug(false)
    }

    /**
     * Sends an email using default smtp configuration and provided {@param subject}, {@param text}
     * @param subject - the title of the email
     * @param text - the text to be added to email body
     */
    void send(String subject, String text) {
        send(subject, text, [])
    }

    /**
     * Sends an email using default smtp configuration and provided {@param subject}, {@param text}, and (a single) {@param attachment}
     * @param subject - the title of the email
     * @param text - the text to be added to email body
     * @param attachment - the file to be attached to the email body

     */
    void send(String subject, String text, File attachment) {
        send(subject, text, [attachment])
    }

    /**
     * Sends an email using default smtp configuration and provided {@param subject}, {@param text}, and {@param attachments}
     * @param subject - the title of the email
     * @param text - the text to be added to email body
     * @param attachments - the files to be attached to the email body
     */
    void send(String subject, String text, List<File> attachments) {
        if (!isSmtpEnabled) {
            LOG.error("SMTP is not enabled; email will not be sent")
            return
        }

        try {
            MimeMessage message = getBasicMessage(subject)

            Multipart content = new MimeMultipart()
            setText(content, text)
            setAttachments(content, attachments)
            message.setContent(content)

            LOG.info("Send email with subject '$subject' to $recipient (attachments: ${attachments.size()})")
            Transport.send(message)
            LOG.info("Email sent successfully")
        } catch (MessagingException me) {
            LOG.error("Could not send email due to MessagingException:", me)
        }
    }

    /**
     * @param subject - of the email
     * @return - a basic message containing session, origin, detination, and {@param subject}
     */
    private MimeMessage getBasicMessage(String subject) {
        MimeMessage message = new MimeMessage(session)
        message.setFrom(new InternetAddress(username))
        message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient))
        message.setSubject(subject)
        return message
    }

    /**
     * Adds given text to the body of an email
     * @param content - {@link MimeMultipart} instance where the {@param text} should be added to; represents the body of an email
     * @param text - the text to be added to {@param content}
     */
    private void setText(MimeMultipart content, String text) {
        MimeBodyPart textPart = new MimeBodyPart()
        textPart.setText(text)
        content.addBodyPart(textPart)
    }

    /**
     * Attaches provided files to the body of an email
     * @param content - {@link MimeMultipart} instance where the text should be added to; represents the body of an email
     * @param attachments - the files to be attached to {@param content}
     */
    private void setAttachments(MimeMultipart content, List<File> attachments) {
        for (File attachment : attachments) {
            MimeBodyPart attachmentPart = new MimeBodyPart()
            attachmentPart.attachFile(attachment)
            content.addBodyPart(attachmentPart)
        }
    }
}

