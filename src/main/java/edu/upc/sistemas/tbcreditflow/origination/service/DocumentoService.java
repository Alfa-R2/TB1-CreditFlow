package edu.upc.sistemas.tbcreditflow.origination.service;

import edu.upc.sistemas.tbcreditflow.common.BadRequestException;
import edu.upc.sistemas.tbcreditflow.common.HashUtil;
import edu.upc.sistemas.tbcreditflow.origination.domain.Documento;
import edu.upc.sistemas.tbcreditflow.origination.domain.DocumentoResponse;
import edu.upc.sistemas.tbcreditflow.origination.domain.TipoDocumento;
import edu.upc.sistemas.tbcreditflow.origination.repository.DocumentoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * Gestión de documentos (HU02, §4.7). Guarda el binario en {@code ./uploads/{solicitudId}/{archivo}},
 * persiste la ruta y el SHA-256 del contenido. Formatos PDF/JPG, máx 5 MB; otro ⇒ HTTP 400.
 */
@Service
public class DocumentoService {

    private static final long MAX_SIZE_BYTES = 5L * 1024 * 1024; // 5 MB
    private static final Set<String> EXTENSIONES_PERMITIDAS = Set.of("pdf", "jpg", "jpeg");
    private static final Set<String> CONTENT_TYPES_PERMITIDOS = Set.of("application/pdf", "image/jpeg");

    private final DocumentoRepository documentoRepository;
    private final SolicitudService solicitudService;
    private final Path uploadsDir;

    public DocumentoService(DocumentoRepository documentoRepository,
                            SolicitudService solicitudService,
                            @Value("${app.uploads.dir}") String uploadsDir) {
        this.documentoRepository = documentoRepository;
        this.solicitudService = solicitudService;
        this.uploadsDir = Paths.get(uploadsDir);
    }

    @Transactional
    public DocumentoResponse subir(Long solicitudId, TipoDocumento tipo, MultipartFile archivo) {
        // 404 si la solicitud no existe
        solicitudService.obtener(solicitudId);

        if (archivo == null || archivo.isEmpty()) {
            throw new BadRequestException("El archivo es obligatorio y no puede estar vacío");
        }
        if (archivo.getSize() > MAX_SIZE_BYTES) {
            throw new BadRequestException("El archivo excede el tamaño máximo permitido (5 MB)");
        }
        validarFormato(archivo);

        byte[] contenido = leerContenido(archivo);
        String hash = HashUtil.sha256(contenido);
        String rutaArchivo = guardarArchivo(solicitudId, archivo.getOriginalFilename(), contenido);

        Documento documento = new Documento(solicitudId, tipo, rutaArchivo, hash, LocalDateTime.now());
        return DocumentoResponse.from(documentoRepository.save(documento));
    }

    private void validarFormato(MultipartFile archivo) {
        String extension = extensionDe(archivo.getOriginalFilename());
        String contentType = archivo.getContentType();
        boolean extensionValida = extension != null && EXTENSIONES_PERMITIDAS.contains(extension);
        boolean contentTypeValido = contentType != null
                && CONTENT_TYPES_PERMITIDOS.contains(contentType.toLowerCase());
        if (!extensionValida && !contentTypeValido) {
            throw new BadRequestException("Formato no permitido. Solo se aceptan PDF o JPG");
        }
    }

    private String extensionDe(String filename) {
        if (filename == null) {
            return null;
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return null;
        }
        return filename.substring(dot + 1).toLowerCase();
    }

    private byte[] leerContenido(MultipartFile archivo) {
        try {
            return archivo.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer el archivo", e);
        }
    }

    private String guardarArchivo(Long solicitudId, String originalFilename, byte[] contenido) {
        try {
            Path directorio = uploadsDir.resolve(String.valueOf(solicitudId));
            Files.createDirectories(directorio);
            // Solo el nombre del archivo, evitando path traversal.
            String nombreSeguro = Paths.get(originalFilename == null ? "archivo" : originalFilename)
                    .getFileName().toString();
            Path destino = directorio.resolve(nombreSeguro);
            Files.write(destino, contenido);
            return destino.toString();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar el archivo en disco", e);
        }
    }
}
