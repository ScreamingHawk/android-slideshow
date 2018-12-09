package link.standen.michael.slideshow.util;

import android.content.Context;
import android.net.Uri;
import android.test.suitebuilder.annotation.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;

import link.standen.michael.slideshow.model.FileItem;

import static org.junit.Assert.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Uri.class})
public class FileItemHelperTest {

    private FileItemHelper fileItemHelper;

    @Mock
    private Context mockContext;

    @Test
    @SmallTest
    public void createFileItem() throws Exception {
        fileItemHelper = new FileItemHelper(mockContext);

        testFileCreate("testFileCreate", "testDir");
        testFileCreate("", "");
        testFileCreate(" ", " ");
        testFileCreate("$51", "%^&");
        testFileCreate("$51", "file://thisIsMyFullPath");
    }

    private void testFileCreate(String fileName, String dirFullPath) {
        File mockedFile = Mockito.mock(File.class);
        Mockito.when(mockedFile.getName()).thenReturn(fileName);
        Mockito.when(mockedFile.getAbsolutePath()).thenReturn(dirFullPath);

        FileItem tempFile = fileItemHelper.createFileItem(mockedFile);
        assertEquals(fileName, tempFile.getName());
        assertEquals(dirFullPath, tempFile.getPath());
    }

    @Test
    @SmallTest
    public void createGoHomeFileItem() {
    }

    @Test
    @SmallTest
    public void createPlayFileItem() {
    }

    @Test
    @SmallTest
    public void isImage() {
    }

    @Test
    @SmallTest
    public void getImageMimeType() {
    }
}