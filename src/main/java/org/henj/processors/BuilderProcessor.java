package org.henj.processors;

import com.google.auto.service.AutoService;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.henj.Utils.TemplateName;


import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
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
public class BuilderProcessor extends VelocityCodeGeneratorAbstractProcessor {

  /**
   * Constructeur permettant de définir le chemin vers le template velocity à utiliser
   */
  public BuilderProcessor() {
    super(TemplateName.BUILDER);
  }


  /**
   * @param annotations : liste des annotations utilisées dans le code
   * @param roundEnv    : représentation de l'environnement d'un "round" lors d'un cycle de compilation
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
      otherMethods.forEach(element -> this.messager.printMessage(Diagnostic.Kind.ERROR, "@BuilderProperty must be applied to a setter method with a single argument", element));

      //Si aucun élément ne respecte le prédicat, passer à l'annotation suivante
      if (setters.isEmpty()) {
        continue;
      }

      //Regrouper les méthodes setters provenant de la même classe entre elles
      final Map<String, List<Element>> classNameMethodMap = setters.stream()
        .collect(Collectors.groupingBy(element -> ((TypeElement) element.getEnclosingElement()).getQualifiedName().toString()));

      //Pour chaque groupe de méthodes setters (provenant de la même classe)
      classNameMethodMap.entrySet().forEach(entry -> {

        //Récupération du nom de la classe
        String className = entry.getKey();

        //Récupération de la liste des méthodes stters
        final List<Element> settersMethods = entry.getValue();

        //Regrouper les methodes setters en une liste de paires (nom de la méthode, nom du type de l'attribut)
        Map<String, String> setterMap = settersMethods.stream().collect(Collectors.toMap(setter -> setter.getSimpleName().toString(),
          setter -> ((ExecutableType) setter.asType()).getParameterTypes().get(0).toString()));

        //Ecrire le fichier .class
        try {
          this.writeBuilderFileFromVelocityTemplate(className, setterMap);
        } catch (IOException e) {
          this.messager.printMessage(Diagnostic.Kind.ERROR, "Echec de l'écriture du code source de la classe " + className + "Builder");
        }
      });

    }

    return true;
  }

  /**
   * Méthode permettant l'écriture du fichier .class à partir du templatePath Velocity
   * @param className
   * @param setterMap
   * @throws IOException
   */
  private void writeBuilderFileFromVelocityTemplate(String className, Map<String, String> setterMap) throws IOException {

    // Récupérer le template grace au velocityEngine
    final Template template = this.getTemplate();

    //Initialiser un contexte
    VelocityContext context = new VelocityContext();

    String packageName = null;
    int lastDot = className.lastIndexOf('.');
    if (lastDot > 0) {
      packageName = className.substring(0, lastDot);
    }

    String simpleClassName = className.substring(lastDot + 1);
    String builderClassName = className + "Builder";
    String builderSimpleClassName = builderClassName.substring(lastDot + 1);

    //Ajouter les variables au contexte
    context.put("packageName", packageName);
    context.put("builderClassName", builderSimpleClassName);
    context.put("className", simpleClassName);
    context.put("methods", setterMap);

    //Créer le fichier .class
    JavaFileObject builderFile = this.filer.createSourceFile(builderClassName);

    //Appliquer le template au fichier .class créé
    try (Writer writer = new PrintWriter(builderFile.openWriter())) {
      template.merge(context, writer);
    }

  }


}
