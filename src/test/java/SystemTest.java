import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SystemTest {

    @BeforeAll
    public static void beforeAll() throws IOException, ClassNotFoundException, InterruptedException {
        Files.createDirectories(Paths.get("src/test/resources/server"));
        Files.createDirectories(Paths.get("src/test/resources/client"));

        new Thread(() -> {
            try {
                Server.main(new String[] {"src/test/resources/server"});
            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }).start();
        new Thread(() -> {
            try {
                Client.main(new String[] {"src/test/resources/client"});
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    @Order(1)
    @Test
    public void test() {
        System.out.println("Integration test");
    }

    @Order(2)
    @Test
    public void testAddFilesAtClientAndCheck() throws IOException, InterruptedException {
        for(int i = 0; i < 10; i++) {
            File file = new File("src/test/resources/client/test" + i + ".txt");
            assertTrue(file.createNewFile());
        }

        sleep(10000);
        assertEquals(Files.list(Paths.get("src/test/resources/server")).count(), Files.list(Paths.get("src/test/resources/client")).count());
    }

    @Order(3)
    @Test
    void testDeleteFilesAtClientAndCheck() throws IOException, InterruptedException {
        for(File file : Objects.requireNonNull(new File("src/test/resources/client").listFiles())) {
            assertTrue(file.delete());
        }

        sleep(10000);
        assertEquals(Files.list(Paths.get("src/test/resources/server")).count(), Files.list(Paths.get("src/test/resources/client")).count());
    }

    @Order(4)
    @Test
    void testUpdateFilesAtClientAndCheck() throws IOException, InterruptedException {
        for(int i = 20; i < 23; i++) {
            File file = new File("src/test/resources/client/test" + i + ".txt");
            assertTrue(file.createNewFile());
        }
        sleep(1000);

        for(int i = 20; i < 23; i++) {
            String message = "Hello World" + i;
            Files.writeString(Paths.get("src/test/resources/client/test" + i + ".txt"), message);
        }

        sleep(10000);

        assertEquals(Files.list(Paths.get("src/test/resources/server")).count(), Files.list(Paths.get("src/test/resources/client")).count());

        for(int i = 0; i < 3; i++) {
            String messageFromServer = Files.readString(Paths.get("src/test/resources/server/test" + i + ".txt"));
            String messageFromClient = Files.readString(Paths.get("src/test/resources/client/test" + i + ".txt"));
            assertEquals(messageFromServer, messageFromClient);
        }
    }

    @AfterAll
    public static void afterAll() throws IOException {
        System.out.println("Integration test finished");
        // Clean up
        for(File file: Objects.requireNonNull(new File("src/test/resources/server").listFiles())) {
            file.delete();
        }

        for(File file: Objects.requireNonNull(new File("src/test/resources/client").listFiles())) {
            file.delete();
        }

        Files.delete(Paths.get("src/test/resources/server"));
        Files.delete(Paths.get("src/test/resources/client"));

    }
}
