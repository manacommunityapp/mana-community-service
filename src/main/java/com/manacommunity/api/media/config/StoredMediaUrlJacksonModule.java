package com.manacommunity.api.media.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.manacommunity.api.media.service.MediaUrlService;
import org.springframework.beans.factory.ObjectProvider;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Re-signs private-bucket S3 URLs stored as plain strings in user-image fields
 * (profile pictures / avatars) so the UI never receives a URL that returns 403.
 */
public class StoredMediaUrlJacksonModule extends SimpleModule {

    private static final Set<String> IMAGE_FIELDS = Set.of(
            "profilePicUrl", "profilePic", "avatarUrl", "avatar", "profileImageUrl",
            "userProfilePicUrl", "driverPhoto", "passengerPhoto", "photo", "authorAvatar");

    public StoredMediaUrlJacksonModule(ObjectProvider<MediaUrlService> urlServiceProvider) {
        setSerializerModifier(new BeanSerializerModifier() {
            @Override
            public List<BeanPropertyWriter> changeProperties(SerializationConfig config,
                                                             BeanDescription beanDesc,
                                                             List<BeanPropertyWriter> props) {
                for (BeanPropertyWriter w : props) {
                    if (IMAGE_FIELDS.contains(w.getName()) && String.class.equals(w.getType().getRawClass())) {
                        w.assignSerializer(new JsonSerializer<Object>() {
                            @Override
                            public void serialize(Object value, JsonGenerator gen, SerializerProvider sp)
                                    throws IOException {
                                String s = (String) value;
                                MediaUrlService svc = urlServiceProvider.getIfAvailable();
                                gen.writeString(svc != null ? svc.resolveStoredUrl(s) : s);
                            }
                        });
                    }
                }
                return props;
            }
        });
    }
}
