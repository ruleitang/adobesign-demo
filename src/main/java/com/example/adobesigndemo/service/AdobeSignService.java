package com.example.adobesigndemo.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.example.adobesigndemo.config.AdobeSignProperties;
import com.example.adobesigndemo.web.dto.SendAgreementRequest;
import com.example.adobesigndemo.web.dto.SendAgreementResponse;

@Service
public class AdobeSignService {

	private static final String MIME_TYPE_DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
	private static final String DEFAULT_MESSAGE = "Please review and sign this sample agreement to verify the Acrobat Sign for Government integration.";
	private static final String SIGNATURE_TYPE_DIGITAL = "DIGITAL_SIGNATURE";
	private static final String AGREEMENT_STATE_IN_PROCESS = "IN_PROCESS";
	private static final String RECIPIENT_ROLE_SIGNER = "SIGNER";

	private final AdobeSignProperties properties;
	private final ResourceLoader resourceLoader;
	private final AdobeSignOAuthService oAuthService;
	private final RestClient restClient;

	public AdobeSignService(AdobeSignProperties properties,
			ResourceLoader resourceLoader,
			AdobeSignOAuthService oAuthService,
			RestClient.Builder restClientBuilder) {
		this.properties = properties;
		this.resourceLoader = resourceLoader;
		this.oAuthService = oAuthService;
		Assert.notNull(properties.getBaseUri(), "adobesign.base-uri is required");
		this.restClient = restClientBuilder
				.baseUrl(properties.getBaseUri().toString())
				.build();
	}

	public SendAgreementResponse sendAgreement(SendAgreementRequest request) {
        final List<String> recipients = resolveRecipients(request);
        final Resource document = resolveDocument(request.documentPath());
        final String agreementName = StringUtils.hasText(request.agreementName())
                ? request.agreementName()
                : properties.getDefaultAgreementName();
        final String messageBody = StringUtils.hasText(request.message()) ? request.message() : DEFAULT_MESSAGE;

        FileReference documentFile = null;
		try {
			documentFile = prepareFile(document);
			final String transientDocumentId = uploadDocument(documentFile, document.getFilename());
            final AgreementCreationRequest agreementRequest = buildAgreementRequest(
                    transientDocumentId,
                    agreementName,
                    messageBody,
                    recipients
            );

			final AgreementCreationResponsePayload creationResponse = createAgreement(agreementRequest);
			final String signingUrl = fetchSigningUrl(creationResponse.id());
			final Instant expiration = creationResponse.expiration();

            return new SendAgreementResponse(
                    creationResponse.id(),
                    expiration,
                    creationResponse.url(),
                    signingUrl
            );
		} catch (IOException ex) {
			throw new AdobeSignClientException("Failed to create an Adobe Sign agreement", ex);
		} finally {
			deleteIfNecessary(documentFile);
		}
	}

    private List<String> resolveRecipients(SendAgreementRequest request) {
        final List<String> recipients = new ArrayList<>();
        if (request.recipientEmails() != null) {
            request.recipientEmails().stream()
                    .filter(StringUtils::hasText)
                    .forEach(recipients::add);
        }
        if (recipients.isEmpty() && StringUtils.hasText(request.recipientEmail())) {
            recipients.add(request.recipientEmail());
        }
        if (recipients.isEmpty() && !CollectionUtils.isEmpty(properties.getDefaultRecipientEmails())) {
            recipients.addAll(properties.getDefaultRecipientEmails());
        }
        if (recipients.isEmpty()) {
            throw new AdobeSignClientException("At least one recipient email address is required to send the test document.");
        }
        return recipients;
    }

    private Resource resolveDocument(String overridePath) {
        final String location = StringUtils.hasText(overridePath)
                ? overridePath
                : properties.getTestDocumentPath();
        Assert.hasText(location, "No document path configured for Adobe Sign test runs.");
        final Resource resource = resourceLoader.getResource(location);
        if (!resource.exists()) {
            throw new AdobeSignClientException("The document resource '" + location + "' could not be found.");
        }
        return resource;
    }

    private FileReference prepareFile(Resource resource) throws IOException {
        if (resource.isFile()) {
            return new FileReference(resource.getFile(), false);
        }
        final String suffix = determineSuffix(resource.getFilename());
        final Path tempFile = Files.createTempFile("adobesign-", suffix);
        try (InputStream inputStream = resource.getInputStream()) {
            Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
        }
        return new FileReference(tempFile.toFile(), true);
    }

    private String determineSuffix(String fileName) {
        if (StringUtils.hasText(fileName) && fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf('.'));
        }
        return ".tmp";
    }

    private String uploadDocument(FileReference document, String fileName) {
        final MultiValueMap<String, Object> multipartBody = new LinkedMultiValueMap<>();
        multipartBody.add("File-Name", fileName);
        multipartBody.add("Mime-Type", MIME_TYPE_DOCX);
        multipartBody.add("File", new FileSystemResource(document.file()));

        try {
            final TransientDocumentResponsePayload response = restClient
                    .post()
                    .uri("/transientDocuments")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .header("Authorization", bearerToken())
                    .body(multipartBody)
                    .retrieve()
                    .body(TransientDocumentResponsePayload.class);
            if (response == null || !StringUtils.hasText(response.transientDocumentId())) {
                throw new AdobeSignClientException("Adobe Sign did not return a transientDocumentId.");
            }
            return response.transientDocumentId();
        } catch (RestClientException ex) {
            throw new AdobeSignClientException("Unable to upload document to Adobe Sign", ex);
        }
    }

	private AgreementCreationRequest buildAgreementRequest(String transientDocumentId,
                                                           String agreementName,
                                                           String message,
                                                           List<String> recipients) {
        final List<ParticipantSetInfoPayload> participantSets = new ArrayList<>();
        int order = 1;
        for (String email : recipients) {
            participantSets.add(new ParticipantSetInfoPayload(
                    RECIPIENT_ROLE_SIGNER,
                    order++,
                    message,
                    Collections.singletonList(new ParticipantInfoPayload(email))
            ));
        }

        return new AgreementCreationRequest(
                Collections.singletonList(new FileInfoPayload(transientDocumentId)),
                agreementName,
                participantSets,
                SIGNATURE_TYPE_DIGITAL,
                new ExternalIdPayload("integration-" + System.currentTimeMillis()),
                message,
                AGREEMENT_STATE_IN_PROCESS
        );
    }

    private AgreementCreationResponsePayload createAgreement(AgreementCreationRequest payload) {
        try {
             final AgreementCreationResponsePayload response = restClient
                    .post()
                    .uri("/agreements")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", bearerToken())
                    .body(payload)
                    .retrieve()
                    .body(AgreementCreationResponsePayload.class);
             if (response == null || !StringUtils.hasText(response.id())) {
                throw new AdobeSignClientException("Adobe Sign did not return an agreement id.");
            }
            return response;
        } catch (RestClientException ex) {
            throw new AdobeSignClientException("Unable to create agreement via Adobe Sign REST API", ex);
        }
    }

    private String fetchSigningUrl(String agreementId) {
        try {
            final SigningUrlResponsePayload response = restClient
                    .get()
                    .uri("/agreements/{agreementId}/signingUrls", agreementId)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("Authorization", bearerToken())
                    .retrieve()
                    .body(SigningUrlResponsePayload.class);

            if (response == null || CollectionUtils.isEmpty(response.signingUrlSetInfos())) {
                return null;
            }

            return response.signingUrlSetInfos()
                    .stream()
                    .filter(Objects::nonNull)
                    .map(SigningUrlSetInfoPayload::signingUrls)
                    .filter(Objects::nonNull)
                    .flatMap(Collection::stream)
                    .map(SigningUrlPayload::esignUrl)
                    .filter(StringUtils::hasText)
                    .findFirst()
                    .orElse(null);
        } catch (RestClientException ex) {
            throw new AdobeSignClientException("Unable to retrieve signing URL from Adobe Sign", ex);
        }
    }

    private void deleteIfNecessary(FileReference reference) {
        if (reference == null || !reference.temporary()) {
            return;
        }
        try {
            Files.deleteIfExists(reference.file().toPath());
        } catch (IOException ignored) {
            // best-effort cleanup
        }
    }

    private record FileReference(File file, boolean temporary) {
    }

    private String bearerToken() {
        return "Bearer " + oAuthService.getAccessToken();
    }

    private record AgreementCreationRequest(
            Collection<FileInfoPayload> fileInfos,
            String name,
            Collection<ParticipantSetInfoPayload> participantSetsInfo,
            String signatureType,
            ExternalIdPayload externalId,
            String message,
            String state) {
    }

    private record FileInfoPayload(String transientDocumentId) {
    }

    private record ParticipantSetInfoPayload(String role, Integer order, String privateMessage, Collection<ParticipantInfoPayload> memberInfos) {
    }

    private record ParticipantInfoPayload(String email) {
    }

    private record ExternalIdPayload(String id) {
    }

    private record AgreementCreationResponsePayload(String id, String embeddedCode, Instant expiration, String url) {
    }

    private record SigningUrlResponsePayload(Collection<SigningUrlSetInfoPayload> signingUrlSetInfos) {
    }

    private record SigningUrlSetInfoPayload(Collection<SigningUrlPayload> signingUrls) {
    }

    private record SigningUrlPayload(String email, String esignUrl) {
    }

    private record TransientDocumentResponsePayload(String transientDocumentId) {
    }
}
