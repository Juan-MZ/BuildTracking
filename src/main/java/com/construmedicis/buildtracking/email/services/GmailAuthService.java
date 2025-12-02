package com.construmedicis.buildtracking.email.services;

import java.io.IOException;
import java.security.GeneralSecurityException;

import com.google.api.services.gmail.Gmail;

public interface GmailAuthService {

    /**
     * Obtiene un cliente de Gmail autenticado usando las credenciales
     * y tokens almacenados en las rutas proporcionadas.
     * 
     * @param credentialsPath Ruta al archivo credentials.json
     * @param tokensDirectoryPath Ruta al directorio donde se almacenan los tokens
     * @return Cliente de Gmail autenticado
     * @throws IOException Si hay problemas leyendo archivos
     * @throws GeneralSecurityException Si hay problemas con la autenticación
     */
    Gmail getGmailService(String credentialsPath, String tokensDirectoryPath) 
            throws IOException, GeneralSecurityException;

    /**
     * Autoriza la aplicación con Gmail. Si es la primera vez, generará una URL
     * para que el usuario autorice la aplicación. Los tokens se guardarán en
     * tokensDirectoryPath para futuros usos.
     * 
     * @param credentialsPath Ruta al archivo credentials.json
     * @param tokensDirectoryPath Ruta al directorio donde se almacenarán los tokens
     * @return URL de autorización si es primera vez, null si ya está autorizado
     * @throws IOException Si hay problemas leyendo archivos
     */
    String authorize(String credentialsPath, String tokensDirectoryPath) throws IOException;
}
