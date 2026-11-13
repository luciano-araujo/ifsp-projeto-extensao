package br.edu.ifsp.tcc.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.core.io.Resource;

/** What the download route streams: the file plus the headers that describe it. */
@Data
@AllArgsConstructor
public class MaterialDownload {
    private Resource resource;
    private String contentType;
    private Long sizeBytes;
    private String contentDisposition;
}
