package cat.ella;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

public class Reflections {

    private final Class<?> callingClass;
    private Collection<Class<?>> presentClasses = new HashSet<>();

    public Reflections(Class<?> callingClass) {
        this.callingClass = callingClass;

        Collection<String> paths = captureFilePaths("");
        this.presentClasses = captureClassPaths(paths);
    }

    public List<Method> getMethodsAnnotatedWith(Class<? extends Annotation> annotation) {
        List<Method> methods = new ArrayList<>();

        for (Class<?> presentClass : presentClasses) {
            for (Method method : presentClass.getDeclaredMethods()) {
                if (method.isAnnotationPresent(annotation)) {
                    methods.add(method);
                }
            }
        }
        return methods;
    }

    private Collection<String> captureFilePaths(String localPath) {
        Set<String> paths = new HashSet<>();
        try {
            final File jarFile = new File(callingClass.getProtectionDomain().getCodeSource().getLocation().getPath());
            if (jarFile.isFile()) { // Running from JAR
                try (JarFile jar = new JarFile(jarFile)) {
                    paths.addAll(
                            jar.stream()
                                    .filter(entry -> !entry.isDirectory())
                                    .map(JarEntry::getName)
                                    .collect(Collectors.toSet())
                    );
                }
            } else { // Running from IDEs
                InputStream inputStream = callingClass.getClassLoader().getResourceAsStream("");
                if (inputStream != null) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            System.out.println("Line: " + line);
                            // Check if the line contains a file path
                            if (line.contains(".java") || line.contains(".class")) {
                                paths.add(localPath + line);
                            } else {
                                paths.addAll(captureFilePaths(localPath + line + "/"));
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