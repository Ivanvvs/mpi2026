package com.exam.service;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceLayerDependencyTest {

    @Test
    void businessServicesExposeNoRealtimeOrStompDependencies() {
        List<Class<?>> services = List.of(
                AdminDashboardService.class,
                ExamLifecycleService.class,
                ExamParticipationService.class,
                ExamResultService.class
        );

        for (Class<?> service : services) {
            Stream.concat(
                            Stream.of(service.getDeclaredFields()).map(Field::getType),
                            Stream.concat(
                                    Stream.of(service.getDeclaredConstructors()).flatMap(this::constructorTypes),
                                    Stream.of(service.getDeclaredMethods()).flatMap(this::methodTypes)
                            )
                    )
                    .forEach(type -> assertThat(isRealtimeInfrastructure(type))
                            .as("%s must not depend on realtime infrastructure", service.getSimpleName())
                            .isFalse());
        }
    }

    private Stream<Class<?>> constructorTypes(Constructor<?> constructor) {
        return Stream.of(constructor.getParameterTypes());
    }

    private Stream<Class<?>> methodTypes(Method method) {
        return Stream.concat(Stream.of(method.getReturnType()), Stream.of(method.getParameterTypes()));
    }

    private boolean isRealtimeInfrastructure(Class<?> type) {
        return type == SimpMessagingTemplate.class || type.getPackageName().startsWith("com.exam.realtime");
    }
}
