package mortar.dagger1support;

import android.app.Activity;
import android.content.Context;
import dagger.ObjectGraph;
import java.util.Collection;
import mortar.MortarScope;
import mortar.bundler.BundleServiceRunner;

/**
 * Provides utility methods for using Mortar with Dagger 1.
 */
public class ObjectGraphService {
  public static final String SERVICE_NAME = ObjectGraphService.class.getName();

  /**
   * Set the {@link ObjectGraph} to be provided from the new scope.
   */
  public static void inNewScope(MortarScope.Builder builder, ObjectGraph objectGraph) {
    builder.withService(ObjectGraphService.SERVICE_NAME, objectGraph);
  }

  /**
   * Create the {@link ObjectGraph} to be provided from the new scope.
   * @throws NullPointerException if the builder has no parent, or the parent provide no graph
   * @param daggerModule may be null, a single module (annotated with {@link dagger.Module}),
   * or a collection of modules.
   */
  public static void inNewScope(MortarScope.Builder builder, Object daggerModule) {
    inNewScope(builder, createSubgraph(getObjectGraph(builder.getParent()), daggerModule));
  }

  public static ObjectGraph getObjectGraph(Context context) {
    return (ObjectGraph) context.getSystemService(ObjectGraphService.SERVICE_NAME);
  }

  public static ObjectGraph getObjectGraph(MortarScope scope) {
    return scope.getService(ObjectGraphService.SERVICE_NAME);
  }

  /**
   * A convenience wrapper for {@link ObjectGraphService#getObjectGraph} to simplify dynamic
   * injection, typically for {@link Activity} and {@link android.view.View} instances that must be
   * instantiated by Android.
   */
  public static void inject(Context context, Object object) {
    getObjectGraph(context).inject(object);
  }

  private static ObjectGraph createSubgraph(ObjectGraph parentGraph, Object daggerModule) {
    ObjectGraph newGraph;
    if (daggerModule == null) {
      newGraph = parentGraph.plus();
    } else if (daggerModule instanceof Collection) {
      Collection c = (Collection) daggerModule;
      newGraph = parentGraph.plus(c.toArray(new Object[c.size()]));
    } else {
      newGraph = parentGraph.plus(daggerModule);
    }
    return newGraph;
  }

  /**
   * Returns the existing {@link MortarScope} scope for the given {@link Activity}, or
   * uses the {@link Blueprint} to create one if none is found. The scope will provide
   * {@link mortar.bundler.BundleService} and {@link BundleServiceRunner}.
   * <p/>
   * It is expected that this method will be called from {@link Activity#onCreate}. Calling
   * it at other times may lead to surprises.
   */
  @Deprecated public static MortarScope requireActivityScope(MortarScope parentScope,
      Blueprint blueprint) {
    String childName = blueprint.getMortarScopeName();
    MortarScope child = parentScope.findChild(childName);
    if (child == null) {
      ObjectGraph parentGraph = parentScope.getService(ObjectGraphService.SERVICE_NAME);
      Object daggerModule = blueprint.getDaggerModule();
      Object childGraph = createSubgraph(parentGraph, daggerModule);
      MortarScope.Builder builder = parentScope.buildChild(childName)
          .withService(ObjectGraphService.SERVICE_NAME, childGraph);
      BundleServiceRunner.inNewScope(builder);
      child = builder.build();
    }
    return child;
  }

  /**
   * Returns the existing child whose name matches the given {@link Blueprint}'s
   * {@link Blueprint#getMortarScopeName()} value. If there is none, a new child is created
   * based on {@link Blueprint#getDaggerModule()}. Note that
   * {@link Blueprint#getDaggerModule()} is not called otherwise.
   *
   * @throws IllegalStateException if this scope has been destroyed
   */
  @Deprecated public static MortarScope requireChild(MortarScope parentScope, Blueprint blueprint) {
    String childName = blueprint.getMortarScopeName();
    MortarScope child = parentScope.findChild(childName);
    if (child == null) {
      ObjectGraph parentGraph = parentScope.getService(ObjectGraphService.SERVICE_NAME);
      Object daggerModule = blueprint.getDaggerModule();
      Object childGraph = createSubgraph(parentGraph, daggerModule);
      child = parentScope.buildChild(childName)
          .withService(ObjectGraphService.SERVICE_NAME, childGraph)
          .build();
    }
    return child;
  }
}
