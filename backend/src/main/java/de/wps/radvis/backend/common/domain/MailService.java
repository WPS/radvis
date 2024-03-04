/*
 * Copyright (c) 2023 WPS - Workplace Solutions GmbH
 *
 * Licensed under the EUPL, Version 1.2 or as soon they will be approved by the European Commission - subsequent versions of the EUPL (the "Licence");
 *
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 */

package de.wps.radvis.backend.common.domain;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;

import jakarta.mail.Message.RecipientType;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MailService {

	private final JavaMailSender mailSender;
	private final String sender;
	private final List<String> wpsTestmails;
	private final String wpsTestmailsRedirectTarget;

	public MailService(JavaMailSender mailSender, String sender, List<String> wpsTestmails,
		String wpsTestmailsRedirectTarget) {
		require(mailSender, notNullValue());
		require(sender, notNullValue());
		require(wpsTestmails, notNullValue());
		require(wpsTestmailsRedirectTarget, notNullValue());

		this.mailSender = mailSender;
		this.sender = sender;
		this.wpsTestmails = wpsTestmails;
		this.wpsTestmailsRedirectTarget = wpsTestmailsRedirectTarget;
	}

	public void sendMail(List<String> empfaenger, String betreff, String inhalt) {
		this.sendMail(empfaenger, betreff, inhalt, false);
	}

	public void sendHtmlMail(List<String> empfaenger, String betreff, String inhalt) {
		this.sendMail(empfaenger, betreff, inhalt, true);
	}

	private void sendMail(List<String> empfaenger, String betreff, String inhalt, boolean sendAsHtml) {
		MimeMessage msg = mailSender.createMimeMessage();
		try {
			msg.setFrom(convertMailAddress(sender));
			msg.setRecipients(RecipientType.TO, convertMailAddresses(empfaenger));
			msg.setSubject(betreff);
			if (sendAsHtml) {
				msg.setContent(inhalt, "text/html; charset=UTF-8");
			} else {
				msg.setText(inhalt, "UTF-8");
			}
			mailSender.send(msg);
			log.info("Mail an {} Empfänger versandt: '{}'", empfaenger.size(), betreff);
		} catch (MessagingException e) {
			log.error("Mail Inhalt konnte nicht gesetzt werden", e);
		} catch (MailException e) {
			log.error("Mail konnte nicht versendet werden", e);
		}
	}

	private InternetAddress[] convertMailAddresses(List<String> addresses) {
		Set<InternetAddress> result = new HashSet<>();
		addresses.forEach(e -> {
			try {
				result.add(new InternetAddress(
					wpsTestmails.contains(e) ? wpsTestmailsRedirectTarget : e));
			} catch (AddressException ex) {
				log.error("Mail-Empfänger konnte nicht hinzugefügt werden: " + e, ex);
			}
		});
		return result.toArray(InternetAddress[]::new);
	}

	private InternetAddress convertMailAddress(String address) {
		try {
			return new InternetAddress(address, "RadVIS");
		} catch (UnsupportedEncodingException e) {
			log.error("Mailadresse konnte nicht konvertiert werden: " + address + ", " + "RadVIS", e);
		}
		return null;
	}

}
