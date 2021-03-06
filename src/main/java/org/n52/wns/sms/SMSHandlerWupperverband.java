/***************************************************************
 /"A service by which a client may conduct asynchronous dialogues 
 (message interchanges) with one or more other services. This 
 service is useful when many collaborating services are required 
 to satisfy a client request, and/or when significant delays are 
 involved is satisfying the request. This service was defined 
 under OWS 1.2 in support of SPS operations. WNS has broad 
 applicability in many such multi-service applications.
 
 Copyright (C) 2007 by 52�North Initiative for Geospatial 
 Open Source Software GmbH

 Author: Johannes Echterhoff, University of Muenster

 Contact: Andreas Wytzisk, 52�North Initiative for Geospatial 
 Open Source Software GmbH,  Martin-Luther-King-Weg 24,
 48155 Muenster, Germany, info@52north.org

 This program is free software; you can redistribute and/or  
 modify it under the terms of the GNU General Public License 
 version 2 as published by the Free Software Foundation.

 This program is distributed in the hope that it will be useful,  
 but WITHOUT ANY WARRANTY; without even the implied warranty of  
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the  
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License 
 along with this program (see gnu-gpl v2.txt); if not, write to  the 
 Free Software Foundation, Inc., 59 Temple Place - Suite 330,  
 Boston, MA 02111-1307, USA or visit the Free Software Foundation's  
 web page, http://www.fsf.org.

 Created on: 2008-01-24

 ***************************************************************/

package org.n52.wns.sms;

import java.util.ArrayList;

import net.opengis.wns.x00.CommunicationMessageDocument;
import net.opengis.wns.x00.DoNotificationDocument;
import net.opengis.wns.x00.NotificationMessageDocument;

import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.n52.wns.WNSException;
import org.n52.wns.mail.IMailHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.x52North.wns.v2.WNSConfigDocument;

/**
 * Specific SMSHandler for the Wupperverband SMS gateway. SMS are delivered
 * by sending emails to this gateway. The behaviour follows that of the Ecall
 * SMSHandler, but phone numbers shall not be converted to international format.
 * 
 * @author Johannes Echterhoff
 * @see SMSHandlerEcall
 */
public class SMSHandlerWupperverband implements SMSHandler {

   private static Logger log = LoggerFactory.getLogger(SMSHandlerWupperverband.class);

   private IMailHandler mh = null;

   private String mailtoAdress;

   private String wnsURL;

   /**
    * SMSHandlerWupperverband will get the systems default mail handler required 
    * for sending mails.
    * 
    * @throws WNSException
    */
   public SMSHandlerWupperverband() throws WNSException {
      log.debug("Init SMSHandlerWupperverband");
      // loads the MailHandler used to send mails to the gateway
      this.mh = org.n52.wns.mail.MailHandlerFactory.getInstance();
   }

   /**
    * @see org.n52.wns.sms.SMSHandler#setProperties(org.x52North.wns.WNSConfigDocument.WNSConfig.RegisteredHandlers.SMSHandler
    *      shd, String wns)
    */
   public void setProperties(
         WNSConfigDocument.WNSConfig.RegisteredHandlers.SMSHandler shd,
         String wns) {
      log.debug("setting properties");
      this.mailtoAdress = shd.getHost();
      this.wnsURL = wns;
   }

   /**
    * sends a SMS NotificationMessage
    * 
    * @see org.n52.wns.sms.SMSHandler#sendNotificationMessage(DoNotificationDocument
    *      dnd, String[] targets, String messageID)
    */
   public void sendNotificationMessage(DoNotificationDocument dnd,
         String[] targets, String messageID) throws WNSException {
      log.debug("Trying to send NotificationMessage");

      String service = "";
      // handle request
      ArrayList validationErrors = new ArrayList();
      XmlOptions validationOptions = new XmlOptions();
      validationOptions.setErrorListener(validationErrors);

      // parse request and handle accordingly
      XmlObject xobj = null;
      ArrayList parsingErrors = new ArrayList();
      XmlOptions parsingOptions = new XmlOptions();
      parsingOptions.setErrorListener(parsingErrors);

      try {

         xobj = XmlObject.Factory.parse(dnd.getDoNotification().getMessage()
               .toString(), parsingOptions);

      } catch (XmlException e) {
         log.error(e.toString());
         throw new WNSException(e.toString());
      }
      try {

         SchemaType type = xobj.schemaType();

         if (type == NotificationMessageDocument.type) {
            NotificationMessageDocument notdoc = NotificationMessageDocument.Factory
                  .parse(dnd.getDoNotification().getMessage().toString());

            service = notdoc.getNotificationMessage()
                  .getServiceDescription().getServiceURL();
         }
         if (type == CommunicationMessageDocument.type) {
            CommunicationMessageDocument notdoc = CommunicationMessageDocument.Factory
                  .parse(dnd.getDoNotification().getMessage().toString());

            service = notdoc.getCommunicationMessage()
                  .getServiceDescription().getServiceURL();
         }
      } catch (XmlException e) {
         log.error("Error while sending Message: " + e.toString());
         throw new WNSException("Error while sending Message: "
               + e.toString());
      }

      String text = dnd.getDoNotification().getShortMessage() +
         " WNSURL: " + this.wnsURL + " MID:" + messageID;
//    String text = dnd.getDoNotification().getShortMessage();
//    String text = "Notification, MessageID:" + messageID + " WNSURL: "
//          + this.wnsURL + " ServiceURL: " + service + " Message: "
//          + dnd.getDoNotification().getShortMessage();
      for (int i = 0; i <= targets.length - 1; i++) {
         String msisdn = targets[i].toString();
         // verify format and set subject
         String to = msisdn + "@" + this.mailtoAdress;
         
         log.debug("Sending message ["+text+"] to: "+to);
         this.mh.sendExternalMessage(to, "", text);
      }

   }

}

