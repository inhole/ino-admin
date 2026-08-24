package com.ino.admin.config;

import java.io.IOException;
import java.io.PushbackReader;
import java.util.Properties;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertySourceFactory;

final class Utf8BomAwarePropertySourceFactory implements PropertySourceFactory {
    @Override
    public PropertySource<?> createPropertySource(String name, EncodedResource resource) throws IOException {
        var properties = new Properties();
        try (var reader = new PushbackReader(resource.getReader(), 1)) {
            var firstCharacter = reader.read();
            if (firstCharacter != 0xFEFF && firstCharacter != -1) {
                reader.unread(firstCharacter);
            }
            properties.load(reader);
        }

        var propertySourceName = name != null ? name : resource.getResource().getDescription();
        return new PropertiesPropertySource(propertySourceName, properties);
    }
}
