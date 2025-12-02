package com.construmedicis.buildtracking.email.services.impl;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;

import com.construmedicis.buildtracking.email.services.GmailAuthService;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class GmailAuthServiceImpl implements GmailAuthService {

    private static final String APPLICATION_NAME = "BuildTracking Invoice Sync";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Collections.singletonList(GmailScopes.GMAIL_READONLY);

    @Override
    public Gmail getGmailService(String credentialsPath, String tokensDirectoryPath)
            throws IOException, GeneralSecurityException {
        final NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        Credential credential = getCredentials(httpTransport, credentialsPath, tokensDirectoryPath);
        return new Gmail.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    @Override
    public String authorize(String credentialsPath, String tokensDirectoryPath) throws IOException {
        try {
            final NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            getCredentials(httpTransport, credentialsPath, tokensDirectoryPath);
            return null; // Ya está autorizado
        } catch (GeneralSecurityException e) {
            log.error("Error durante la autorización: {}", e.getMessage());
            throw new IOException("Error de seguridad durante la autorización", e);
        } catch (IOException e) {
            // Si falla, puede ser porque necesita autorización
            log.warn("Puede requerir autorización inicial: {}", e.getMessage());
            throw e;
        }
    }

    private Credential getCredentials(final NetHttpTransport httpTransport, String credentialsPath,
            String tokensDirectoryPath) throws IOException {

        // Cargar client secrets desde el archivo credentials.json
        GoogleClientSecrets clientSecrets;
        try (FileInputStream in = new FileInputStream(credentialsPath);
                InputStreamReader reader = new InputStreamReader(in)) {
            clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, reader);
        }

        // Configurar el flujo de autorización
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(tokensDirectoryPath)))
                .setAccessType("offline")
                .build();

        // Configurar el receptor local para recibir el código de autorización
        LocalServerReceiver receiver = new LocalServerReceiver.Builder()
                .setPort(8888)
                .build();

        // Autorizar y obtener credenciales
        // Si es la primera vez, abrirá el navegador para autorizar
        // Las veces siguientes, usará el token almacenado
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }
}
