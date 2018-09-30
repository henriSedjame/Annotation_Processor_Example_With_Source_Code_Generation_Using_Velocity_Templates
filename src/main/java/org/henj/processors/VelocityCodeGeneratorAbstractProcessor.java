package org.henj.processors;

import org.apache.velocity.Template;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;

/**
 * @Project henj
 * @Author Henri Joel SEDJAME
 * @Date 30/09/2018
 * @Class purposes : .......
 */
public abstract class VelocityCodeGeneratorAbstractProcessor extends AbstractProcessor {

  protected VelocityEngine velocityEngine;
  protected Messager messager;
  protected Filer filer;
  protected String templatePath;


  public VelocityCodeGeneratorAbstractProcessor(String templatePath) {
    this.templatePath = templatePath;
  }

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

    messager = processingEnv.getMessager();
    filer = processingEnv.getFiler();
    this.velocityEngine = new VelocityEngine();
    velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
    velocityEngine.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
    velocityEngine.init();
  }

  protected Template getTemplate(){
    return this.velocityEngine.getTemplate(this.templatePath);
  }
}
