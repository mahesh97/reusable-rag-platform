
package com.tech9.knowledge.service;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class DocumentExtractionService {

    private final Tika tika = new Tika();

    public String extractText(String filePath)
            throws IOException, TikaException {

        Path path = Path.of(filePath);

        if (!Files.exists(path)) {
            throw new IOException("Document file not found.");
        }

        try (InputStream inputStream = Files.newInputStream(path)) {
            return tika.parseToString(inputStream);
        }
    }
}
