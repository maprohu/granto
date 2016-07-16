package granto.client.impl;

import granto.client.GrantoApi;
import granto.client.GrantoClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Collections;

/**
 * Created by martonpapp on 16/07/16.
 */
public class GrantoClientImpl implements GrantoClient {

    final File jar;
    final ParentLastURLClassLoader classLoader;
    final GrantoClient delegate;

    public GrantoClientImpl() {
        try {
            jar = File.createTempFile("granto", ".jar");
            ReadableByteChannel rbc = Channels.newChannel(getClass().getResourceAsStream("granto.jar"));
            FileOutputStream fos = new FileOutputStream(jar);
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            fos.close();

            classLoader = new ParentLastURLClassLoader(
                    Collections.singletonList(jar.toURI().toURL())
            );

            delegate = (GrantoClient) classLoader.loadClass("granto.client.bin.GrantoClientImpl").newInstance();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T extends GrantoApi> T load(Class<T> clazz) {
        return delegate.load(clazz);
    }

    public void close() {
        delegate.close();
        jar.delete();
    }

}
