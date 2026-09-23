package net.jemsit.media.service;

public interface ImageProcessingService {

    byte[] processImageWithWaterMark(byte[] bytes, String filename) throws Exception;
}
