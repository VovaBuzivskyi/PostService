package faang.school.postservice.service.image;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class ImageCompressionServiceTest {

    @InjectMocks
    private ImageCompressionService imageCompressionService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(imageCompressionService, "maxSquareLength", 500);
        ReflectionTestUtils.setField(imageCompressionService, "maxRectangularLongSideLength", 800);
        ReflectionTestUtils.setField(imageCompressionService, "maxRectangularShortSideLength", 400);
    }

    @Test
    void compressImageSquareImageExceedsLimitTest() throws IOException {
        BufferedImage squareImage = new BufferedImage(1000, 1000, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(squareImage, "jpg", outputStream);
        byte[] imageData = outputStream.toByteArray();

        byte[] compressedImage = imageCompressionService.compressImage(imageData ,"jpg");

        BufferedImage resultImage = ImageIO.read(new ByteArrayInputStream(compressedImage));
        assertEquals(500, resultImage.getWidth());
        assertEquals(500, resultImage.getHeight());
    }

    @Test
    void compressImageRectangularImageExceedsLongSideLimitTest() throws IOException {
        BufferedImage rectangularImage = new BufferedImage(1000, 200, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(rectangularImage, "jpg", outputStream);
        byte[] imageData = outputStream.toByteArray();

        byte[] compressedImage = imageCompressionService.compressImage(imageData, "jpg");

        BufferedImage resultImage = ImageIO.read(new ByteArrayInputStream(compressedImage));
        assertEquals(800, resultImage.getWidth());
        assertEquals(160, resultImage.getHeight());
    }

    @Test
    void compressImageRectangularImageExceedsShortSideLimitTest() throws IOException {
        BufferedImage rectangularImage = new BufferedImage(500, 700, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(rectangularImage, "jpg", outputStream);
        byte[] imageData = outputStream.toByteArray();

        byte[] compressedImage = imageCompressionService.compressImage(imageData, "jpg");

        BufferedImage resultImage = ImageIO.read(new ByteArrayInputStream(compressedImage));
        assertEquals(400, resultImage.getWidth());
        assertEquals(560, resultImage.getHeight());
    }

    @Test
    void compressImageAlreadyValidSizeTest() throws IOException {
        BufferedImage validImage = new BufferedImage(400, 400, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(validImage, "jpg", outputStream);
        byte[] imageData = outputStream.toByteArray();

        byte[] compressedImage = imageCompressionService.compressImage(imageData, "jpg");

        assertArrayEquals(imageData, compressedImage);
    }

    @Test
    void compressImageIOExceptionTest() {
        byte[] invalidData = "not-an-image".getBytes();
        assertThrows(IOException.class, () -> imageCompressionService.compressImage(invalidData, "jpg"));
    }
}