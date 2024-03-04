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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mail.javamail.JavaMailSender;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

public class MailServiceTest {

	private MailService service;

	@Mock
	private JavaMailSender mailSender;

	@Mock
	private MimeMessage mimeMessage;

	@BeforeEach
	public void setUp() {
		MockitoAnnotations.openMocks(this);
		when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

		service = new MailService(mailSender,
			"testSender@testRadvis.de",
			List.of("lasgo@testRadvis.de", "jupiter@testRadvis.de"),
			"testRedirectTarget@testRadvis.de");
	}

	@Test
	public void testSendMail_versendetMail() throws MessagingException {
		// arrange
		List<String> empfaenger = List.of("batman@testRadvis.dcu", "spiderman@testRadvis.mcu");
		String betreff = "Justice League beitreten?";
		String inhalt = "Hey Spiderman, willst du nicht lieber der Justice League beitreten?";

		// act
		service.sendMail(empfaenger, betreff, inhalt);

		// assert
		verify(mimeMessage).setSubject(betreff);
		verify(mimeMessage).setText(inhalt, "UTF-8");
		verify(mailSender).send(mimeMessage);
	}

	@Test
	void testSendMail_MailsAnTestbenutzerAufWpsUmleiten() throws MessagingException {
		// arrange
		List<String> ursprunglicheEmpfaenger = List.of("lasgo@testRadvis.de", "jupiter@testRadvis.de",
			"testbert.testmann@testRadvis.de");

		String betreff = "bla";
		String inhalt = "bla bla bla";

		// act
		service.sendMail(ursprunglicheEmpfaenger, betreff, inhalt);

		// assert
		InternetAddress[] modifizierteEmpfaenger = new InternetAddress[2];
		modifizierteEmpfaenger[0] = new InternetAddress("testRedirectTarget@testRadvis.de");
		modifizierteEmpfaenger[1] = new InternetAddress("testbert.testmann@testRadvis.de");

		verify(mimeMessage).setSubject(betreff);
		verify(mimeMessage).setText(inhalt, "UTF-8");
		verify(mimeMessage).setRecipients(Message.RecipientType.TO, modifizierteEmpfaenger);
		verify(mailSender).send(mimeMessage);
	}
}
