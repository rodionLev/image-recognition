package transformation;

import com.demo.azure.AzureWalmartTransformer;
import com.demo.azure.dto.RecognitionResponse;
import com.demo.azure.dto.TextLine;
import com.demo.dto.Receipt;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static com.demo.azure.TextLinesUtils.groupWithDistance;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static transformation.TransformationTest.readRecognitionResponse;

public class AzureWalmartTransformerTest {

    @Test
    public void testTransformation() throws IOException {

        RecognitionResponse recognitionResponse = readRecognitionResponse("transformation/wallmart_response.json");
        recognitionResponse.getRecognitionResult().setLines(groupWithDistance(recognitionResponse.getRecognitionResult().getLines()).toArray(new TextLine[]{}));
        Receipt transform = AzureWalmartTransformer.transform(recognitionResponse);
        assertNotNull(transform.getAddress());

        assertEquals("303 -666-0340 Mgr : ANNIE SIMMONS, 745 US HIGHWAY 287, LAFAYETTE CO 80026", transform.getAddress());
        assertEquals(15, transform.getReceiptItems().size());
    }
}
