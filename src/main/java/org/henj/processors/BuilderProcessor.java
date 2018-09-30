package org.henj.processors;

import com.google.auto.service.AutoService;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ExecutableType;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @Project henj
 * @Author Henri Joel SEDJAME
 * @Date 21/09/2018
 * @Class purposes : .......
 */

@SupportedAnnotationTypes("org.henj.annotations.BuilderProperty")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@AutoService(Processor.class)
public class BuilderProcessor extends AbstractProcessor {

  VelocityEngine velocityEngine;

  /**
   * Initializes the processor with the processing environment by
   * setting the {@code processingEnv} field to the value of the
   * {@code processingEnv} argument.  An {@code
   * IllegalStateException} will be thrown if this method is called
   * more than once on the same object.
   *
   * @param processingEnv environment to access facilities the tool framework
   *                      provides to the processor
   * @throws IllegalStateException if this method is called more than once.
   */
  @Override
  public synchronized void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);

    velocityEngine = new VelocityEngine();
    velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
    velocityEngine.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
    velocityEngine.init();

  }

  /**
   *
   * @param annotations : liste des annotations utilisées dans le code
   * @param roundEnv : représentation de l'environnement d'un "round" lors d'un cycle de compilation
   * @return
   */
  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

    // Itération sur la liste des annotations
    for (TypeElement annotation : annotations) {

      //Récupération des élements annotés avec @BuilderProperty
      Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(annotation);

      //Définition du prédicat sur les éléments devant être pris en compte pour l'annotation @BuilderProperty
      Predicate<Element> predicate = element -> ((ExecutableType) element.asType()).getParameterTypes().size() == 1 && element.getSimpleName().toString().startsWith("set");

      //Collecte sous forme d'un Map des éléments annotés en deux parties: donc deux paires (K,V)
      //1ere paire (True, Liste des éléments respectant le prédicat)
      //2e paire (False, Liste des éléments ne respectant pas le prédicat)
      Map<Boolean, List<Element>> annotatedMethods = annotatedElements.stream().collect(Collectors.partitioningBy(predicate));

      //Récupération des deux listes
      List<Element> setters = annotatedMethods.get(true);
      List<Element> otherMethods = annotatedMethods.get(false);

      //Ecriture d'un message d'erreur pour chacun des éléments de la 2e liste
      otherMethods.forEach(element -> processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "@BuilderProperty must be applied to a setXxx method with a single argument", element));

      //Si aucun élément ne respecte le prédicat, passer à l'annotation suivante
      if (setters.isEmpty()) {
        continue;
      }



      final Map<String, List<Element>> classNameMethodMap = setters.stream()
        .collect(Collectors.groupingBy(element -> ((TypeElement) element.getEnclosingElement()).getQualifiedName().toString()));

      classNameMethodMap.entrySet().forEach(entry -> {

        //Récupération du nom de la
        String className = entry.getKey();

        Map<String, String> setterMap = entry.getValue().stream().collect(Collectors.toMap(setter -> setter.getSimpleName().toString(),
          setter -> ((ExecutableType) setter.asType()).getParameterTypes().get(0).toString()));

        try {
          //writeBuilderFile(className, setterMap);
          this.writeBuilderFileFromVelocityTemplate(velocityEngine, className, setterMap);
        } catch (IOException e) {
          e.printStackTrace();
        }
      });

    }

    return true;
  }

  private void writeBuilderFileFromVelocityTemplate(VelocityEngine engine, String className, Map<String, String> setterMap) throws IOException {

    final Template template = engine.getTemplate("Templates/Builder.vm");

    VelocityContext context = new VelocityContext();

    String packageName = null;
    int lastDot = className.lastIndexOf('.');
    if (lastDot > 0) {
      packageName = className.substring(0, lastDot);
    }

    String simpleClassName = className.substring(lastDot + 1);
    String builderClassName = className + "Builder";
    String builderSimpleClassName = builderClassName.substring(lastDot + 1);

    context.put("packageName", packageName);
    context.put("builderClassName", builderSimpleClassName);
    context.put("className", simpleClassName);
    context.put("methods",setterMap);

    JavaFileObject builderFile = processingEnv.getFiler().createSourceFile(builderClassName);

    try(Writer writer = new PrintWriter(builderFile.openWriter())) {
      template.merge(context, writer);
    }

  }

  private void writeBuilderFile(String className, Map<String, String> setterMap) throws IOException {

    String packageName = null;
    int lastDot = className.lastIndexOf('.');
    if (lastDot > 0) {
      packageName = className.substring(0, lastDot);
    }

    String simpleClassName = className.substring(lastDot + 1);
    String builderClassName = className + "Builder";
    String builderSimpleClassName = builderClassName.substring(lastDot + 1);

    JavaFileObject builderFile = processingEnv.getFiler().createSourceFile(builderClassName);
    try (PrintWriter out = new PrintWriter(builderFile.openWriter())) {

      if (packageName != null) {
        out.print("package ");
        out.print(packageName);
        out.println(";");
        out.println();
      }

      out.print("public class ");
      out.print(builderSimpleClassName);
      out.println(" {");
      out.println();

      out.print("    private ");
      out.print(simpleClassName);
      out.print(" object = new ");
      out.print(simpleClassName);
      out.println("();");
      out.println();

      out.print("    public ");
      out.print(simpleClassName);
      out.println(" build() {");
      out.println("        return object;");
      out.println("    }");
      out.println();

      setterMap.entrySet().forEach(setter -> {
        String methodName = setter.getKey();
        String argumentType = setter.getValue();

        out.print("    public ");
        out.print(builderSimpleClassName);
        out.print(" ");
        out.print(methodName);

        out.print("(");

        out.print(argumentType);
        out.println(" value) {");
        out.print("        object.");
        out.print(methodName);
        out.println("(value);");
        out.println("        return this;");
        out.println("    }");
        out.println();
      });

      out.println("}");

    }
  }
}
