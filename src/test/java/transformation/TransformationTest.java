package transformation;

import com.demo.azure.dto.RecognitionResponse;
import com.demo.JsonUtils;
import com.demo.azure.dto.TextLine;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static com.demo.azure.TextLinesUtils.groupWithDistance;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TransformationTest {

    @Test
    public void testJsonTransformation() throws IOException {
        RecognitionResponse recognitionResponse = readRecognitionResponse("transformation/target_response.json");
        assertNotNull(recognitionResponse);
        List<TextLine> textLines = groupWithDistance(recognitionResponse.getRecognitionResult().getLines());
        assertEquals(53, textLines.size());
    }

    public static RecognitionResponse readRecognitionResponse(String path) throws IOException {
        return JsonUtils.toObject(new String(Files.readAllBytes(getResource(path).toPath())), RecognitionResponse.class);
    }

    private static File getResource(String path) {
        ClassLoader classLoader = TransformationTest.class.getClassLoader();
        return new File(classLoader.getResource(path).getFile());
    }
}
