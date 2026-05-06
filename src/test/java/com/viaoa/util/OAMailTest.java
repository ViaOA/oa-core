package com.viaoa.util;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.Properties;
import javax.mail.Session;
import com.viaoa.OAUnitTest;
import com.viaoa.mail.OAMail;

import test.xice.tsac3.model.oa.*;
import javax.mail.*;
import javax.mail.internet.*;
import javax.mail.util.ByteArrayDataSource;


public class OAMailTest extends OAUnitTest {

    @Test
    public void test() {
        
    }

    public static void mainAaa(String[] args) throws Exception {
        String msg = "";
        String contentType = "text/html; charset=UTF-8";
        
        OAMail m = new OAMail("smtp-auth.test.com", 3325, "test.com@test-smtp", "testPW");
        m.setDebug(true);
        // m.sendSmtp(new String[]{"test@test.com"}, null, "test@test.com", "subject", "text", null, new String[] {"c:\\temp\\cem.jpg"});
        
        
        m = new OAMail("mail.test.com", 2525, "notifications@test.com","testpw" );
        // "mail.test.com", "notifications","test3"
        m.setDebug(true);

        
        m.sendSmtp(new String[]{"test.x@xice.com"}, 
                new String[]{"jmaddx@test.com", "x123@test.com"}, "tes@testf.com", 
                "Email from test", 
                "<html><body>This is <i>another</i> email from the <h3>test</h3>, with an attachment</body></html>", null, 
                new String[] {"c:\\temp\\test.jpg"});
    }
    public static void mainB(String[] args) throws Exception {
        // mail.send("titan.test.net", "t@vtest.com", "auto@tests.com", "HTTP Post Response", s);
        // OAMail m = new OAMail("mail.test.com", 2525, "notifications@test.com","tpw" );

        String pw = "pw";
        OAMail m = new OAMail("secure.emailsrvr.com", 465, "smtp@test.com", pw);
        m.setUseSSL(true);
        m.setDebug(true);

        m.sendSmtp(
            new String[]{"test@testoa.com"}, 
            new String[]{}, 
            "info@test.com",
            "Test Email from info vj",
            "<html><body>This is a test</body></html>", "text/html; charset=UTF-8", 
            new String[] {}
        );
    }

    
    public static void main(String[] args) throws Exception {

        String fromEmail = "info@test.com";
        fromEmail = "test@test.com";        
        String subject = "test subject";
        String msg = "test message";
        String toEmail = "test@test.com";
        
        fromEmail = "testX@test.com";        
        subject = "test subject";
        msg = "test message";
        toEmail = "test@testoa.com";
        
        try {
          Properties props = System.getProperties();
          props.put("mail.smtp.auth", "true"); // required
    
          Session session = Session.getInstance(props);
          session.setDebug(true);
    
          MimeMessage message = new MimeMessage(session);
          message.setFrom(new InternetAddress(fromEmail));
          message.addRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
          
          message.setSubject(""+subject);
          
          MimeBodyPart mbp1 = new MimeBodyPart();
          mbp1.setContent( msg, "text/html; charset=UTF-8");
          
          Multipart mp = new MimeMultipart();
          mp.addBodyPart(mbp1);
          
          message.setContent(mp);
          message.setSentDate(new java.util.Date());
          message.saveChanges();
          
          Transport transport = session.getTransport("smtps");
    
          transport.connect("secure.emailsrvr.com", 465, "smtp@test.com", "test");
          transport.sendMessage(message, message.getAllRecipients());
          transport.close();

        }
        catch (Exception e) {
            System.out.println("Error sending email");
            e.printStackTrace();
        }
    }

    
}
