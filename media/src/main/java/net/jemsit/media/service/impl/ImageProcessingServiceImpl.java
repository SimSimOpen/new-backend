package net.jemsit.media.service.impl;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import net.jemsit.common.exceptions.UserException;
import net.jemsit.media.service.ImageProcessingService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Service
@Slf4j
public class ImageProcessingServiceImpl implements ImageProcessingService {

    private Path logoTempPath;

    @PostConstruct
    public void init() throws Exception {
        ClassPathResource logo = new ClassPathResource("SimSim-Logo.png");
        logoTempPath = Files.createTempFile("simsim-logo-", ".png");
        Files.copy(logo.getInputStream(), logoTempPath, StandardCopyOption.REPLACE_EXISTING);
        log.info("Logo extracted to: {}", logoTempPath);
    }

    @PreDestroy
    public void cleanup() throws Exception {
        Files.deleteIfExists(logoTempPath);
    }

    @Override
    public byte[] processImageWithWaterMark(byte[] bytes, String filename) throws Exception {
        Path inputPath = Files.createTempFile("upload-", getExt(filename));
        Path outputPath = Files.createTempFile("processed-", ".webp");
        try {
            Files.write(inputPath, bytes);
            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg",
                    "-i", inputPath.toString(),
                    "-i", logoTempPath.toString(),
                    "-filter_complex",
                    "[1:v]scale=150:-1[logo];" +
                            "[0:v]scale=1920:-1,eq=brightness=0.05:contrast=1.1:saturation=1.2[img];" +
                            "[img][logo]overlay=W-w-20:H-h-20",
                    "-quality", "82",
                    "-y",
                    outputPath.toString()
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            String output = new String(process.getInputStream().readAllBytes());
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.error("ffmpeg failed with exit code {}: {}", exitCode, output);
                throw new UserException("Image processing failed: " + output);
            }
            return Files.readAllBytes(outputPath);
        } finally {
            Files.deleteIfExists(inputPath);
            Files.deleteIfExists(outputPath);
        }
    }

    private String getExt(String filename) {
        if (filename == null) return ".jpg";
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot) : ".jpg";
    }
}
