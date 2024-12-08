package faang.school.postservice.service.image;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import net.coobird.thumbnailator.Thumbnails;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

@Service
public class ImageCompressionService {

    @Value("${file-upload.post.image-size-limits.square.max-length}")
    private int maxSquareLength;

    @Value("${file-upload.post.image-size-limits.rectangular.max-long-side-length}")
    private int maxRectangularLongSideLength;

    @Value("${file-upload.post.image-size-limits.rectangular.max-short-side-length}")
    private int maxRectangularShortSideLength;

    public byte[] compressImage(byte[] imageData, String extension) throws IOException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
        if (image == null) {
            throw new IOException("Failed to read the image from the byte array.");
        }

        int width = image.getWidth();
        int height = image.getHeight();

        int maxWidth, maxHeight;
        if (width != height) {
            maxWidth = width > height ? maxRectangularLongSideLength : maxRectangularShortSideLength;
            maxHeight = width > height ? maxRectangularShortSideLength : maxRectangularLongSideLength;
        } else {
            maxWidth = maxSquareLength;
            maxHeight = maxSquareLength;
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        if (width > maxWidth || height > maxHeight) {
            Thumbnails.of(image)
                    .size(maxWidth, maxHeight)
                    .outputFormat(extension)
                    .toOutputStream(outputStream);
        } else {
            return imageData;
        }

        return outputStream.toByteArray();
    }
}
