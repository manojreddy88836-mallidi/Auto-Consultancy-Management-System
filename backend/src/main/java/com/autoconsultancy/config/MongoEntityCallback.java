package com.autoconsultancy.config;

import com.autoconsultancy.service.SequenceGeneratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertEvent;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

@Component
@RequiredArgsConstructor
public class MongoEntityCallback extends AbstractMongoEventListener<Object> {

    private final SequenceGeneratorService sequenceGenerator;

    @Override
    public void onBeforeConvert(BeforeConvertEvent<Object> event) {
        Object source = event.getSource();
        Class<?> clazz = source.getClass();

        if (clazz.isAnnotationPresent(Document.class)) {
            Document documentAnnotation = clazz.getAnnotation(Document.class);
            String collectionName = documentAnnotation.collection();

            if (collectionName == null || collectionName.isEmpty()) {
                collectionName = clazz.getSimpleName().toLowerCase();
            }

            try {
                Field idField = findIdField(clazz);
                if (idField != null) {
                    idField.setAccessible(true);
                    Object idValue = idField.get(source);
                    if (idValue == null && idField.getType().equals(Long.class)) {
                        long nextId = sequenceGenerator.generateSequence(collectionName);
                        idField.set(source, nextId);
                    }
                }
            } catch (Exception e) {
                // Ignore reflection exceptions if any
            }
        }
    }

    private Field findIdField(Class<?> clazz) {
        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(org.springframework.data.annotation.Id.class)) {
                    return field;
                }
            }
            clazz = clazz.getSuperclass();
        }
        return null;
    }
}
