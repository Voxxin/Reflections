import cat.ella.Reflections;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Collection;

public class reflectionstest {

    @Test
    public void test() {
        Reflections reflections = new Reflections(Reflections.class);
        Collection<Method> methods = reflections.getMethodsAnnotatedWith(Test.class);

        System.out.println(methods);
    }
}
