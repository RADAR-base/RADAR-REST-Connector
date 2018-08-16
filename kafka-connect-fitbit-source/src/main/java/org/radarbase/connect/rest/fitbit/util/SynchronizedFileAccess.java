package org.radarbase.connect.rest.fitbit.util;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.radarbase.connect.rest.util.ThrowingFunction.tryOrRethrow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SynchronizedFileAccess<T> {
  private static final Map<Path, SynchronizedFileAccess<?>> instances = new ConcurrentHashMap<>();

  public static <T> SynchronizedFileAccess<T> ofPath(Path path, ObjectMapper mapper, Class<T> cls) throws IOException {
    try {
      SynchronizedFileAccess<?> instance = instances.computeIfAbsent(path,
          tryOrRethrow(p -> new SynchronizedFileAccess<>(mapper, path, cls),
              (p, ex) -> new UncheckedIOException("Failed to read path " + p, (IOException) ex)));

      if (instance.cls != cls) {
        throw new IllegalArgumentException("Cannot map path " + path + " to both classes "
            + instance.cls + " and " + cls);
      }
      @SuppressWarnings("unchecked")
      SynchronizedFileAccess<T> castedInstance = (SynchronizedFileAccess<T>) instance;
      return castedInstance;
    } catch (UncheckedIOException ex) {
      throw ex.getCause();
    }
  }

  private final ObjectWriter writer;
  private final T value;
  private final Path path;
  private final Class<T> cls;

  public SynchronizedFileAccess(ObjectMapper mapper, Path path, Class<T> cls) throws IOException {
    try (InputStream in = Files.newInputStream(path)) {
      this.value = mapper.readValue(in, cls);
    }
    this.writer = mapper.writerFor(cls);
    this.path = path;
    this.cls = cls;
  }

  public synchronized T get() {
    return value;
  }

  public void store() throws IOException {
    Path temp = Files.createTempFile("sync-file", ".tmp");
    try {
      try (OutputStream out = Files.newOutputStream(temp)) {
        synchronized (this) {
          writer.writeValue(out, value);
        }
      }
      Files.move(temp, path, REPLACE_EXISTING);
    } catch (IOException ex) {
      Files.deleteIfExists(temp);
    }
  }
}
