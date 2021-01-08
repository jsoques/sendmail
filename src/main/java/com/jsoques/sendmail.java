/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jsoques;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Properties;
import java.util.logging.*;

/**
 *
 * @author jsoques
 */
public class sendmail {

    private final static Logger LOGGER = Logger.getLogger(sendmail.class.getName());

    public static void main(String[] args) throws Exception {

        LOGGER.setLevel(Level.INFO);
        FileHandler fileTxt;
        SimpleFormatter formatterTxt;

        // get the global logger to configure it
        Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

        // suppress the logging output to the console
        Logger rootLogger = Logger.getLogger("");
        Handler[] handlers = rootLogger.getHandlers();
        if (handlers[0] instanceof ConsoleHandler) {
            rootLogger.removeHandler(handlers[0]);
        }

        logger.setLevel(Level.INFO);

        String cpath = new File(sendmail.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getParent();

        //cpath = "/tmp/sendmailkl";
        String logdir = cpath + File.separator + "log";
        File dir = new File(logdir);
        if (!dir.exists()) {
            if (!dir.mkdir()) {
                throw new Exception("Can not create folder 'log'!!!");
            }
        }

        //Configuration file
        File cfile = new File(cpath + File.separator + "config.properties");

        if (!cfile.exists()) {
            try (OutputStream output = new FileOutputStream(cfile)) {

                Properties prop = new Properties();
                prop.setProperty("fromemail", "put_your_gmail_here");
                prop.setProperty("password", "your_gmail_password");
                prop.store(output, null);
            }
        }

        String fromemail;
        String password;
        try (InputStream input = new FileInputStream(cfile)) {

            Properties prop = new Properties();
            // load a properties file
            prop.load(input);

            fromemail = prop.getProperty("fromemail");
            password = prop.getProperty("password");
            System.out.println("FromEmail: " + fromemail);
            System.out.println("Password: " + password);
        }

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        Date date = new Date(System.currentTimeMillis());

        fileTxt = new FileHandler(cpath + File.separator + "log" + File.separator + "log" + formatter.format(date) + ".txt", Boolean.TRUE);

        //System.out.println("CPATH: " + cpath);
        // create a TXT formatter
        formatterTxt = new SimpleFormatter();
        fileTxt.setFormatter(formatterTxt);
        logger.addHandler(fileTxt);

        String[] params = args;

        logger.log(Level.INFO, "Parameters {0}\n Length: {1}", new Object[]{Arrays.toString(params), params.length});
        if (params.length < 3) {

            System.err.println("This program has to be called with arguments!\r\n\r\nExample: \r\njava -jar SendMailJS.jar tomail \"subject\" \"body\" [attachment] [replyto:\"replyto email\"]\r\n*attachment is optional and reply to is also optional.\r\n");
            System.err.println("\r\nThere can be more attachement parameters passed as arguments and they will be appended to the email.\r\nExample: \r\njava -jar SendMailJS.jar tomail \"subject\" \"body\" attachment attachment2 attachmentN replyto:person@email");

            logger.log(Level.SEVERE, "Insufficient paramters passed to program! Arguments: {0}", Arrays.toString(params));
            //AnsiConsole.systemUninstall();
            return;
        }

        String tomail = params[0].toLowerCase();
        String subject = params[1].trim();
        String body = params[2];
        String attachment;
        String replyto = "";
        String frommail = fromemail;

        //System.out.println("HTML Body:\n" + unescapedString + "\n");
        body = org.jsoup.parser.Parser.unescapeEntities(body, true);

        if (params[params.length - 1].contains("replyto:")) {
            replyto = params[params.length - 1].replace("replyto:", "");
            System.out.println("Replyto = " + replyto);
            params = Arrays.copyOf(params, params.length - 1);
        }

        StringBuilder attachments;

        // Assuming you are sending email from through gmails smtp
        String host = "smtp.gmail.com";

        // Get system properties
        Properties properties = System.getProperties();

        // Setup mail server
        properties.put("mail.smtp.host", host);
        properties.put("mail.smtp.port", "587"); // tls 587 ssl 465
        //properties.put("mail.smtp.ssl.enable", "true"); //SSL
        properties.put("mail.smtp.starttls.enable", "true"); //TLS
        properties.put("mail.smtp.auth", "true");

        // Get the Session object.// and pass username and password
        Session session = Session.getInstance(properties, new javax.mail.Authenticator() {

            protected PasswordAuthentication getPasswordAuthentication() {

                return new PasswordAuthentication(frommail, password);

            }

        });

        // Used to debug SMTP issues
        session.setDebug(true);

        try {
            // Create a default MimeMessage object.
            MimeMessage message = new MimeMessage(session);

            //logger.log(Level.INFO, "Message subject: " + message.getSubject());
            if (!replyto.equals("")) {
                message.setReplyTo(new javax.mail.Address[]{
                    new javax.mail.internet.InternetAddress(replyto)
                });
            }

            // Set From: header field of the header.
            message.setFrom(new InternetAddress(frommail));

            // Set To: header field of the header.
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(tomail));

            // Set Subject: header field
            message.setSubject(subject);

            if (params.length == 3) {
                // Now set the actual message
                //message.setText(body);
                message.setContent(body, "text/html");
                //logger.log(Level.INFO, "Sending without attachment");
            } else {
                try {
                    System.out.println("Params: " + Arrays.toString(new Object[]{Arrays.toString(params), params.length}));
                    System.out.println(params[3]);
                    File attachfile = new File(params[3].replace('"', ' ').trim());
                    boolean exists = attachfile.exists();
                    System.out.println("Attachement exists? " + exists);
                    attachment = attachfile.getAbsolutePath();
                    System.out.println("Attachment: " + attachment);

                    Multipart multipart = new MimeMultipart();
                    MimeBodyPart attachmentPart = new MimeBodyPart();
                    MimeBodyPart textPart = new MimeBodyPart();

                    try {

                        File f = new File(attachment);

                        attachmentPart.attachFile(f);
                        textPart.setText(body);
                        multipart.addBodyPart(textPart);
                        multipart.addBodyPart(attachmentPart);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    message.setContent(multipart);

                    logger.log(Level.INFO, "Sending with attachment: " + params[3]);
                    attachments = new StringBuilder("Sending with attachment(s): \r\n " + params[3]);

                    if (params.length > 4) {
                        File f;
                        for (int a = 4; a < params.length; a++) {
                            attachment = params[a];
                            //message.addAttachment(attachment);
                            f = new File(attachment);
                            attachmentPart = new MimeBodyPart();
                            attachmentPart.attachFile(f);
                            multipart.addBodyPart(attachmentPart);
                            attachments.append("\r\n ").append(params[a]);
                        }
                    }
                    logger.log(Level.INFO, attachments.toString());
                } catch (Exception ex) {
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    ex.printStackTrace(pw);
                    logger.log(Level.SEVERE, "Error trying to send email.\r\n Detail: {0}\r\n----------------------------------------------------------------------------------", sw.toString());
                    throw ex;
                }
            }

            System.out.println("sending...");
            // Send message
            Transport.send(message);
            System.out.println("Sent message successfully....");
        } catch (MessagingException mex) {
            mex.printStackTrace();
        }

    }
}
