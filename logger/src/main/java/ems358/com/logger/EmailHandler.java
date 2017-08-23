package ems358.com.logger;

import android.util.Log;

import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

/**
 * Created by hcqi on.
 * Des:
 * Date: 2017/8/3
 */
public class EmailHandler {
    /**
     * 邮件发送程序
     *
     * @throws Exception
     */

    public void sendEmail(String filePath,String host, String address, String from, String password, String to, String port, String subject, String content) throws Exception {
        Multipart multiPart;
        Properties props = System.getProperties();
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.user", address);
        props.put("mail.smtp.password", password);
        props.put("mail.smtp.port", port);
        props.put("mail.smtp.auth", "true");
        Log.i("Check", "done pops");

        Session session = Session.getDefaultInstance(props, null);
        DataHandler handler = new DataHandler(new FileDataSource(filePath));
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(from));
        message.setDataHandler(handler);
        Log.i("Check", "done sessions");
        multiPart = new MimeMultipart();
        InternetAddress toAddress;
        toAddress = new InternetAddress(to);
        message.addRecipient(Message.RecipientType.TO, toAddress);
        Log.i("Check", "added recipient");
        message.setSubject(subject);
        message.setContent(multiPart);
        message.setText(content);

        Log.i("check", "transport");
        Transport transport = session.getTransport("smtp");
        Log.i("check", "connecting");
        transport.connect(host, address, password);
        Log.i("check", "wana send");
        transport.sendMessage(message, message.getAllRecipients());
        transport.close();
        Log.i("check", "sent");

    }

//    public static class EmailBuilder {
//        //邮件服务器 如：smtp.qq.com
//        private String host;
//        //发送邮件的地址 如：545099227@qq.com
//        private String address;
//        // 来自： wsx2miao@qq.com
//        private String from;
//        //邮箱密码
//        private String password;
//        //接收人
//        private String to;
//        //端口（QQ:25）
//        private String port;
//        //邮件主题
//        private String subject;
//        // 邮件内容
//        private String content;
//
//        public EmailBuilder() {
//        }
//
//        public EmailBuilder(String host, String address, String from, String password, String to, String port, String subject, String content) {
//            this.host = host;
//            this.address = address;
//            this.from = from;
//            this.password = password;
//            this.to = to;
//            this.port = port;
//            this.subject = subject;
//            this.content = content;
//        }
//
//        public String getHost() {
//            return host;
//        }
//
//        public EmailBuilder setHost(String host) {
//            this.host = host;
//            return this;
//        }
//
//        public String getAddress() {
//            return address;
//        }
//
//        public EmailBuilder setAddress(String address) {
//            this.address = address;
//            return this;
//        }
//
//        public String getFrom() {
//            return from;
//        }
//
//        public EmailBuilder setFrom(String from) {
//            this.from = from;
//            return this;
//        }
//
//        public String getPassword() {
//            return password;
//        }
//
//        public EmailBuilder setPassword(String password) {
//            this.password = password;
//            return this;
//        }
//
//        public String getTo() {
//            return to;
//        }
//
//        public EmailBuilder setTo(String to) {
//            this.to = to;
//            return this;
//        }
//
//        public String getPort() {
//            return port;
//        }
//
//        public EmailBuilder setPort(String port) {
//            this.port = port;
//            return this;
//        }
//
//        public String getSubject() {
//            return subject;
//        }
//
//        public EmailBuilder setSubject(String subject) {
//            this.subject = subject;
//            return this;
//        }
//
//        public String getContent() {
//            return content;
//        }
//
//        public EmailBuilder setContent(String content) {
//            this.content = content;
//            return this;
//        }
//
//        public EmailHandler build() {
//            return new EmailHandler(this);
//        }
//
//    }
}
