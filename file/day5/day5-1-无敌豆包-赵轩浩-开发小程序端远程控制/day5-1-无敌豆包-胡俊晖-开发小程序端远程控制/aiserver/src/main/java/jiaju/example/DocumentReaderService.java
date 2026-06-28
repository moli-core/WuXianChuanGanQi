package jiaju.example;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DocumentReaderService {

    /**
     * 从文件路径读取文档
     */
    public String readDocument(String filePath) {
        Resource resource = new FileSystemResource(filePath);
        TikaDocumentReader reader = new TikaDocumentReader(resource);
        List<Document> documents = reader.read();
        
        // 提取文本内容
        return documents.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n"));
    }

    /**
     * 读取文档并返回 Document 对象列表
     */
    public List<Document> readDocuments(String filePath) {
        Resource resource = new FileSystemResource(filePath);
        TikaDocumentReader reader = new TikaDocumentReader(resource);
        return reader.read();
    }

    /**
     * 从 MultipartFile 读取文档（用于文件上传）
     */
    public String readMultipartFile(MultipartFile file) throws IOException {
        File tempFile = File.createTempFile("upload_", "_" + file.getOriginalFilename());
        file.transferTo(tempFile);
        
        Resource resource = new FileSystemResource(tempFile);
        TikaDocumentReader reader = new TikaDocumentReader(resource);
        List<Document> documents = reader.read();
        
        // 删除临时文件
        tempFile.delete();
        
        return documents.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n"));
    }

    /**
     * 读取文档并获取元数据
     */
    public DocumentInfo readDocumentWithMetadata(String filePath) {
        Resource resource = new FileSystemResource(filePath);
        TikaDocumentReader reader = new TikaDocumentReader(resource);
        List<Document> documents = reader.read();
        
        if (documents.isEmpty()) {
            return null;
        }
        
        Document doc = documents.get(0);
        DocumentInfo info = new DocumentInfo();
        info.setContent(doc.getText());
        info.setMetadata(doc.getMetadata());
        info.setFileName(new File(filePath).getName());
        
        return info;
    }

    /**
     * 文档信息封装类
     */
    public static class DocumentInfo {
        private String content;
        private String fileName;
        private java.util.Map<String, Object> metadata;

        // getters and setters
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public java.util.Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(java.util.Map<String, Object> metadata) { this.metadata = metadata; }
    }
}