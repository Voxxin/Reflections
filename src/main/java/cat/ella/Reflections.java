package cat.ella;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

public class Reflections {

    private final Class<?> callingClass;

    public Reflections(Class<?> callingClass) {
        this.callingClass = callingClass;

        Collection<String> paths = captureFilePaths("");
        Collection<Class<?>> classes = captureClassPaths(paths);

        for (Class<?> path : classes) {
            System.out.println(path);
        }
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
                InputStream inputStream = getClass().getClassLoader().getResourceAsStream(localPath);
                if (inputStream != null) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                        reader.lines().forEach(p -> paths.addAll(p.contains(".") ?
                                List.of(localPath + p) : captureFilePaths(localPath + p + "/")));
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