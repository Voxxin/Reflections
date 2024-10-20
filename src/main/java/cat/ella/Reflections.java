package cat.ella;

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

public class Reflections {

    private final Class<?> callingClass;
    private final Collection<Class<?>> presentClasses;

    public Reflections(Class<?> callingClass) {
        this.callingClass = callingClass;

        Collection<String> paths = captureFilePaths("");
        this.presentClasses = captureClassPaths(paths);
    }

    public List<Method> getMethodsAnnotatedWith(Class<? extends Annotation> annotation) {
        List<Method> methods = new ArrayList<>();
        Set<Class<?>> processedClasses = new HashSet<>();

        for (Class<?> presentClass : this.presentClasses) {
            if (!processedClasses.add(presentClass)) {
                continue;
            }

            Method[] declaredMethods = presentClass.getDeclaredMethods();
            for (Method method : declaredMethods) {
                if (method.isAnnotationPresent(annotation)) {
                    methods.add(method);
                }
            }
        }

        return methods;
    }


    private Collection<String> captureFilePaths(String localPath) {
        Set<String> paths = new HashSet<>();
        Set<String> seenPaths = new HashSet<>();

        try {
            final File jarFile = new File(callingClass.getProtectionDomain().getCodeSource().getLocation().getPath());
            if (jarFile.isFile()) {
                try (JarFile jar = new JarFile(jarFile)) {
                    jar.stream().map(JarEntry::getName).filter(s -> s.endsWith(".class")).forEach(paths::add);
                }
            } else { // Running from IDEs
                InputStream inputStream = getClass().getClassLoader().getResourceAsStream(localPath);
                if (inputStream != null) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (!line.contains(".")) { // Directories
                                String newPath = localPath + line + "/";
                                if (!seenPaths.contains(newPath)) {
                                    seenPaths.add(newPath);
                                    paths.addAll(captureFilePaths(newPath));
                                }
                            } else { // Files
                                paths.add(localPath + line);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return paths;
    }

    private Collection<Class<?>> captureClassPaths(Collection<String> paths) {
        Set<Class<?>> classes = new HashSet<>();

        for (String path : paths) {
            if (path.endsWith(".class")) {
                String className = path.replace("/", ".").substring(0, path.length() - 6);
                try {
                    classes.add(Class.forName(className));
                } catch (ClassNotFoundException e) {
                    System.err.println("Class not found: " + className);
                }
            }
        }
        return classes;
    }
}