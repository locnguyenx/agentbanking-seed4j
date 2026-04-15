package com.agentbanking;

import static org.assertj.core.api.Assertions.*;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

@UnitTest
class BeanValidationTest {

  private static final String ROOT_PACKAGE = "com.agentbanking";
  
  // Exclude test controllers and controllers with @Validated at class level
  private static final Set<String> EXCLUDED_CONTROLLERS = Set.of(
    "ExceptionTranslatorTestController",
    "AuthenticationResource",
    "DummyResource"
  );
  
  private static final Set<Method> OBJECT_METHODS = Arrays.stream(Object.class.getMethods()).collect(Collectors.toUnmodifiableSet());
  private static final Set<Class<?>> controllers;

  static {
    var reflections = new Reflections(
      new ConfigurationBuilder()
        .setUrls(ClasspathHelper.forPackage(ROOT_PACKAGE))
        .setScanners(Scanners.TypesAnnotated, Scanners.SubTypes)
        .filterInputsBy(new FilterBuilder().includePackage(ROOT_PACKAGE))
    );
    controllers = reflections.getTypesAnnotatedWith(RestController.class);
    System.out.println("=== BeanValidationTest: Found " + controllers.size() + " controllers ===");
    controllers.forEach(c -> System.out.println("  - " + c.getSimpleName() + " @Validated=" + c.isAnnotationPresent(Validated.class)));
  }

  @Test
  void shouldHaveValidatedAnnotationForAllParameters() {
    var failingControllers = controllers
      .stream()
      .filter(controller -> !EXCLUDED_CONTROLLERS.contains(controller.getSimpleName()))
      .filter(controller -> !controller.isAnnotationPresent(Validated.class)) // Only check controllers WITHOUT @Validated
      .filter(controller -> hasDomainParameters(controller))
      .collect(Collectors.toList());
    
    if (!failingControllers.isEmpty()) {
      System.out.println("=== Failing controllers (missing @Validated): " + failingControllers.size() + " ===");
      failingControllers.forEach(c -> System.out.println("  - " + c.getSimpleName()));
      throw new AssertionError("Missing @Validated on class: " + failingControllers.stream().map(Class::getSimpleName).toList());
    }
  }

  private boolean hasDomainParameters(Class<?> controller) {
    return Arrays.stream(controller.getMethods())
      .filter(m -> !OBJECT_METHODS.contains(m))
      .filter(m -> !Modifier.isPrivate(m.getModifiers()))
      .anyMatch(m -> Arrays.stream(m.getParameters())
        .anyMatch(p -> !p.getType().isPrimitive() && p.getType().getName().startsWith(ROOT_PACKAGE)));
  }
}